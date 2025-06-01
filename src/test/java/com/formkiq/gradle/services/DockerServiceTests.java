package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** JUnit 5 tests for DefaultDockerService and ShellDockerService. */
class DockerServiceTests {

  private static final String DOCKER_IMAGE_24 = "ghcr.io/graalvm/native-image-community:24.0.1";
  private static final Path PATH = Path.of("build", GRAALVM_JAVA_MAIN);

  @Test
  void testDefaultServiceBuildDockerfile() throws IOException {
    // given
    Files.createDirectories(PATH);
    DefaultDockerService service = new DefaultDockerService();
    List<String> nativeArgs = List.of();

    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24)
        .addNativeImageArgs(nativeArgs).mainClass(null).build();

    // when
    Path outputPath = service.buildDockerfile(gen.generateContents());

    // then
    assertTrue(Files.exists(outputPath), "Dockerfile should be created");

    String expected = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24).mainClass(null)
        .addNativeImageArgs(nativeArgs).build().generateContents();
    String actual = Files.readString(outputPath, StandardCharsets.UTF_8);
    assertEquals(expected, actual, "Generated Dockerfile should match expected contents");
  }

  @Test
  void testDefaultServiceIsDockerRunningDoesNotThrow() {
    DefaultDockerService service = new DefaultDockerService();
    assertDoesNotThrow(() -> {
      boolean running = service.isDockerRunning();
      assertTrue(running, "isDockerRunning should return a boolean and not throw");
    });
  }

  @Test
  void testShellServiceBuildDockerfile() throws IOException {
    // given
    Files.createDirectories(PATH);
    ShellDockerService service = new ShellDockerService();
    List<String> nativeArgs = List.of();

    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24)
        .addNativeImageArgs(nativeArgs).mainClass(null).build();

    // when
    Path outputPath = service.buildDockerfile(gen.generateContents());

    // then
    assertTrue(Files.exists(outputPath), "Dockerfile should be created");

    String expected =
        DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24).build().generateContents();
    String actual = Files.readString(outputPath, StandardCharsets.UTF_8);
    assertEquals(expected, actual, "Generated Dockerfile should match expected contents");
  }

  @Test
  void testShellServiceIsDockerRunningDoesNotThrow() {
    ShellDockerService service = new ShellDockerService();
    assertDoesNotThrow(() -> {
      boolean running = service.isDockerRunning();
      assertTrue(running, "isDockerRunning should return a boolean and not throw");
    });
  }
}
