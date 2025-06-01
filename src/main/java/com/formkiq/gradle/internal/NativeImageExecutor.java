/**
 * Copyright [2020] FormKiQ Inc. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.formkiq.gradle.internal;

import com.formkiq.gradle.GraalvmClasspathArguments;
import com.formkiq.gradle.GraalvmNativeExtension;
import com.formkiq.gradle.GraalvmParameterToStrings;
import com.formkiq.gradle.services.RuntimeDependenciesDecompress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;

/** GraalVM Native-Image {@link DefaultExecutor}. */
public class NativeImageExecutor {

  /** Graalvm Java Main. */
  public static final String GRAALVM_JAVA_MAIN = "graalvm/java/main";

  /** {@link DockerUtils}. */
  private final DockerUtils docker;

  /** {@link GraalvmNativeExtension}. */
  private final GraalvmNativeExtension extension;

  /**
   * constructor.
   *
   * @param ext {@link GraalvmNativeExtension}
   */
  public NativeImageExecutor(final GraalvmNativeExtension ext) {
    this.extension = ext;
    this.docker = new DockerUtils();
  }

  /**
   * Add Classpaths.
   *
   * @param volumeMounts {@link List} {@link File}
   */
  private void addClasspaths(final List<File> volumeMounts) {
    if (this.extension.getAddClasspath() != null) {
      String[] cp = this.extension.getAddClasspath().split(",");
      for (String c : cp) {
        volumeMounts.add(new File(c));
      }
    }
  }

  /**
   * Build Graalvm Image.
   *
   * @param project {@link Project}
   * @param graalvmBaseDir {@link File}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void buildGraalvmImage(final Project project, final File graalvmBaseDir, File outputDir)
      throws IOException {

    List<String> args = getBuildGraalvmImageArguments(project);

    if (this.extension.getDockerImage() != null) {

      List<String> a = new ArrayList<>();
      a.add("native-image");
      a.addAll(args);

      this.docker.exec(project, outputDir.toString(), a);

    } else {

      project.exec(arg0 -> {
        String executeable =
            OperatingSystem.current().isWindows() ? "native-image.cmd" : "native-image";
        arg0.setCommandLine(
            Paths.get(getGraalBin(graalvmBaseDir).toAbsolutePath().toString(), "/" + executeable)
                .toFile());
        arg0.args(args);
        arg0.setWorkingDir(outputDir);
      });
    }
  }

  /**
   * Build Graalvm classes folder.
   *
   * @param project {@link Project}
   * @throws IOException IOException
   */
  public void buildGraalvmJavaMain(final Project project) throws IOException {

    new RuntimeDependenciesDecompress().apply(project);
    // ArchiveUtils archiveUtils = new ArchiveUtils();
    //
    // Path outputPath = Path.of(project.getBuildDir().getCanonicalPath(), GRAALVM_JAVA_MAIN);
    // File outputdir = outputPath.toFile();
    //
    // List<File> classPathFiles = GradleUtils.getRuntimeClasspath(project);
    //
    // for (File file : classPathFiles) {
    // archiveUtils.decompressJar(file, outputdir);
    // }
    //
    // List<File> files = Files.list(Path.of(project.getBuildDir().getAbsolutePath(), "libs"))
    // .map(Path::toFile).toList();
    //
    // for (File file : files) {
    // archiveUtils.decompressJar(file, outputdir);
    // }
  }

  private List<String> getBuildGraalvmImageArguments(final Project project) {

    List<String> args = new ArrayList<>();
    args.add("--report-unsupported-elements-at-runtime");
    args.add("--no-server");

    args.addAll(new GraalvmParameterToStrings().apply(this.extension));

    args.add("-H:Name=" + getExecutableName(project));

    args.addAll(new GraalvmClasspathArguments(project).apply(this.extension));

    args.add(this.extension.getMainClassName());

    return args;
  }

  private String getExecutableName(final Project project) {
    return this.extension.getOutputFileName() != null ? this.extension.getOutputFileName()
        : project.getName();
  }

  private Path getGraalBin(final File graalvmBaseDir) {
    return OperatingSystem.current().isMacOsX()
        ? Path.of(graalvmBaseDir.getAbsolutePath(), "Contents/Home/bin")
        : Path.of(graalvmBaseDir.getAbsolutePath(), "bin");
  }

  /**
   * Run 'gu' insallation.
   *
   * @param project {@link Project}
   * @param graalvmBaseDir {@link File}
   * @throws IOException IOException
   */
  public void runGuInstallation(final Project project, final File graalvmBaseDir)
      throws IOException {

    if (this.extension.getDockerImage() != null) {

      this.docker.exec(project, null, Arrays.asList("gu", "install", "native-image"));

    } else {

      String guExecutable = OperatingSystem.current().isWindows() ? "gu.cmd" : "gu";
      Path gu =
          Paths.get(getGraalBin(graalvmBaseDir).toAbsolutePath().toString(), "/" + guExecutable);

      if (gu.toFile().exists()) {
        project.exec(arg0 -> {
          arg0.setCommandLine(gu.toFile());
          arg0.args(Arrays.asList("install", "native-image"));
        });
      }
    }
  }

  /**
   * Run Native Image Command.
   *
   * @param project {@link Project}
   * @param graalvmBaseDir {@link Files}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void runNativeImage(final Project project, final File graalvmBaseDir, File outputDir)
      throws IOException {

    buildGraalvmJavaMain(project);

    buildGraalvmImage(project, graalvmBaseDir, outputDir);
  }

  /**
   * Start Native-Image Executor.
   *
   * @param project {@link Project}
   * @throws IOException IOException
   */
  public void start(final Project project) throws IOException {

    if (this.extension.getDockerImage() != null) {
      List<File> volumeMounts = new ArrayList<>();
      volumeMounts.add(Path.of(project.getBuildDir().getAbsolutePath()).toFile());

      addClasspaths(volumeMounts);

      this.docker.startImage(project, this.extension, volumeMounts);
    }
  }
}
