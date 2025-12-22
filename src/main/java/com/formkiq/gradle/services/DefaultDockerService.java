package com.formkiq.gradle.services;

import com.github.dockerjava.api.DockerClient;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import org.gradle.api.logging.Logger;

/** Default implementation using docker-java client (socket first, then TCP). */
public final class DefaultDockerService implements DockerService {
  private final DockerClient dockerClient;

  /** {@link Logger}. */
  private final Logger log;

  /** {@link LoggingBuildImageResultCallback}. */
  private final LoggingBuildImageResultCallback callback;

  /**
   * constructor.
   *
   * @param logger {@link Logger}
   */
  public DefaultDockerService(final Logger logger) {
    this.dockerClient = initClient();
    this.log = logger;
    this.callback = new LoggingBuildImageResultCallback(this.log);
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
      final String dockerFileContent, final Path contextDir) throws IOException {

    Path dockerfile = writeDockerFile(buildDir, dockerFileContent);

    String dockerCommand =
        String.format("docker build -f %s -t %s %s", dockerfile, imageTag, contextDir);

    log(dockerCommand);
    dockerClient.buildImageCmd().withBaseDirectory(contextDir.toFile())
        .withDockerfile(dockerfile.toFile()).withTags(Collections.singleton(imageTag))
        .exec(this.callback).awaitImageId();

    return dockerfile;
  }

  private void log(final String log) {
    if (this.log != null) {
      this.log.info(log);
    }
  }

  @Override
  public void runDockerImage(final Path buildDir, final String imageTag)
      throws IOException, InterruptedException {

    Path path = buildDir.resolve("output");
    Files.createDirectories(path);

    String containerName = "copy-file-container-" + System.currentTimeMillis();
    String hostPath = path.toAbsolutePath().toString();

    log(String.format("docker run --name %s -v %s:/output %s", containerName, hostPath, imageTag));

    Volume containerOutputVolume = new Volume("/output");
    HostConfig hostConfig =
        HostConfig.newHostConfig().withBinds(new Bind(hostPath, containerOutputVolume));

    log("Creating container '" + containerName + "' from image '" + imageTag + "'");
    CreateContainerResponse container = dockerClient.createContainerCmd(imageTag)
        .withName(containerName).withHostConfig(hostConfig).exec();

    String containerId = container.getId();
    log("Container created with ID: " + containerId);

    try {
      log("Starting container " + containerId);
      dockerClient.startContainerCmd(containerId).exec();

      log("Waiting for container " + containerId + " to finish");
      dockerClient.waitContainerCmd(containerId).start().awaitCompletion();
      log("Container " + containerId + " finished successfully");
    } finally {
      log("Removing container " + containerId + " (force=true)");
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
      log("Container " + containerId + " removed");
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
