package com.formkiq.gradle.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.formkiq.gradle.GraalvmNativeExtension;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NativeImageExecutorNoMockTest {

  private Project project;
  private Path fakeBuildDir;

  @BeforeEach
  void setUp() {
    project = ProjectBuilder.builder().build();
    fakeBuildDir = Path.of("build", "dummy");
  }

  /** With output filename set. */
  @Test
  void testGetBuildGraalvmImageArguments01() {
    // given
    GraalvmNativeExtension extension = new GraalvmNativeExtension(project.getObjects());
    extension.setOutputFileName("my-app");
    extension.setMainClassName("com.example.Main");

    NativeImageExecutor executor = new NativeImageExecutor(extension);

    // when
    List<String> args = executor.getBuildGraalvmImageArguments(project, fakeBuildDir);

    // then
    assertTrue(args.stream().anyMatch(a -> a.equals("-H:Name=my-app")),
        () -> "Expected '-H:Name=my-app' in " + args);
    assertEquals("com.example.Main", args.get(args.size() - 1));
  }

  /** Without output filename set. */
  @Test
  void testGetBuildGraalvmImageArguments02() {
    // given
    GraalvmNativeExtension extension = new GraalvmNativeExtension(project.getObjects());
    extension.setMainClassName("com.example.Main");
    NativeImageExecutor executor = new NativeImageExecutor(extension);

    // when
    List<String> args = executor.getBuildGraalvmImageArguments(project, fakeBuildDir);

    // then
    assertTrue(args.stream().noneMatch(a -> a.startsWith("-H:Name=")),
        () -> "Did not expect any '-H:Name=' flag in " + args);
    assertEquals("com.example.Main", args.get(args.size() - 1));
  }

  /** With multiple build options. */
  @Test
  void testGetBuildGraalvmImageArguments03() {
    // given
    GraalvmNativeExtension extension = new GraalvmNativeExtension(project.getObjects());
    extension.setBuildOptions("-Os -o fk");
    extension.setMainClassName("com.example.Main");
    NativeImageExecutor executor = new NativeImageExecutor(extension);

    // when
    List<String> args = executor.getBuildGraalvmImageArguments(project, fakeBuildDir);

    // then
    final int expected = 7;
    int i = 0;
    assertEquals(expected, args.size());
    assertEquals("-Os", args.get(i++));
    assertEquals("-o fk", args.get(i++));
    assertEquals("--enable-http", args.get(i++));
    assertEquals("--enable-https", args.get(i));
  }
}
