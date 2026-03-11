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
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/** GraalVM Plugin to build a native-image from a Java application. */
public class GraalvmNativePlugin implements Plugin<Project> {

  @Override
  public void apply(final Project project) {

    GraalvmNativeExtension ext = project.getExtensions().create("nativeImage",
        GraalvmNativeExtension.class, project.getObjects());

    Provider<GraalvmBuildService> svc = project.getGradle().getSharedServices().registerIfAbsent(
        "web", GraalvmBuildService.class, spec -> spec.getMaxParallelUsages().set(1));

    // ✅ Register task immediately so tasks.named(...) always works
    TaskProvider<GraalvmNativeTask> nativeImage =
        project.getTasks().register("graalvmNativeImage", GraalvmNativeTask.class, task -> {
          task.setGroup("Graalvm");
          task.setDescription("Build GraalVM Native Image");
          task.setExtension(ext);
          task.usesService(svc);
          task.getBuildDirectory().set(project.getLayout().getBuildDirectory().dir("graalvm"));
          task.getProjectName().set(project.getName());

          // ✅ Opt-in: task will only run if configured
          task.onlyIf(t -> {
            String dockerFile = ext.getDockerFile();
            return ext.getMainClassName().isPresent()
                || (dockerFile != null && !dockerFile.isBlank());
          });
        });

    // Wire only if Java plugin is applied
    project.getPlugins().withType(JavaPlugin.class, jp -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

      nativeImage.configure(t -> {
        t.getSources().from(main.getAllSource());
        t.getRuntimeClasspath().from(main.getRuntimeClasspath());
        t.dependsOn(project.getTasks().named(JavaPlugin.JAR_TASK_NAME));
        t.dependsOn(project.getTasks().named(JavaPlugin.TEST_TASK_NAME));
      });
    });

    // Safe: assemble depends on nativeImage, but nativeImage will be SKIPPED if not configured
    project.getTasks()
        .named(org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        .configure(t -> t.dependsOn(nativeImage));

    project.getPlugins().withId("distribution",
        __ -> project.getTasks().named("distZip").configure(t -> t.dependsOn(nativeImage)));
  }
}
