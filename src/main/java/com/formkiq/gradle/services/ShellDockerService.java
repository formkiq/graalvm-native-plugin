package com.formkiq.gradle.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Shell-based implementation using the Docker CLI. */
public class ShellDockerService implements DockerService {

  @Override
  public boolean isDockerRunning() {
    try {
      ProcessBuilder pb = new ProcessBuilder("docker", "info");
      pb.redirectErrorStream(true);
      Process process = pb.start();
      int exitCode = process.waitFor();
      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  @Override
  public void runDockerImage(final Path buildDir, final String imageTag) throws IOException {

    Path dir = buildDir.resolve("graalvm/output");

    Files.createDirectories(dir);
    String hostOutputDir = dir.toAbsolutePath().toString();
    ProcessBuilder pb =
        new ProcessBuilder("docker", "run", "--rm", "-v", hostOutputDir + ":/output", imageTag);

    pb.redirectErrorStream(true);
    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      throw new IOException("Failed to start docker run process", e);
    }

    String output;
    try (InputStream in = process.getInputStream()) {
      output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("docker run interrupted", e);
    }

    if (exitCode != 0) {
      throw new IOException("docker run failed with exit code " + exitCode + ": " + output);
    }
  }

  @Override
  public Path buildDockerImage(final Path buildDir, final String imageTag,
      final String dockerFileContent) throws IOException {

    Path dockerfile = writeDockerFile(buildDir, dockerFileContent);
    File contextDir =
        Optional.ofNullable(dockerfile.getParent()).map(Path::toFile).orElse(new File("."));

    ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", imageTag, "-f",
        dockerfile.toString(), contextDir.toString());
    pb.redirectErrorStream(false);
    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      throw new IOException("Failed to start docker build process", e);
    }

    String stderr;
    try (InputStream errStream = process.getErrorStream()) {
      stderr = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("docker build interrupted", e);
    }

    if (exitCode != 0) {
      throw new IOException("docker build failed with exit code " + exitCode + ": " + stderr);
    }
    return dockerfile;
  }

  @Override
  public void removeDockerImage(String imageTag) throws IOException {
    ProcessBuilder pb = new ProcessBuilder("docker", "rmi", imageTag);
    pb.redirectErrorStream(true);
    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      throw new IOException("Failed to start docker rmi process", e);
    }

    String output;
    try (InputStream in = process.getInputStream()) {
      output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("docker rmi interrupted", e);
    }

    if (exitCode != 0) {
      throw new IOException("docker rmi failed with exit code " + exitCode + ": " + output);
    }
  }
}
