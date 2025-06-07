package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

/** Default implementation using docker-java client (socket first, then TCP). */
public final class DefaultDockerService implements DockerService {
  private final DockerClient dockerClient;

  /** constructor. */
  public DefaultDockerService() {
    this.dockerClient = initClient();
  }

  private DockerClient initClient() {

    String home = System.getProperty("user.home");
    String userSocket = "unix:///" + home + "/.docker/run/docker.sock";
    String[] endpoints = {"unix:///var/run/docker.sock", "unix:///run/docker.sock", userSocket,
        "tcp://localhost:2375"};

    for (String endpoint : endpoints) {
      try {
        DockerClientConfig config =
            DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(endpoint).build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30)).responseTimeout(Duration.ofSeconds(45))
            .build();
        DockerClient client =
            DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
        client.pingCmd().exec();
        return client;
      } catch (Exception ignored) {
        // Try next endpoint
      }
    }
    throw new IllegalStateException("Cannot connect to Docker on any known endpoint");
  }

  @Override
  public boolean isDockerRunning() {
    try {
      dockerClient.pingCmd().exec();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Path buildDockerImage(final Path buildDir, final String imageTag,
      final String dockerFileContent) throws IOException {

    Path dockerfile = writeDockerFile(buildDir, dockerFileContent);

    File contextDir = buildDir.resolve(GRAALVM_JAVA_MAIN).toFile();

    dockerClient.buildImageCmd().withBaseDirectory(contextDir).withDockerfile(dockerfile.toFile())
        .withTags(Collections.singleton(imageTag)).exec(new BuildImageResultCallback())
        .awaitImageId();

    return dockerfile;
  }

  @Override
  public void runDockerImage(final Path buildDir, final String imageTag)
      throws IOException, InterruptedException {

    Path path = buildDir.resolve("graalvm/output");
    Files.createDirectories(path);

    Volume containerOutputVolume = new Volume("/output");
    HostConfig hostConfig = HostConfig.newHostConfig()
        .withBinds(new Bind(path.toAbsolutePath().toString(), containerOutputVolume));

    CreateContainerResponse container = dockerClient.createContainerCmd(imageTag)
        .withName("copy-file-container-" + System.currentTimeMillis()).withHostConfig(hostConfig)
        .exec();

    String containerId = container.getId();
    dockerClient.startContainerCmd(containerId).exec();
    try {
      dockerClient.waitContainerCmd(containerId).start().awaitCompletion();
    } finally {
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
    }
  }

  @Override
  public void removeDockerImage(final String imageTag) {
    try {
      dockerClient.removeImageCmd(imageTag).withForce(true).exec();
    } catch (NotFoundException e) {
      // ignore
    }
  }
}
