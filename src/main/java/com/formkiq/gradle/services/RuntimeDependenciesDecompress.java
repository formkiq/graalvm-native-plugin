package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import com.formkiq.gradle.internal.ArchiveUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;

/** Decompress {@link Project} Runtime Dependencies. */
public class RuntimeDependenciesDecompress
    implements BiFunction<Path, ConfigurableFileCollection, Void> {

  /** {@link ArchiveUtils}. */
  private final ArchiveUtils archiveUtils = new ArchiveUtils();

  /** constructor. */
  public RuntimeDependenciesDecompress() {}

  @Override
  public Void apply(final Path buildDir, final ConfigurableFileCollection files) {

    try {
      Path outputPath = buildDir.resolve(GRAALVM_JAVA_MAIN);
      File outputdir = outputPath.toFile();

      for (File file : files) {
        if (file.getName().endsWith(".jar")) {
          archiveUtils.decompressJar(file.toPath().toFile(), outputdir);
        }
      }

      Path libsDir = buildDir.resolve("../libs");

      try (Stream<Path> stream = Files.list(libsDir)) {
        List<File> libFiles = stream.map(Path::toFile).toList();

        for (File file : libFiles) {
          archiveUtils.decompressJar(file, outputdir);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }
}
