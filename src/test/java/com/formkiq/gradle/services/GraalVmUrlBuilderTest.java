package com.formkiq.gradle.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraalVmUrlBuilderTest {

  @Test
  void testGraalvmUrls() throws IOException, URISyntaxException {
    // given
    final int expectedLines = 225;
    List<String> lines = loadLinesWithFiles();
    assertEquals(expectedLines, lines.size());

    // when
    for (String line : lines) {
      String[] s = line.split(",");
      GraalVmUrlBuilder.Builder builder = GraalVmUrlBuilder.builder().withVersion(s[1])
          .withJavaVersion(s[2]).withPlatform(Platform.fromSuffix(s[0]));

      // then
      assertTrue(builder.build().contains(s[3]));
    }
  }

  /**
   * Reads all lines from a file on the classpath using Files.readAllLines(...).
   *
   * @return a List<String> containing every line of the file in order
   * @throws IllegalArgumentException if the resource is not found on the classpath
   * @throws IOException if an I/O error occurs reading from the file
   * @throws URISyntaxException if the resource URL cannot be converted to a URI/Path
   */
  private List<String> loadLinesWithFiles() throws IOException, URISyntaxException {

    URL url = GraalVmUrlBuilderTest.class.getResource("/graalvmUrls.csv");
    if (url == null) {
      throw new IllegalArgumentException("Resource not found on classpath: " + "/graalvmUrls.csv");
    }

    Path path = Paths.get(url.toURI());
    return Files.readAllLines(path);
  }

  @Test
  @DisplayName("Missing GraalVM version throws exception")
  void testMissingGraalVMVersion() {
    GraalVmUrlBuilder.Builder builder =
        GraalVmUrlBuilder.builder().withJavaVersion("24").withPlatform(Platform.MACOS_X64);

    Exception ex = assertThrows(IllegalStateException.class, builder::build);
    assertEquals("GraalVM version must be specified", ex.getMessage());
  }

  @Test
  @DisplayName("Missing platform throws exception")
  void testMissingPlatform() {
    GraalVmUrlBuilder.Builder builder =
        GraalVmUrlBuilder.builder().withJavaVersion("24").withVersion("24.0.1");

    Exception ex = assertThrows(IllegalStateException.class, builder::build);
    assertEquals("Platform must be specified", ex.getMessage());
  }
}
