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

import static org.junit.Assert.assertNotNull;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

/**
 * Unit test for the 'com.formkiq.gradle.graalvm-native-plugin' plugin.
 */
public class GraalvmNativePluginTest {

  /**
   * Test Registering Task.
   */
  @Test
  public void pluginRegistersATask() {
    // Create a test project and apply the plugin
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java-gradle-plugin");
    project.getPlugins().apply("com.formkiq.gradle.graalvm-native-plugin");

    // Verify the result
    assertNotNull(project.getTasks().findByName("graalvmNativeImage"));
  }
}
