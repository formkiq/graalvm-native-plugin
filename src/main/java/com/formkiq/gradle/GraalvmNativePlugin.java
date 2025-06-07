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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;

/** GraalVM Plugin to build a native-image from a Java application. */
public class GraalvmNativePlugin implements Plugin<Project> {

  @Override
  public void apply(final Project project) {

    project.getPluginManager().apply(JavaPlugin.class);

    GraalvmNativeExtension ext = project.getExtensions().create("nativeImage",
        GraalvmNativeExtension.class, project.getObjects());

    Provider<GraalvmBuildService> svc = project.getGradle().getSharedServices().registerIfAbsent(
        "web", GraalvmBuildService.class, spec -> spec.getMaxParallelUsages().set(1));

    project.getTasks().register("graalvmNativeImage", GraalvmNativeTask.class, task -> {
      task.setGroup("Graalvm");
      task.setDescription("Build GraalVM Native Image");
      task.setExtension(ext);
      task.usesService(svc);
    });

    project.afterEvaluate(
        p -> p.getTasks().named("graalvmNativeImage").configure(t -> t.dependsOn("check")));
  }
}
