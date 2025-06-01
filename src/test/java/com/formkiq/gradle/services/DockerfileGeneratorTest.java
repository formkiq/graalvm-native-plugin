package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;
import static org.gradle.internal.impldep.org.testng.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for DockerfileGenerator. */
public class DockerfileGeneratorTest {

  @BeforeEach
  public void setup() throws IOException {
    Files.createDirectories(Path.of("build", GRAALVM_JAVA_MAIN));
  }

  @Test
  void testGenerateContentsWithoutArgs() throws IOException {
    // given
    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage("test/image:latest").build();

    // when
    String content = gen.generateContents();

    // then
    assertDockerfileEquals("dockerfile/Dockerfile1", content);
    assertFalse(content.contains("RUN native-image"),
        "Should not include a native-image invocation when no args are provided");
  }

  @Test
  void testGenerateContentsWithArgsAndMainClass() throws IOException {
    // given
    DockerfileGenerator gen =
        DockerfileGenerator.builder().baseImage("graalvm:21.3.0").mainClass("com.example.Main")
            .addNativeImageArg("--no-fallback").addNativeImageArg("-H:Name=myapp").build();

    // when
    String content = gen.generateContents();

    // then
    assertDockerfileEquals("dockerfile/Dockerfile2", content);
  }

  @Test
  void testWriteToFile(@TempDir Path tempDir) throws IOException {
    // given
    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage("ubuntu:20.04")
        .mainClass("com.test.App").addNativeImageArg("--static").build();

    // when
    Path dockerfilePath = tempDir.resolve("Dockerfile");
    gen.writeTo(dockerfilePath);

    // then
    assertTrue(Files.exists(dockerfilePath), "Dockerfile should be written to the specified path");

    String fileContent = Files.readString(dockerfilePath);
    assertEquals(gen.generateContents(), fileContent,
        "Written file content should match generated contents");
  }

  private void assertDockerfileEquals(final String dockerFile, final String content)
      throws IOException {
    assertEquals(content, getDockerfileContent(dockerFile));
  }

  private String getDockerfileContent(final String file) throws IOException {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
      Assertions.assertNotNull(is);
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
