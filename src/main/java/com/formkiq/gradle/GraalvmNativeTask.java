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
import com.formkiq.gradle.internal.NativeImageExecutor;
import com.formkiq.gradle.services.DefaultDockerService;
import com.formkiq.gradle.services.DockerService;
import com.formkiq.gradle.services.DockerfileGenerator;
import com.formkiq.gradle.services.GraalVmUrlBuilder;
import com.formkiq.gradle.services.Platform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.resources.ResourceException;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/** Graalvm Build Task Plugin. */
public abstract class GraalvmNativeTask extends DefaultTask {

  /**
   * Java + resources that influence the native image.
   *
   * @return ConfigurableFileCollection
   */
  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getSources();

  /**
   * Runtime classpath (jars/classes/resources) for native-image.
   *
   * @return ConfigurableFileCollection
   */
  @InputFiles
  @Classpath
  public abstract ConfigurableFileCollection getRuntimeClasspath();

  /**
   * Output Directory binary.
   *
   * @return DirectoryProperty
   */
  @OutputDirectory
  public abstract DirectoryProperty getBuildDirectory();

  /**
   * Use {@link ExecOperations} instead of project.exec(...).
   *
   * @return ExecOperations
   */
  @Inject
  protected abstract ExecOperations getExecOperations();

  /** {@link ArchiveUtils}, */
  private final ArchiveUtils archiveUtils = new ArchiveUtils();

  /** {@link Downloader}. */
  private final Downloader downloader = new Downloader();

  // The extension with your ~20 inputs:
  private GraalvmNativeExtension extension;

  /**
   * Set Extension.
   *
   * @param params {@link GraalvmNativeExtension}
   */
  public void setExtension(final GraalvmNativeExtension params) {
    this.extension = params;
  }

  /** Create GraalVM Image. */
  @TaskAction
  public void createImage() {

    if (extension.getMainClassName() != null) {
      try {

        NativeImageExecutor executor = new NativeImageExecutor(this.extension);

        if (this.extension.getDockerFile() != null) {
          executeDockerFile();
        } else if (this.extension.getDockerImage() != null) {

          executeDockerImage(executor);

        } else {

          Path buildDirGraalvm = getBuildDirectoryAsPath().resolve("graalvm");
          Path toFile = buildDirGraalvm.resolve(getFilename());

          if (this.extension.getImageFile() == null) {
            List<String> urls =
                GraalVmUrlBuilder.builder().withJavaVersion(this.extension.getJavaVersion())
                    .withVersion(this.extension.getImageVersion()).withPlatform(Platform.detect())
                    .build();
            downloader.download(urls, toFile);
          }

          archiveUtils.decompress(toFile.toFile(), buildDirGraalvm.toFile());

          String folder = getFirstSubdirectory(buildDirGraalvm);
          Path graalvmBaseDir = buildDirGraalvm.resolve(folder);

          Path path = getBuildDirectoryAsPath().resolve("java/main");
          if (path.toFile().exists()) {
            deleteDirectory(path);
          }

          executor.runGuInstallation(getExecOperations(), graalvmBaseDir);
          executor.runNativeImage(getExecOperations(), getProject(), getBuildDirectoryAsPath(),
              graalvmBaseDir.toFile(), path.toFile(), getRuntimeClasspath());
        }

      } catch (IOException | InterruptedException e) {
        throw new ResourceException(e.getMessage(), e);
      }
    }
  }

  private Path getBuildDirectoryAsPath() {
    return getBuildDirectory().get().getAsFile().toPath();
  }

  private void executeDockerFile() throws IOException, InterruptedException {

    DockerService service = new DefaultDockerService();
    if (!service.isDockerRunning()) {
      throw new ResourceException("Docker is not running");
    }

    String dockerfileContent = Files.readString(Path.of(this.extension.getDockerFile()));
    getLogger().info("Generating Dockerfile");
    getLogger().info("{}", dockerfileContent);

    service.removeDockerImage(this.extension.getOutputImageTag());

    Path buildDir = getBuildDirectoryAsPath();
    service.buildDockerImage(buildDir, this.extension.getOutputImageTag(), dockerfileContent);
    service.runDockerImage(buildDir, this.extension.getOutputImageTag());
  }

  private void executeDockerImage(NativeImageExecutor executor)
      throws IOException, InterruptedException {

    DockerService service = new DefaultDockerService();
    if (!service.isDockerRunning()) {
      throw new ResourceException("Docker is not running");
    }

    getRuntimeClasspath().forEach(path -> getLogger().info("FOUND PATH: " + path));
    executor.buildGraalvmJavaMain(getBuildDirectoryAsPath(), getRuntimeClasspath());

    DockerfileGenerator.Builder builder =
        DockerfileGenerator.builder().baseImage(this.extension.getDockerImage())
            .addNativeImageArgs(this.extension).mainClass(this.extension.getMainClassName().get());

    if (this.extension.getOutputFileName() != null) {
      builder.addNativeImageArg("-H:Name=" + this.extension.getOutputFileName());
    }

    String dockerfileContent = builder.build().generateContents(getBuildDirectoryAsPath());
    getLogger().info("Generating Dockerfile");
    getLogger().info("{}", dockerfileContent);

    service.removeDockerImage(this.extension.getOutputImageTag());

    Path buildDir = getBuildDirectoryAsPath();
    service.buildDockerImage(buildDir, this.extension.getOutputImageTag(), dockerfileContent);
    service.runDockerImage(buildDir, this.extension.getOutputImageTag());
  }

  /**
   * @param directory the path to a real directory on disk (absolute or relative)
   * @return the name of the first subdirectory (alphabetical order), or empty if none
   * @throws IllegalArgumentException if dirPath does not exist or is not a directory
   * @throws IOException if an I/O error occurs while reading the directory
   */
  private String getFirstSubdirectory(final Path directory) throws IOException {

    // 1) Make sure the path exists and is a directory
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      throw new IllegalArgumentException("Not a directory: " + directory);
    }

    // 2) List entries, filter only directories, sort by name, and pick the first
    // sort by Path’s natural order (alphabetical)
    Optional<Path> firstDir;
    try (Stream<Path> stream = Files.list(directory)) {
      firstDir = stream.filter(Files::isDirectory).sorted().findFirst();
    }

    return firstDir.map(Path::getFileName).map(Path::toString).orElseThrow();
  }

  private void deleteDirectory(Path pathToBeDeleted) throws IOException {
    try (Stream<Path> walker = Files.walk(pathToBeDeleted)) {
      walker.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  private String getFilenameExtension() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows") ? "zip" : "tar.gz";
  }

  private String getFilename() {
    return MessageFormat.format("graalvm-ce.{0}", getFilenameExtension());
  }
}
