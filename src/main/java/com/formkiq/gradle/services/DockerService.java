package com.formkiq.gradle.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Abstraction for Docker-related operations. */
public interface DockerService {

  /**
   * Checks whether the Docker daemon is accessible.
   *
   * @return true if Docker responds, false otherwise
   */
  boolean isDockerRunning();

  /**
   * Generates (or overwrites) a Dockerfile on disk using specified parameters.
   *
   * @param buildDir {@link Path}
   * @param imageTag {@link String}
   * @param dockerFileContent Docker file content
   * @return Path
   * @throws IOException IOException
   */
  Path buildDockerImage(Path buildDir, String imageTag, String dockerFileContent)
      throws IOException;

  /**
   * Write Dockerfile Content to a {@link Path}.
   *
   * @param buildDir {@link Path}
   * @param dockerFileContent {@link String}
   * @return Path
   * @throws IOException IOException
   */
  default Path writeDockerFile(final Path buildDir, final String dockerFileContent)
      throws IOException {

    Path dir = buildDir.resolve("java/main");
    Files.createDirectories(dir);

    Path dockerfilePath = Path.of(dir.toString(), "Dockerfile");
    Files.writeString(dockerfilePath, dockerFileContent, StandardCharsets.UTF_8);
    return dockerfilePath;
  }

  /**
   * Run Docker Image.
   *
   * @param buildDir {@link Path}
   * @param outputImageTag {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  void runDockerImage(Path buildDir, String outputImageTag)
      throws IOException, InterruptedException;

  /**
   * Remove Docker Image.
   *
   * @param imageTag {@link String}
   * @throws IOException IOException
   */
  void removeDockerImage(String imageTag) throws IOException;
}
