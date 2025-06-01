package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.IOException;
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
  public Path buildDockerfile(final String dockerFileContent) throws IOException {

    Path dockerfile = writeDockerFile(dockerFileContent);

    File contextDir = Path.of("build", GRAALVM_JAVA_MAIN).toFile();

    dockerClient.buildImageCmd().withBaseDirectory(contextDir).withDockerfile(dockerfile.toFile())
        .withTags(Collections.singleton("generated-image")).exec(new BuildImageResultCallback())
        .awaitImageId();

    return dockerfile;
  }
}
