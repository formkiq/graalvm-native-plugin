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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.UnknownConfigurationException;

/** Gradle Utilities. */
public class GradleUtils {

  /** private constructor. */
  private GradleUtils() {}

  /**
   * Get Runtime Class Path Files.
   *
   * @param project {@link Project}
   * @param buildDir {@link Path}
   * @return {@link List} {@link File}
   * @throws IOException IOException
   */
  public static List<File> getRuntimeClasspath(final Project project, final Path buildDir)
      throws IOException {

    List<File> files = new ArrayList<>();

    Path path = buildDir.resolve("libs");

    if (path.toFile().exists()) {
      try (Stream<Path> stream = Files.list(path)) {
        files.addAll(stream.map(Path::toFile).toList());
      }
    }

    ConfigurationContainer configurations = project.getConfigurations();
    try {
      Configuration runtimeClasspath = configurations.getAt("runtimeClasspath");
      files.addAll(runtimeClasspath.getFiles());
    } catch (UnknownConfigurationException e) {
      // ignore
    }

    return files;
  }
}
