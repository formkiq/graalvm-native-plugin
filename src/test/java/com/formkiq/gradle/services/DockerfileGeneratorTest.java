package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;
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

/** Tests for DockerfileGenerator. */
public class DockerfileGeneratorTest {

  /** Build Dir. */
  private final Path buildDir = Path.of("build");

  @BeforeEach
  public void setup() throws IOException {
    Files.createDirectories(buildDir.resolve(GRAALVM_JAVA_MAIN));
  }

  @Test
  void testGenerateContentsWithoutArgs() throws IOException {
    // given
    DockerfileGenerator gen = DockerfileGenerator.builder().baseImage("test/image:latest").build();

    // when
    String content = gen.generateContents(buildDir);

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
    String content = gen.generateContents(buildDir);

    // then
    assertDockerfileEquals("dockerfile/Dockerfile2", content);
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
