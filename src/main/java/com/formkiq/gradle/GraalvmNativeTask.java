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
package com.formkiq.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.resources.ResourceException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import com.formkiq.gradle.internal.ArchiveUtils;
import com.formkiq.gradle.internal.DockerUtils;
import com.formkiq.gradle.internal.Downloader;
import com.formkiq.gradle.internal.GradleUtils;
import com.formkiq.gradle.internal.NativeImageExecutor;

/** Graalvm Build Task Plugin. */
public class GraalvmNativeTask extends DefaultTask {

  /** Supported Java Versions. */
  private static final List<String> SUPPORTED_JAVA_VERSIONS =
      Arrays.asList("java11", "java8", "java17");

  /** {@link GraalvmNativeExtension}. */
  private GraalvmNativeExtension extension;

  /**
   * constructor.
   * 
   * @param objects {@link ObjectFactory}
   */
  @Inject
  public GraalvmNativeTask(final ObjectFactory objects) {
    this.setGroup("build");
    this.setDescription("Builds a native image for Java applications using GraalVM tools");
  }

  /**
   * Create GraalVM Image.
   */
  @TaskAction
  public void createImage() {

    File buildDir = Path.of(getProject().getBuildDir().getAbsolutePath(), "graalvm").toFile();

    try {

      Path toFile = this.extension.getImageFile() != null ? Path.of(this.extension.getImageFile())
          : Path.of(buildDir.getAbsolutePath(), getFilename());

      if (!this.extension.isEnableDocker().booleanValue()
          && this.extension.getImageFile() == null) {
        new Downloader().download(getDownloadUrl(), toFile);
      }

      NativeImageExecutor executor = new NativeImageExecutor(this.extension);

      boolean decompressed = this.extension.isEnableDocker().booleanValue()
          || new ArchiveUtils().decompress(toFile.toFile(), buildDir);

      if (this.extension.isEnableDocker().booleanValue()) {
        DockerUtils docker = new DockerUtils();
        if (!docker.isDockerInstalled(getProject())) {
          throw new ResourceException("Cannot find Docker command in path");
        }
      }

      if (decompressed) {

        try {

          File generatedFileDir = getOutputDirectory();
          Path path = Path.of(generatedFileDir.getAbsolutePath(), "java");
          if (path.toFile().exists()) {
            deleteDirectory(path);
          }

          executor.start(getProject(), generatedFileDir);

          Path graalvmBaseDir = Path.of(buildDir.getAbsolutePath(), getFilenameShort());
          executor.runGuInstallation(getProject(), graalvmBaseDir.toFile());
          executor.runNativeImage(getProject(), graalvmBaseDir.toFile(), generatedFileDir);

        } finally {
          executor.stop(getProject());
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
      throw new ResourceException(e.getMessage(), e);
    }
  }

  private void deleteDirectory(Path pathToBeDeleted) throws IOException {
    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile)
        .forEach(File::delete);
  }

  private String getArchitecture() {

    String osArch = System.getProperty("os.arch").toLowerCase();
    String arch = osArch.startsWith("arm") ? "aarch64" : null;

    switch (osArch) {
      case "x86":
      case "i386":
        arch = "386";
        break;
      case "x86_64":
      case "amd64":
        arch = "amd64";
        break;
      default:
        break;
    }

    return arch;
  }

  private String getDownloadUrl() {
    return "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-"
        + this.extension.getImageVersion() + "/" + getFilename();
  }

  private String getExtension() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows") ? "zip" : "tar.gz";
  }

  private String getFilename() {
    return MessageFormat.format("graalvm-ce-{0}-{1}-{2}-{3}.{4}", getJavaVersion(), getPlatform(),
        getArchitecture(), this.extension.getImageVersion(), getExtension());
  }

  private String getFilenameShort() {
    return MessageFormat.format("graalvm-ce-{0}-{1}", getJavaVersion(),
        this.extension.getImageVersion());
  }

  private String getJavaVersion() {

    String javaVersion = this.extension.getJavaVersion();

    if (!SUPPORTED_JAVA_VERSIONS.contains(javaVersion)) {
      throw new ResourceException(
          "Java Version must be one of: " + String.join(",", SUPPORTED_JAVA_VERSIONS));
    }

    return javaVersion;
  }

  /**
   * Output File Directory.
   * 
   * @return {@link File}
   */
  @SuppressWarnings("resource")
  @OutputDirectory
  public File getOutputDirectory() {
    return FileSystems.getDefault().getPath(getProject().getBuildDir().getAbsolutePath(), "graalvm")
        .toFile();
  }

  private String getPlatform() {
    String os = System.getProperty("os.name").toLowerCase();
    String platform = os.startsWith("windows") ? "windows" : null;
    platform = os.startsWith("mac") ? "darwin" : platform;
    platform = platform != null ? platform : "linux";
    return platform;
  }

  /**
   * Get Runtime Classpath.
   * 
   * @return {@link Collection} {@link File}
   * @throws IOException IOException
   */
  @Input
  public Collection<File> getRuntimeClasspath() throws IOException {
    List<File> classPathFiles = GradleUtils.getRuntimeClasspath(getProject());
    return classPathFiles;
  }

  /**
   * Source Input Directory.
   * 
   * @return {@link File}
   */
  @SuppressWarnings("resource")
  @InputDirectory
  public File getSourceFileDir() {
    return FileSystems.getDefault()
        .getPath(getProject().getProjectDir().getAbsolutePath(), "src/main/java").toFile();
  }

  /**
   * Set {@link GraalvmNativeExtension}.
   * 
   * @param ext {@link GraalvmNativeExtension}
   */
  public void setExtension(final GraalvmNativeExtension ext) {
    this.extension = ext;
  }
}
