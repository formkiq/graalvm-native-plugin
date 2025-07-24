package com.formkiq.gradle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GraalvmParameterToStrings}. */
class GraalvmParameterToStringsTest {

  @Test
  void testDefaultParameters() {
    // Arrange: create a GraalvmNativeExtension with no properties explicitly set
    Project project = ProjectBuilder.builder().build();
    ObjectFactory objects = project.getObjects();
    GraalvmNativeExtension extension = new GraalvmNativeExtension(objects);

    // Act: generate the argument list
    List<String> args = new GraalvmParameterToStrings().apply(extension);

    // Assert: since enableHttp and enableHttps default to true, expect exactly those two flags
    List<String> expected = List.of("--enable-http", "--enable-https");
    assertEquals(expected, args,
        "Default extension should only produce --enable-http and --enable-https");
  }

  @Test
  void testAllParametersSet() {
    // Arrange: create the extension and set every supported property
    Project project = ProjectBuilder.builder().build();
    ObjectFactory objects = project.getObjects();
    GraalvmNativeExtension extension = new GraalvmNativeExtension(objects);

    // 1. Build options (string)
    extension.setBuildOptions("myBuildOptions");

    // 2. Boolean flags
    extension.setEnableNoFallback(true); // isEnableFallback → true → "--no-fallback"
    extension.setEnableAllowIncompleteClasspath(true); // "--allow-incomplete-classpath"
    extension.setEnableInstallExitHandlers(true); // "--install-exit-handlers"
    extension.setEnableHttp(false); // disable the default "--enable-http"
    extension.setEnableHttps(false); // disable the default "--enable-https"
    extension.setEnableVerbose(true); // "--verbose"
    extension.setEnableAutofallback(true); // "--auto-fallback"
    extension.setEnableForceFallback(true); // "--force-fallback"
    extension.setEnableAllSecurityServices(true); // "--enable-all-security-services"
    extension.setEnableShared(true); // "--shared"
    extension.setEnableStatic(true); // "--static"
    extension.setEnableAddAllCharsets(true); // "-H:+AddAllCharsets"

    // 3. List-based arguments
    extension.setInitializeAtBuildTime(Arrays.asList("a", "b")); // "--initialize-at-build-time=a,b"
    extension.setInitializeAtRunTime(List.of("c")); // "--initialize-at-run-time=c"
    extension.setSystemProperty(Arrays.asList("prop1", "prop2")); // "-Dprop1", "-Dprop2"

    // 4. String-based configuration files
    extension.setReflectionConfig("ref.json"); // "-H:ReflectionConfigurationFiles=ref.json"
    extension.setSerializationConfig("ser.json"); // "-H:SerializationConfigurationResources=ser.json"
    extension.setJniConfigurationFiles("jni.conf"); // "-H:JNIConfigurationFiles=jni.conf"
    extension.setResourceConfigurationFiles("res.conf"); // "-H:ResourceConfigurationFiles=res.conf"
    extension.setFeatures("featX"); // "--features=featX"

    // 5. Trace class initialization
    extension.setTraceClassInitialization("traceClass"); // "--trace-class-initialization=traceClass"

    // 6. More boolean flags at the end
    extension.setEnableRemoveSaturatedTypeFlows(true); // "-H:+RemoveSaturatedTypeFlows"
    extension.setEnableReportExceptionStackTraces(true); // "-H:+ReportExceptionStackTraces"
    extension.setEnablePrintAnalysisCallTree(true); // "-H:+PrintAnalysisCallTree"
    extension.setEnableCheckToolchain(true); // "-H:-CheckToolchain"
    extension.setEnableReportUnsupportedElementsAtRuntime(true); // "-H:+ReportUnsupportedElementsAtRuntime"

    // Act: generate the full argument list
    List<String> args = new GraalvmParameterToStrings().apply(extension);

    // Assert: verify that the arguments appear in exactly the same order they are added in
    // apply(...)
    List<String> expected = List.of(
        // 1. buildOptions
        "-myBuildOptions",
        // 2. boolean flags in declaration order
        "--no-fallback", "--allow-incomplete-classpath", "--install-exit-handlers",
        // disabled HTTP/HTTPS → do not appear
        // next flags that were set true:
        "--verbose", "--auto-fallback", "--force-fallback", "--enable-all-security-services",
        "--shared", "--static", "-H:+AddAllCharsets",
        // 3. list-based arguments
        "--initialize-at-build-time=a,b", "--initialize-at-run-time=c", "-Dprop1", "-Dprop2",
        // 4. string-based configuration files
        "-H:ReflectionConfigurationFiles=ref.json",
        "-H:SerializationConfigurationResources=ser.json", "-H:JNIConfigurationFiles=jni.conf",
        "-H:ResourceConfigurationFiles=res.conf", "--features=featX",
        // 5. trace-class-initialization
        "--trace-class-initialization=traceClass",
        // 6. final boolean flags
        "-H:+RemoveSaturatedTypeFlows", "-H:+ReportExceptionStackTraces",
        "-H:+PrintAnalysisCallTree", "-H:-CheckToolchain",
        "-H:+ReportUnsupportedElementsAtRuntime");
    assertEquals(expected, args,
        "When all properties are set, the resulting argument list must match exactly.");
  }

  @Test
  void testMinusHparameters() {
    // given
    Project project = ProjectBuilder.builder().build();
    ObjectFactory objects = project.getObjects();
    GraalvmNativeExtension extension = new GraalvmNativeExtension(objects);
    extension.setBuildOptions("-Os -H:-ReduceImplicitExceptionStackTraceInformation");

    // when
    List<String> args = new GraalvmParameterToStrings().apply(extension);

    // then
    List<String> expected = List.of("-Os", "-H:-ReduceImplicitExceptionStackTraceInformation",
        "--enable-http", "--enable-https");
    assertEquals(expected, args);
  }
}
