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
import org.gradle.process.ExecOperations;

/** GraalVM Native-Image {@link DefaultExecutor}. */
public class NativeImageExecutor {

  /** Graalvm Java Main. */
  public static final String GRAALVM_JAVA_MAIN = "graalvm/java/main";

  /** {@link GraalvmNativeExtension}. */
  private final GraalvmNativeExtension extension;

  /**
   * constructor.
   *
   * @param ext {@link GraalvmNativeExtension}
   */
  public NativeImageExecutor(final GraalvmNativeExtension ext) {
    this.extension = ext;
  }

  /**
   * Build Graalvm Image.
   *
   * @param execOperations {@link ExecOperations}
   * @param project {@link Project}
   * @param buildDir {@link Path}
   * @param graalvmBaseDir {@link File}
   * @param outputDir {@link File}
   */
  public void buildGraalvmImage(final ExecOperations execOperations, final Project project,
      final Path buildDir, final File graalvmBaseDir, File outputDir) {

    List<String> args = getBuildGraalvmImageArguments(project, buildDir);

    execOperations.exec(arg0 -> {
      String executeable =
          OperatingSystem.current().isWindows() ? "native-image.cmd" : "native-image";
      arg0.setCommandLine(
          Paths.get(getGraalBin(graalvmBaseDir).toAbsolutePath().toString(), "/" + executeable)
              .toFile());
      arg0.args(args);
      arg0.setWorkingDir(outputDir);
    });
  }

  /**
   * Build Graalvm classes folder.
   *
   * @param project {@link Project}
   * @param buildDir {@link Path}
   */
  public void buildGraalvmJavaMain(final Project project, final Path buildDir) {
    new RuntimeDependenciesDecompress(project).apply(buildDir);
  }

  List<String> getBuildGraalvmImageArguments(final Project project, final Path buildDir) {

    List<String> args = new ArrayList<>(new GraalvmParameterToStrings().apply(this.extension));

    String executableName = this.extension.getOutputFileName();

    if (executableName != null) {
      args.add("-H:Name=" + getExecutableName(project));
    }

    args.addAll(new GraalvmClasspathArguments(buildDir).apply(this.extension));

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
   * @param execOperations {@link ExecOperations}
   * @param graalvmBaseDir {@link File}
   * @throws IOException IOException
   */
  public void runGuInstallation(final ExecOperations execOperations, final Path graalvmBaseDir)
      throws IOException {

    String guExecutable = OperatingSystem.current().isWindows() ? "gu.cmd" : "gu";
    Path gu = getGraalBin(graalvmBaseDir.toFile()).resolve(guExecutable);

    if (gu.toFile().exists()) {
      execOperations.exec(arg0 -> {
        arg0.setCommandLine(gu.toFile());
        arg0.args(Arrays.asList("install", "native-image"));
      });
    }
  }

  /**
   * Run Native Image Command.
   *
   * @param execOperations {@link ExecOperations}
   * @param project {@link Project}
   * @param buildDir {@link Path}
   * @param graalvmBaseDir {@link Files}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void runNativeImage(final ExecOperations execOperations, final Project project,
      final Path buildDir, final File graalvmBaseDir, File outputDir) throws IOException {

    buildGraalvmJavaMain(project, buildDir);

    buildGraalvmImage(execOperations, project, buildDir, graalvmBaseDir, outputDir);
  }
}
