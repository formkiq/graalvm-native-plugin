package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;
import static org.junit.jupiter.api.Assertions.*;

import com.formkiq.gradle.internal.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** JUnit 5 tests for DefaultDockerService and ShellDockerService. */
class DockerServiceTests {

  private static final String TEST_IMAGE_NAME = "generated-graalvm-native-plugin";
  private static final String DOCKER_IMAGE_24 = "ghcr.io/graalvm/native-image-community:24.0.1";
  private static final Path BUILD_DIR = Paths.get("build");
  private static final Path PATH = BUILD_DIR.resolve(GRAALVM_JAVA_MAIN);

  @Test
  void testDefaultServiceBuildDockerImage() throws IOException {
    // given
    Files.createDirectories(PATH);
    DefaultDockerService service = new DefaultDockerService();
    List<String> nativeArgs = List.of();

    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24)
        .addNativeImageArgs(nativeArgs).mainClass(null).build();

    // when
    Path outputPath = service.buildDockerImage(BUILD_DIR, TEST_IMAGE_NAME, gen.generateContents());

    // then
    assertTrue(Files.exists(outputPath), "Dockerfile should be created");

    String expected = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24).mainClass(null)
        .addNativeImageArgs(nativeArgs).build().generateContents();
    String actual = Files.readString(outputPath, StandardCharsets.UTF_8);
    assertEquals(expected, actual, "Generated Dockerfile should match expected contents");
  }

  @Test
  void testDefaultServiceRunDockerImage() throws IOException, InterruptedException {
    // given
    String dockerFileContent = getDockerfileContent("dockerfile/Dockerfile3");

    for (DockerService service : List.of(new DefaultDockerService(), new ShellDockerService())) {

      FileUtils.deleteRecursively(PATH);
      FileUtils.deleteRecursively(Path.of("build", "graalvm"));

      // when
      service.removeDockerImage(TEST_IMAGE_NAME);
      service.buildDockerImage(BUILD_DIR, TEST_IMAGE_NAME, dockerFileContent);
      service.runDockerImage(BUILD_DIR, TEST_IMAGE_NAME);

      // then
      Path path = Path.of("build", "graalvm", "output", "generated.txt");
      assertTrue(Files.exists(path), "Output should be created");
    }
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
  void testShellServiceBuildDockerImage() throws IOException {
    // given
    Files.createDirectories(PATH);
    ShellDockerService service = new ShellDockerService();
    List<String> nativeArgs = List.of();

    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage(DOCKER_IMAGE_24)
        .addNativeImageArgs(nativeArgs).mainClass(null).build();

    // when
    Path outputPath = service.buildDockerImage(BUILD_DIR, TEST_IMAGE_NAME, gen.generateContents());

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

  private String getDockerfileContent(final String file) throws IOException {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
      Assertions.assertNotNull(is);
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
