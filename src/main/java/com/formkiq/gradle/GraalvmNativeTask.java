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
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.resources.ResourceException;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/** Graalvm Build Task Plugin. */
public abstract class GraalvmNativeTask extends DefaultTask {

  /**
   * The directory containing your main source set.
   *
   * @return DirectoryProperty
   */
  @InputDirectory
  public abstract DirectoryProperty getSourceDir();

  /**
   * Get Runtime Classpath.
   *
   * @return ConfigurableFileCollection
   */
  @Classpath
  public abstract ConfigurableFileCollection getRuntimeClasspath();

  /**
   * Where we'll write out the native image.
   *
   * @return DirectoryProperty
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  private final ExecOperations execOperations;
  private final Path buildDirectory;
  private final Path projectDirectory;

  /** {@link GraalvmNativeExtension}. */
  private GraalvmNativeExtension extension;

  /** {@link ArchiveUtils}, */
  private final ArchiveUtils archiveUtils = new ArchiveUtils();

  /** {@link Downloader}. */
  private final Downloader downloader = new Downloader();

  /**
   * constructor.
   *
   * @param layout {@link ProjectLayout}
   * @param configurations {@link ConfigurationContainer}
   * @param execOperations {@link ExecOperations}
   */
  @Inject
  public GraalvmNativeTask(final ProjectLayout layout, final ConfigurationContainer configurations,
      final ExecOperations execOperations) {

    this.execOperations = execOperations;

    // 1) Source files under src/main/java
    getSourceDir().set(layout.getProjectDirectory().dir("src/main/java"));

    // 2) Wire in the Java plugin’s runtimeClasspath
    getRuntimeClasspath().from(configurations.named("runtimeClasspath"));

    // 3) Pick an output folder under build/graalvm
    getOutputDir().set(layout.getBuildDirectory().dir("graalvm"));
    this.setGroup("build");
    this.setDescription("Builds a native image for Java applications using GraalVM tools");
    buildDirectory = layout.getBuildDirectory().get().getAsFile().toPath();
    projectDirectory = layout.getProjectDirectory().getAsFile().toPath();
  }

  /** Create GraalVM Image. */
  @TaskAction
  public void createImage() {

    try {

      NativeImageExecutor executor = new NativeImageExecutor(this.extension);

      if (this.extension.getDockerFile() != null) {
        executeDockerFile();
      } else if (this.extension.getDockerImage() != null) {

        executeDockerImage(executor);

      } else {

        Path buildDirGraalvm = buildDirectory.resolve("graalvm");
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

        Path path = buildDirectory.resolve("graalvm/java/main");
        if (path.toFile().exists()) {
          deleteDirectory(path);
        }

        executor.runGuInstallation(this.execOperations, graalvmBaseDir);
        executor.runNativeImage(this.execOperations, getProject(), buildDirectory,
            graalvmBaseDir.toFile(), path.toFile());
      }

    } catch (IOException | InterruptedException e) {
      throw new ResourceException(e.getMessage(), e);
    }
  }

  private void executeDockerFile() throws IOException, InterruptedException {

    DockerService service = new DefaultDockerService();
    if (!service.isDockerRunning()) {
      throw new ResourceException("Docker is not running");
    }

    String dockerfileContent = Files.readString(Path.of(this.extension.getDockerFile()));
    getLogger().info("Generating Dockerfile {}", dockerfileContent);

    service.removeDockerImage(this.extension.getOutputImageTag());

    Path buildDir = buildDirectory;
    service.buildDockerImage(buildDir, this.extension.getOutputImageTag(), dockerfileContent);
    service.runDockerImage(buildDir, this.extension.getOutputImageTag());
  }

  private void executeDockerImage(NativeImageExecutor executor)
      throws IOException, InterruptedException {
    DockerService service = new DefaultDockerService();
    if (!service.isDockerRunning()) {
      throw new ResourceException("Docker is not running");
    }

    executor.buildGraalvmJavaMain(getProject(), buildDirectory);

    DockerfileGenerator gen = DockerfileGenerator.builder()
        .baseImage(this.extension.getDockerImage()).addNativeImageArgs(this.extension)
        .mainClass(this.extension.getMainClassName()).build();

    String dockerfileContent = gen.generateContents();
    getLogger().info("Generating Dockerfile {}", dockerfileContent);

    service.removeDockerImage(this.extension.getOutputImageTag());

    Path buildDir = buildDirectory;
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

  private String getExtension() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.startsWith("windows") ? "zip" : "tar.gz";
  }

  private String getFilename() {
    return MessageFormat.format("graalvm-ce.{0}", getExtension());
  }

  /**
   * Source Input Directory.
   *
   * @return {@link File}
   */
  @InputDirectory
  public File getSourceFileDir() {
    return projectDirectory.resolve("src").resolve("main").toFile();
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
