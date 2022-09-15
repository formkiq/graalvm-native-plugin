/**
 * Copyright [2020] FormKiQ Inc. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.formkiq.gradle.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.ExecSpec;
import com.formkiq.gradle.GraalvmNativeExtension;

/**
 * 
 * GraalVM Native-Image {@link DefaultExecutor}.
 *
 */
public class NativeImageExecutor {

  private static final String GRAALVM_JAVA_MAIN = "graalvm/java/main";

  /** {@link GraalvmNativeExtension}. */
  private GraalvmNativeExtension extension;
  /** {@link DockerUtils}. */
  private DockerUtils docker;

  /**
   * constructor.
   * 
   * @param ext {@link GraalvmNativeExtension}
   */
  public NativeImageExecutor(final GraalvmNativeExtension ext) {
    this.extension = ext;
    this.docker = new DockerUtils();
  }

  private void addBooleanArgument(final List<String> args, final Boolean bool,
      final String argument) {
    if (Boolean.TRUE.equals(bool)) {
      args.add(argument);
    }
  }

  private List<String> getBuildGraalvmImageArguments(final Project project, final File outputDir) {

    List<String> args = new ArrayList<>();
    args.add("--report-unsupported-elements-at-runtime");
    args.add("--no-server");

    addBooleanArgument(args, this.extension.isEnableFallback(), "--no-fallback");
    addBooleanArgument(args, this.extension.isAllowIncompleteClasspath(),
        "--allow-incomplete-classpath");
    addBooleanArgument(args, this.extension.isEnableInstallExitHandlers(),
        "--install-exit-handlers");
    addBooleanArgument(args, this.extension.isEnableHttp(), "--enable-http");
    addBooleanArgument(args, this.extension.isEnableHttps(), "--enable-https");
    addBooleanArgument(args, this.extension.isEnableVerbose(), "--verbose");
    addBooleanArgument(args, this.extension.isEnableAutofallback(), "--auto-fallback");
    addBooleanArgument(args, this.extension.isEnableForceFallback(), "--force-fallback");
    addBooleanArgument(args, this.extension.isEnableAllSecurityServices(),
        "--enable-all-security-services");
    addBooleanArgument(args, this.extension.isEnableShared(), "--shared");
    addBooleanArgument(args, this.extension.isEnableStatic(), "--static");

    addBooleanArgument(args, this.extension.isEnableAddAllCharsets(), "-H:+AddAllCharsets");
    addStringListArgument(args, this.extension.getInitializeAtBuildTime(),
        "--initialize-at-build-time");
    addStringListArgument(args, this.extension.getInitializeAtRunTime(),
        "--initialize-at-run-time");

    for (String property : this.extension.getSystemProperty()) {
      addStringArgument(args, property, "-D" + property);
    }

    String reflectConfig = this.extension.getReflectionConfig();
    if (reflectConfig != null) {
      addStringArgument(args, reflectConfig, "-H:ReflectionConfigurationFiles=" + reflectConfig);
    }

    String serializationConfig = this.extension.getSerializationConfig();
    if (serializationConfig != null) {
      addStringArgument(args, serializationConfig,
          "-H:SerializationConfigurationResources=" + serializationConfig);
    }

    String jniConfig = this.extension.getJniConfigurationFiles();
    if (jniConfig != null) {
      addStringArgument(args, jniConfig, "-H:JNIConfigurationFiles=" + jniConfig);
    }
    
    String resourceConfig = this.extension.getResourceConfigurationFiles();
    if (resourceConfig != null) {
      addStringArgument(args, resourceConfig, "-H:ResourceConfigurationFiles=" + resourceConfig);
    }
    
    addStringArgument(args, this.extension.getFeatures(),
        "--features=" + this.extension.getFeatures());

    addBooleanArgument(args, this.extension.isEnableInstallExitHandlers(),
        "--install-exit-handlers");

    addStringArgument(args, this.extension.getTraceClassInitialization(),
        "--trace-class-initialization=" + this.extension.getTraceClassInitialization());
    addBooleanArgument(args, this.extension.isEnableRemoveSaturatedTypeFlows(),
        "-H:+RemoveSaturatedTypeFlows");
    addBooleanArgument(args, this.extension.isEnableReportExceptionStackTraces(),
        "-H:+ReportExceptionStackTraces");
    addBooleanArgument(args, this.extension.isEnablePrintAnalysisCallTree(),
        "-H:+PrintAnalysisCallTree");
    addBooleanArgument(args, this.extension.isEnableCheckToolchain(), "-H:-CheckToolchain");
    addBooleanArgument(args, this.extension.isEnableReportUnsupportedElementsAtRuntime(),
        "-H:+ReportUnsupportedElementsAtRuntime");

    args.add("-H:Name=" + getExecutableName(project));

    args.add("-cp");
    args.add(buildClassPathString(project));

    args.add(this.extension.getMainClassName());

    return args;
  }

  private String getExecutableName(final Project project) {
    return this.extension.getOutputFileName() != null ? this.extension.getOutputFileName()
        : project.getName();
  }

  private void addStringArgument(List<String> args, String s, String argument) {
    if (s != null) {
      args.add(argument);
    }
  }

  private void addStringListArgument(final List<String> args, final List<String> list,
      final String argument) {
    if (!list.isEmpty()) {
      args.add(argument + "=" + String.join(",", list));
    }
  }

  private String buildClassPathString(final Project project) {

    List<File> files = buildClassPath(project);

    return files.stream().map(File::getAbsolutePath)
        .collect(Collectors.joining(OperatingSystem.current().isWindows() ? ";" : ":"));
  }

  private List<File> buildClassPath(final Project project) {
    List<File> files = new ArrayList<>();
    files.add(Path.of(project.getBuildDir().getAbsolutePath(), GRAALVM_JAVA_MAIN).toFile());

    if (this.extension.getAddClasspath() != null) {
      String[] cp = this.extension.getAddClasspath().split(",");
      for (String c : cp) {
        files.add(new File(c));
      }
    }
    return files;
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

    List<String> args = getBuildGraalvmImageArguments(project, outputDir);

    if (this.extension.isEnableDocker().booleanValue()) {

      List<String> a = new ArrayList<>();
      a.add("native-image");
      a.addAll(args);

      this.docker.exec(project, a);

    } else {

      project.exec(new Action<ExecSpec>() {
        @Override
        public void execute(ExecSpec arg0) {

          String executeable =
              OperatingSystem.current().isWindows() ? "native-image.cmd" : "native-image";
          arg0.setCommandLine(
              Paths.get(getGraalBin(graalvmBaseDir).toAbsolutePath().toString(), "/" + executeable)
                  .toFile());
          arg0.args(args);
          arg0.setWorkingDir(outputDir);
        }
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

    ArchiveUtils archiveUtils = new ArchiveUtils();

    Path outputPath = Path.of(project.getBuildDir().getCanonicalPath(), GRAALVM_JAVA_MAIN);
    File outputdir = outputPath.toFile();

    List<File> classPathFiles = GradleUtils.getRuntimeClasspath(project);

    for (File file : classPathFiles) {
      archiveUtils.decompressJar(file, outputdir);
    }

    List<File> files = Files.list(Path.of(project.getBuildDir().getAbsolutePath(), "libs"))
        .map(f -> f.toFile()).collect(Collectors.toList());

    for (File file : files) {
      archiveUtils.decompressJar(file, outputdir);
    }
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
   * @return boolean
   * @throws IOException IOException
   */
  public boolean runGuInstallation(final Project project, final File graalvmBaseDir)
      throws IOException {

    if (this.extension.isEnableDocker().booleanValue()) {

      this.docker.exec(project, Arrays.asList("gu", "install", "native-image"));

    } else {

      project.exec(new Action<ExecSpec>() {
        @Override
        public void execute(ExecSpec arg0) {

          String gu = OperatingSystem.current().isWindows() ? "gu.cmd" : "gu";
          arg0.setCommandLine(Paths
              .get(getGraalBin(graalvmBaseDir).toAbsolutePath().toString(), "/" + gu).toFile());
          arg0.args(Arrays.asList("install", "native-image"));
        }
      });
    }

    return true;
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

    if (this.extension.isEnableDocker().booleanValue()) {
      this.docker.copy(project, new File("/" + getExecutableName(project)), outputDir);
    }
  }

  /**
   * Start Native-Image Executor.
   * 
   * @param project {@link Project}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void start(final Project project, File outputDir) throws IOException {

    if (this.extension.isEnableDocker().booleanValue()) {
      this.docker.startImage(project, this.extension, buildClassPath(project));
    }
  }

  /**
   * Stop Native-Image Executor.
   * 
   * @param project {@link Project}
   * @throws IOException IOException
   */
  public void stop(final Project project) throws IOException {
    if (this.extension.isEnableDocker().booleanValue()) {
      this.docker.stopImage(project);
    }
  }
}
