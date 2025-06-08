package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import com.formkiq.gradle.internal.ArchiveUtils;
import com.formkiq.gradle.internal.GradleUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.gradle.api.Project;

/** Decompress {@link Project} Runtime Dependencies. */
public class RuntimeDependenciesDecompress implements Function<Path, Void> {

  /** {@link ArchiveUtils}. */
  private final ArchiveUtils archiveUtils = new ArchiveUtils();

  /** {@link Project}. */
  private final Project project;

  /**
   * constructor.
   *
   * @param project {@link Project}
   */
  public RuntimeDependenciesDecompress(final Project project) {
    this.project = project;
  }

  @Override
  public Void apply(final Path buildDir) {

    try {
      Path outputPath = buildDir.resolve(GRAALVM_JAVA_MAIN);
      File outputdir = outputPath.toFile();

      List<File> classPathFiles = GradleUtils.getRuntimeClasspath(project, buildDir);

      for (File file : classPathFiles) {
        archiveUtils.decompressJar(file, outputdir);
      }

      Path libsDir = buildDir.resolve("libs");

      try (Stream<Path> stream = Files.list(libsDir)) {
        List<File> files = stream.map(Path::toFile).toList();

        for (File file : files) {
          archiveUtils.decompressJar(file, outputdir);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }
}
