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
package com.formkiq.gradle;

import com.formkiq.gradle.internal.ArchiveUtils;
import com.formkiq.gradle.internal.Downloader;
import com.formkiq.gradle.internal.GradleUtils;
import com.formkiq.gradle.internal.NativeImageExecutor;
import com.formkiq.gradle.services.DefaultDockerService;
import com.formkiq.gradle.services.DockerService;
import com.formkiq.gradle.services.DockerfileGenerator;
import com.formkiq.gradle.services.GraalVmUrlBuilder;
import com.formkiq.gradle.services.Platform;
import com.formkiq.gradle.services.RuntimeDependenciesDecompress;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.resources.ResourceException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Graalvm Build Task Plugin. */
public class GraalvmNativeTask extends DefaultTask {

  /** {@link GraalvmNativeExtension}. */
  private GraalvmNativeExtension extension;

  /** {@link ArchiveUtils}, */
  private final ArchiveUtils archiveUtils = new ArchiveUtils();

  /** {@link Downloader}. */
  private final Downloader downloader = new Downloader();

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

  /** Create GraalVM Image. */
  @TaskAction
  public void createImage() {

    Path buildDir = Path.of(getProject().getBuildDir().getAbsolutePath(), "graalvm");

    try {

      Path toFile = this.extension.getImageFile() != null ? Path.of(this.extension.getImageFile())
          : Path.of(buildDir.toFile().getAbsolutePath(), getFilename());

      NativeImageExecutor executor = new NativeImageExecutor(this.extension);

      if (this.extension.getDockerImage() != null) {

        DockerService service = new DefaultDockerService();
        if (!service.isDockerRunning()) {
          throw new ResourceException("Docker is not running");
        }

        new RuntimeDependenciesDecompress().apply(getProject());

        DockerfileGenerator gen = DockerfileGenerator.builder()
            .baseImage(this.extension.getDockerImage()).addNativeImageArgs(this.extension)
            .mainClass(this.extension.getMainClassName()).build();

        String dockerfileContent = gen.generateContents();
        getLogger().info(MessageFormat.format("Generating Dockerfile {0}", dockerfileContent));
        service.buildDockerfile(dockerfileContent);

      } else {

        if (this.extension.getImageFile() == null) {
          List<String> urls =
              GraalVmUrlBuilder.builder().withJavaVersion(this.extension.getJavaVersion())
                  .withVersion(this.extension.getImageVersion()).withPlatform(Platform.detect())
                  .build();
          downloader.download(urls, toFile);
        }

        archiveUtils.decompress(toFile.toFile(), buildDir.toFile());

        Optional<String> folder = getFirstSubdirectory(buildDir);

        Path graalvmBaseDir = Path.of(buildDir.toFile().getAbsolutePath(), folder.get());

        File generatedFileDir = getOutputDirectory();
        Path path = Path.of(generatedFileDir.getAbsolutePath(), "java");
        if (path.toFile().exists()) {
          deleteDirectory(path);
        }

        executor.start(getProject());

        executor.runGuInstallation(getProject(), graalvmBaseDir.toFile());
        executor.runNativeImage(getProject(), graalvmBaseDir.toFile(), generatedFileDir);
      }

      // if (this.extension.getDockerImage() == null && this.extension.getImageFile() == null)
      // {
      //
      // List<String> urls = GraalVmUrlBuilder.builder()
      // .withJavaVersion(this.extension.getJavaVersion())
      //
      // .withVersion(this.extension.getImageVersion()).withPlatform(Platform.detect()).build();
      // new Downloader().download(urls, toFile);
      // }

      // NativeImageExecutor executor = new NativeImageExecutor(this.extension);
      //
      // boolean decompressed = this.extension.getDockerImage() != null
      // || new ArchiveUtils().decompress(toFile.toFile(), buildDir.toFile());
      //
      // if (this.extension.getDockerImage() != null) {
      // DockerUtils docker = new DockerUtils();
      // docker.isDockerInstalled(getProject());
      // }
      //
      // if (decompressed) {
      //
      // try {
      //
      // File generatedFileDir = getOutputDirectory();
      // Path path = Path.of(generatedFileDir.getAbsolutePath(), "java");
      // if (path.toFile().exists()) {
      // deleteDirectory(path);
      // }
      //
      // executor.start(getProject(), generatedFileDir);
      //
      // Path graalvmBaseDir = Path.of(buildDir.toFile().getAbsolutePath(),
      // getFilenameShort());
      // executor.runGuInstallation(getProject(), graalvmBaseDir.toFile());
      // executor.runNativeImage(getProject(), graalvmBaseDir.toFile(), generatedFileDir);
      //
      // } finally {
      // executor.stop(getProject());
      // }
      // }

    } catch (IOException e) {
      e.printStackTrace();
      throw new ResourceException(e.getMessage(), e);
    }
  }

  /**
   * @param directory the path to a real directory on disk (absolute or relative)
   * @return the name of the first subdirectory (alphabetical order), or empty if none
   * @throws IllegalArgumentException if dirPath does not exist or is not a directory
   * @throws IOException if an I/O error occurs while reading the directory
   */
  private Optional<String> getFirstSubdirectory(final Path directory) throws IOException {

    // 1) Make sure the path exists and is a directory
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      throw new IllegalArgumentException("Not a directory: " + directory);
    }

    // 2) List entries, filter only directories, sort by name, and pick the first
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      Optional<Path> firstDir = Files.list(directory).filter(Files::isDirectory).sorted() // sort by
          // Path’s
          // natural
          // order
          // (alphabetical)
          .findFirst();

      return firstDir.map(Path::getFileName).map(Path::toString);
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
      default:
        arch = "amd64";
        break;
    }

    return arch;
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
    return this.extension.getJavaVersion();
  }

  /**
   * Output File Directory.
   *
   * @return {@link File}
   */
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
    return GradleUtils.getRuntimeClasspath(getProject());
  }

  /**
   * Source Input Directory.
   *
   * @return {@link File}
   */
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
