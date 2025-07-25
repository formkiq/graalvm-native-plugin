package com.formkiq.gradle;

import static com.formkiq.gradle.internal.Strings.formatToUnix;

import com.formkiq.gradle.internal.NativeImageExecutor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gradle.internal.os.OperatingSystem;

/** {@link Function} to transform -cp for graalvm. */
public class GraalvmClasspathArguments implements Function<GraalvmNativeExtension, List<String>> {

  /** Build Dir. */
  private final Path buildDir;

  /**
   * constructor.
   *
   * @param buildDir {@link Path}
   */
  public GraalvmClasspathArguments(final Path buildDir) {
    this.buildDir = buildDir;
  }

  @Override
  public List<String> apply(final GraalvmNativeExtension extension) {
    return List.of("-cp", buildClassPathString(extension));
  }

  private String buildClassPathString(final GraalvmNativeExtension extension) {

    List<File> files = new ArrayList<>();

    Path path = buildDir.resolve(NativeImageExecutor.GRAALVM_JAVA_MAIN);
    files.add(path.toFile());
    addClasspaths(extension, files);

    return files.stream().map(File::getAbsolutePath)
        .map(s -> extension.getDockerImage() != null ? formatToUnix(s) : s)
        .collect(Collectors.joining(OperatingSystem.current().isWindows() ? ";" : ":"));
  }

  /**
   * Add Classpaths.
   *
   * @param volumeMounts {@link List} {@link File}
   */
  private void addClasspaths(final GraalvmNativeExtension extension,
      final List<File> volumeMounts) {
    if (extension.getAddClasspath() != null) {
      String[] cp = extension.getAddClasspath().split(",");
      for (String c : cp) {
        volumeMounts.add(new File(c));
      }
    }
  }
}
