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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

/**
 * A simple functional test for the 'com.formkiq.gradle.graalvm-native-plugin' plugin.
 */
public class GraalvmNativePluginFunctionalTest {

  /**
   * Test Graalvm Task can run.
   * 
   * @throws IOException IOException
   */
  @Test
  public void canRunTask() throws IOException {
    // given
    deleteOutputFile();

    File projectDir = new File("build/functionalTest");
    Files.createDirectories(Path.of(projectDir.getAbsolutePath(), "src/main/java"));

    Path classpath = Path.of(projectDir.getAbsolutePath(), "../classes/java/functionalTest");

    writeString(new File(projectDir, "settings.gradle"), "");
    writeString(new File(projectDir, "build.gradle"),
        "plugins {" + "  id('com.formkiq.gradle.graalvm-native-plugin')\n" + "id('java-library')\n"
            + " }\n" + "nativeImage {\n" + " addClasspath = '"
            + classpath.toFile().getCanonicalPath() + "'\n"
            + " mainClassName = 'com.formkiq.gradle.Test'\n"
            + "enableReportExceptionStackTraces = true }");

    // when
    GradleRunner runner = GradleRunner.create();
    runner.forwardOutput();
    runner.withPluginClasspath();
    runner.withArguments("graalvmNativeImage");
    runner.withProjectDir(projectDir);

    BuildResult result = runner.build();

    // verify
    assertTrue(result.getOutput().contains("Task :graalvmNativeImage"));
    File file = new File("build/functionalTest/build/graalvm/functionalTest");
    assertTrue(file.exists());

    CommandLine cmdLine = new CommandLine(file);
    DefaultExecutor executor = new DefaultExecutor();
    int exitValue = executor.execute(cmdLine);
    assertEquals(0, exitValue);
  }

  /**
   * Test Graalvm Task can run build by Docker.
   * 
   * @throws IOException IOException
   */
  @Test
  public void canRunTaskAsDocker() throws IOException {
    // given
    deleteOutputFile();

    File projectDir = new File("build/functionalTest");
    Files.createDirectories(Path.of(projectDir.getAbsolutePath(), "src/main/java"));

    Path classpath = Path.of(projectDir.getAbsolutePath(), "../classes/java/functionalTest");

    writeString(new File(projectDir, "settings.gradle"), "");
    writeString(new File(projectDir, "build.gradle"),
        "plugins {" + "  id('com.formkiq.gradle.graalvm-native-plugin')\n" + "id('java-library')\n"
            + " }\n" + "nativeImage {\n" + " addClasspath = '"
            + classpath.toFile().getCanonicalPath() + "'\n" + " dockerImage = 'ghcr.io/graalvm/graalvm-ce:java11-21.3.2'\n"
            + " mainClassName = 'com.formkiq.gradle.Test'\n" + "}");

    // when
    GradleRunner runner = GradleRunner.create();
    runner.forwardOutput();
    runner.withPluginClasspath();
    runner.withArguments("graalvmNativeImage");
    runner.withProjectDir(projectDir);

    BuildResult result = runner.build();

    // verify
    assertTrue(result.getOutput().contains("Task :graalvmNativeImage"));
    File file = new File("build/functionalTest/build/graalvm/functionalTest");
    assertTrue(file.exists());
  }

  private void writeString(File file, String string) throws IOException {
    try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
      writer.write(string);
    }
  }

  private void deleteOutputFile() {
    File file = new File("build/functionalTest/build/graalvm/functionalTest");
    if (file.exists()) {
      assertTrue(file.delete());
    }
  }
}
