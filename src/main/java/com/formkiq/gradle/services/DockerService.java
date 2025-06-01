package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import java.io.File;
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
   * @param dockerFileContent Docker file content
   * @return Path
   * @throws IOException IOException
   */
  Path buildDockerfile(final String dockerFileContent) throws IOException;

  /**
   * Write Dockerfile Content to a {@link Path}.
   *
   * @param dockerFileContent {@link String}
   * @return Path
   * @throws IOException IOException
   */
  default Path writeDockerFile(final String dockerFileContent) throws IOException {

    File dir = Path.of("build", GRAALVM_JAVA_MAIN).toFile();

    Path dockerfilePath = Path.of(dir.toString(), "Dockerfile");
    Files.writeString(dockerfilePath, dockerFileContent, StandardCharsets.UTF_8);
    return dockerfilePath;
  }
}
