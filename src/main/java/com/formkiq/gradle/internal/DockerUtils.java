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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
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
   * @param args {@link List}
   * @return boolean
   * @throws IOException IOException
   */
  public boolean exec(final Project project, final List<String> args) throws IOException {

    List<String> a = new ArrayList<>();
    a.add("exec");
    a.add(this.containerId);
    a.addAll(args);

    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.setArgs(a);
      }
    });

    project.getLogger().debug(result.toString());
    return result.getExitValue() == 0;
  }

  private String getImageName(final String imageVersion, final String javaVersion) {
    return MessageFormat.format("oracle/graalvm-ce:{0}-{1}", imageVersion, javaVersion);
  }

  /**
   * Is Docker Installed in the system.
   * 
   * @param project {@link Project}
   * @return boolean
   * @throws IOException IOException
   */
  public boolean isDockerInstalled(final Project project) throws IOException {
    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.args("--version");
      }
    });
    project.getLogger().debug(result.toString());
    return result.getExitValue() == 0;
  }

  /**
   * Pull Docker Image.
   * 
   * @param project {@link Project}
   * @param imageVersion {@link String}
   * @param javaVersion {@link String}
   * @return boolean
   * @throws IOException IOException
   */
  private boolean pullImage(final Project project, final String imageVersion,
      final String javaVersion) throws IOException {
    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {
        arg0.setCommandLine("docker");
        arg0.args(Arrays.asList("pull", getImageName(imageVersion, javaVersion)));
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

    String imageVersion = extension.getImageVersion();
    String javaVersion = extension.getJavaVersion();
    pullImage(project, imageVersion, javaVersion);

    ByteArrayOutputStream so = new ByteArrayOutputStream();

    ExecResult result = project.exec(new Action<ExecSpec>() {
      @Override
      public void execute(ExecSpec arg0) {

        arg0.setCommandLine("docker");
        arg0.setStandardOutput(so);

        List<String> args = new ArrayList<>(Arrays.asList("run", "-d"));

        classPaths.forEach(cp -> args.addAll(Arrays.asList("-v", cp + ":" + cp)));

        if (extension.getReflectionConfig() != null) {
          args.addAll(Arrays.asList("-v",
              extension.getReflectionConfig() + ":" + extension.getReflectionConfig()));
        }

        try {
          createDirectories(classPaths);

          if (extension.getReflectionConfig() != null) {
            createDirectories(Arrays.asList(new File(extension.getReflectionConfig())));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        args.addAll(Arrays.asList(getImageName(imageVersion, javaVersion), "sleep", "infinity"));

        arg0.args(args);
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
