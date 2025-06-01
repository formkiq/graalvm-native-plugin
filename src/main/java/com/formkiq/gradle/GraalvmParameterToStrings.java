package com.formkiq.gradle;

import static com.formkiq.gradle.internal.Strings.formatToUnix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** {@link Function} to transform {@link GraalvmNativeExtension} to {@link List} {@link String}. */
public class GraalvmParameterToStrings implements Function<GraalvmNativeExtension, List<String>> {
  @Override
  public List<String> apply(final GraalvmNativeExtension extension) {

    List<String> args = new ArrayList<>();

    addBooleanArgument(args, extension.isEnableFallback(), "--no-fallback");
    addBooleanArgument(args, extension.isAllowIncompleteClasspath(),
        "--allow-incomplete-classpath");
    addBooleanArgument(args, extension.isEnableInstallExitHandlers(), "--install-exit-handlers");
    addBooleanArgument(args, extension.isEnableHttp(), "--enable-http");
    addBooleanArgument(args, extension.isEnableHttps(), "--enable-https");
    addBooleanArgument(args, extension.isEnableVerbose(), "--verbose");
    addBooleanArgument(args, extension.isEnableAutofallback(), "--auto-fallback");
    addBooleanArgument(args, extension.isEnableForceFallback(), "--force-fallback");
    addBooleanArgument(args, extension.isEnableAllSecurityServices(),
        "--enable-all-security-services");
    addBooleanArgument(args, extension.isEnableShared(), "--shared");
    addBooleanArgument(args, extension.isEnableStatic(), "--static");

    addBooleanArgument(args, extension.isEnableAddAllCharsets(), "-H:+AddAllCharsets");
    addStringListArgument(args, extension.getInitializeAtBuildTime(), "--initialize-at-build-time");
    addStringListArgument(args, extension.getInitializeAtRunTime(), "--initialize-at-run-time");

    for (String property : extension.getSystemProperty()) {
      addStringArgument(args, property, "-D" + property);
    }

    String reflectConfig = extension.getReflectionConfig();
    if (reflectConfig != null) {
      addStringArgument(args, reflectConfig,
          "-H:ReflectionConfigurationFiles=" + formatToUnix(reflectConfig));
    }

    String serializationConfig = extension.getSerializationConfig();
    if (serializationConfig != null) {
      addStringArgument(args, serializationConfig,
          "-H:SerializationConfigurationResources=" + serializationConfig);
    }

    String jniConfig = extension.getJniConfigurationFiles();
    if (jniConfig != null) {
      addStringArgument(args, jniConfig, "-H:JNIConfigurationFiles=" + jniConfig);
    }

    String resourceConfig = extension.getResourceConfigurationFiles();
    if (resourceConfig != null) {
      addStringArgument(args, resourceConfig, "-H:ResourceConfigurationFiles=" + resourceConfig);
    }

    addStringArgument(args, extension.getFeatures(), "--features=" + extension.getFeatures());

    addBooleanArgument(args, extension.isEnableInstallExitHandlers(), "--install-exit-handlers");

    addStringArgument(args, extension.getTraceClassInitialization(),
        "--trace-class-initialization=" + extension.getTraceClassInitialization());
    addBooleanArgument(args, extension.isEnableRemoveSaturatedTypeFlows(),
        "-H:+RemoveSaturatedTypeFlows");
    addBooleanArgument(args, extension.isEnableReportExceptionStackTraces(),
        "-H:+ReportExceptionStackTraces");
    addBooleanArgument(args, extension.isEnablePrintAnalysisCallTree(),
        "-H:+PrintAnalysisCallTree");
    addBooleanArgument(args, extension.isEnableCheckToolchain(), "-H:-CheckToolchain");
    addBooleanArgument(args, extension.isEnableReportUnsupportedElementsAtRuntime(),
        "-H:+ReportUnsupportedElementsAtRuntime");

    return args;
  }

  private void addBooleanArgument(final List<String> args, final Boolean bool,
      final String argument) {
    if (Boolean.TRUE.equals(bool)) {
      args.add(argument);
    }
  }

  private void addStringArgument(List<String> args, String s, String argument) {
    if (s != null) {
      args.add(argument);
    }
  }

  private void addStringListArgument(final List<String> args, final List<String> list,
      final String argument) {
    if (!list.isEmpty()) {
      args.add(argument + "=" + String.join(",", list));
    }
  }
}
