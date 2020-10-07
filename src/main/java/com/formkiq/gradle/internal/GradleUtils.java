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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;

/**
 * 
 * Gradle Utilities.
 *
 */
public class GradleUtils {

  /**
   * private constructor.
   */
  private GradleUtils() {}

  /**
   * Get Runtime Class Path Files.
   * 
   * @param project {@link Project}
   * @return {@link List} {@link File}
   * @throws IOException IOException
   */
  public static List<File> getRuntimeClasspath(final Project project) throws IOException {

    List<File> files = new ArrayList<>();

    files.addAll(Files.list(Path.of(project.getBuildDir().getAbsolutePath(), "libs"))
        .map(f -> f.toFile()).collect(Collectors.toList()));

    ConfigurationContainer configurations = project.getConfigurations();
    Configuration runtimeClasspath = configurations.getAt("runtimeClasspath");

    if (runtimeClasspath != null) {
      files.addAll(runtimeClasspath.getFiles());
    }

    return files;
  }
}
