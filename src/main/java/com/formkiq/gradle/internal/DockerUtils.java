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

import static com.formkiq.gradle.internal.Strings.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;
import com.formkiq.gradle.GraalvmNativeExtension;

/**
 * 
 * Docker Utilities.
 *
 */
public class DockerUtils {

  /** Docker Container Id. */
  private String containerId;

  /**
   * Exec Docker.
   * 
   * @param project {@link Project}
   * @param workingDir {@link String}
   * @param args {@link List}
   * @return boolean
   * @throws IOException IOException
   */
  public boolean exec(final Project project, final String workingDir, final List<String> args)
      throws IOException {

    List<String> a = new ArrayList<>();
    a.add("exec");

    if (workingDir != null) {
      a.add("--workdir");
      a.add(workingDir);
    }

    a.add(this.containerId);
    a.addAll(args);

    project.getLogger().lifecycle("running: docker " + String.join(" ", a));

    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.setArgs(a);
      }
    });

    project.getLogger().lifecycle("result: " + result.toString());

    return result.getExitValue() == 0;
  }

  /**
   * Is Docker Installed in the system.
   * 
   * @param project {@link Project}
   * 
   * @throws IOException IOException
   */
  public void isDockerInstalled(final Project project) throws IOException {

    try {
      ExecResult result = project.exec(new Action<ExecSpec>() {
        @Override
        public void execute(ExecSpec arg0) {
          arg0.setCommandLine("docker");
          arg0.args("--version");
        }
      });

      if (result.getExitValue() != 0) {
        throw new IOException(
            "Cannot find a running 'Docker', exit code: " + result.getExitValue());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Cannot find a running 'Docker'");
    }
  }

  /**
   * Pull Docker Image.
   * 
   * @param project {@link Project}
   * @param extension {@link GraalvmNativeExtension}
   * @param imageVersion {@link String}
   * @param javaVersion {@link String}
   * @return boolean
   * @throws IOException IOException
   */
  private boolean pullImage(final Project project, final GraalvmNativeExtension extension,
      final String dockerImage) throws IOException {
    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");

        List<String> args = new ArrayList<>();
        args.add("pull");
        if (extension.getPlatform() != null) {
          args.add("--platform");
          args.add(extension.getPlatform());
        }

        args.add(dockerImage);
        arg0.args(args);
      }
    });
    project.getLogger().debug(result.toString());
    return result.getExitValue() == 0;
  }

  /**
   * Start Docker Image.
   * 
   * @param project {@link Project}
   * @param extension {@link GraalvmNativeExtension}
   * @param classPaths {@link List} {@link File}
   * @return boolean
   * @throws IOException IOException
   */
  public boolean startImage(final Project project, final GraalvmNativeExtension extension,
      final List<File> classPaths) throws IOException {

    String dockerImage = extension.getDockerImage();
    pullImage(project, extension, dockerImage);

    ByteArrayOutputStream so = new ByteArrayOutputStream();

    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {

        arg0.setCommandLine("docker");
        arg0.setStandardOutput(so);

        List<String> args = new ArrayList<>(Arrays.asList("run", "-d"));

        if (extension.getPlatform() != null) {
          args.add("--platform");
          args.add(extension.getPlatform());
        }

        classPaths.forEach(
            cp -> args.addAll(Arrays.asList("-v", formatToUnix(cp) + ":" + formatToUnix(cp))));

        if (extension.getReflectionConfig() != null) {
          args.addAll(Arrays.asList("-v", formatToUnix(extension.getReflectionConfig()) + ":"
              + formatToUnix(extension.getReflectionConfig())));
        }

        try {
          createDirectories(classPaths);

          if (extension.getReflectionConfig() != null) {
            createDirectories(Arrays.asList(new File(extension.getReflectionConfig())));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        args.addAll(Arrays.asList(dockerImage, "sleep", "infinity"));

        arg0.args(args);

        project.getLogger().lifecycle("docker " + String.join(" ", args));
      }

      /**
       * If Docker starts with directories that do not exist under linux the directories will be
       * created as root and the plugin will have permission issues. So we create the directories as
       * the running user before.
       * 
       * @param files {@link List} {@link File}
       * @throws IOException IOException
       */
      private void createDirectories(final List<File> files) throws IOException {

        for (File f : files) {
          if (f.isFile()) {
            Files.createDirectories(Path.of(f.getParentFile().getAbsolutePath()));
          } else {
            Files.createDirectories(Path.of(f.getAbsolutePath()));
          }
        }
      }
    });

    project.getLogger().debug(result.toString());
    this.containerId = new String(so.toByteArray(), StandardCharsets.UTF_8).substring(0, 5);
    project.getLogger().debug("using Docker Container Id: " + this.containerId);
    return result.getExitValue() == 0;
  }

  /**
   * Stop Docker Image.
   * 
   * @param project {@link Project}
   * 
   * @throws IOException IOException
   */
  public void stopImage(final Project project) throws IOException {

    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.setIgnoreExitValue(true);
        arg0.args(Arrays.asList("stop", DockerUtils.this.containerId));
      }
    });

    project.getLogger().debug(result.toString());

    result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.setIgnoreExitValue(true);
        arg0.args(Arrays.asList("rm", DockerUtils.this.containerId));
      }
    });
    project.getLogger().debug(result.toString());
  }

  /**
   * Copy {@link File} from Docker Container to Output Directory.
   * 
   * @param project {@link Project}
   * @param file {@link File}
   * @param outputDir {@link File}
   * @return boolean
   */
  public boolean copy(final Project project, final File file, final File outputDir) {
    ExecResult exec = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.args(Arrays.asList("cp", DockerUtils.this.containerId + ":" + file.getAbsolutePath(),
            outputDir.getAbsolutePath()));
      }
    });

    project.getLogger().debug(exec.toString());
    return exec.getExitValue() == 0;
  }
}
