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

    project.afterEvaluate(p -> {
      // Treat "configured" as: user set mainClassName (adjust the predicate if you prefer)
      boolean configured = ext.getMainClassName().isPresent();

      if (!configured) {
        // User didn't declare nativeImage { ... } in this subproject — do nothing.
        return;
      }

      // Register the task now that we know it's wanted in this project
      TaskProvider<GraalvmNativeTask> nativeImage =
          project.getTasks().register("graalvmNativeImage", GraalvmNativeTask.class, task -> {
            task.setGroup("Graalvm");
            task.setDescription("Build GraalVM Native Image");
            task.setExtension(ext); // inject the extension (nested inputs)
            task.usesService(svc);
            task.getBuildDirectory().set(project.getLayout().getBuildDirectory().dir("graalvm"));
          });

      // Wire only if the Java plugin is applied
      project.getPlugins().withType(JavaPlugin.class, jp -> {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        // Inputs
        nativeImage.configure(t -> {
          t.getSources().from(main.getAllSource()); // java + resources
          t.getRuntimeClasspath().from(main.getRuntimeClasspath()); // runtime jars/classes
        });

        // Ensure producers run first (jar is enough; avoids assemble cycles)
        nativeImage.configure(t -> t.dependsOn(project.getTasks().named(JavaPlugin.JAR_TASK_NAME)));
      });

      // If you want assemble to run AFTER native image in projects that opted in
      project.getTasks()
          .named(org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
          .configure(t -> t.dependsOn(nativeImage));

      // If the Distribution plugin is applied, make distZip wait for native image
      project.getPlugins().withId("distribution",
          __ -> project.getTasks().named("distZip").configure(t -> t.dependsOn(nativeImage)));
    });

    // TaskProvider<GraalvmNativeTask> nativeImage =
    // project.getTasks().register("graalvmNativeImage", GraalvmNativeTask.class, task -> {
    // task.setGroup("Graalvm");
    // task.setDescription("Build GraalVM Native Image");
    // task.setExtension(ext);
    // task.usesService(svc);
    // task.onlyIf(t -> ext.getMainClassName().isPresent());
    //
    // task.getBuildDirectory().set(project.getLayout().getBuildDirectory().dir("graalvm"));
    // });
    //
    // project.getPlugins().withType(JavaPlugin.class, jp -> {
    // SourceSetContainer sourceSets =
    // project.getExtensions().getByType(SourceSetContainer.class);
    // SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    //
    // // Inputs
    // nativeImage.configure(t -> {
    // t.getSources().from(main.getAllSource()); // sources (java + resources)
    // t.getRuntimeClasspath().from(main.getRuntimeClasspath()); // runtime jars/classes
    // });
    //
    // nativeImage.configure(t ->
    // t.dependsOn(project.getTasks().named(JavaPlugin.JAR_TASK_NAME)));
    // });
    //
    // // Make assemble run AFTER native image (i.e., assemble depends on native)
    // project.getTasks()
    // .named(org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
    // .configure(t -> t.dependsOn(nativeImage));
    //
    // // If using the Distribution plugin, make distZip run AFTER native image
    // project.getPlugins().withId("distribution", p -> {
    // project.getTasks().named("distZip").configure(t -> t.dependsOn(nativeImage));
    // });
  }
}
