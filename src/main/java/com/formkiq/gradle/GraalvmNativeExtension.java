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

import java.util.List;
import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/** Graalvm Build Extension. */
public class GraalvmNativeExtension {

  /** Graalvm Default Version. */
  private static final String DEFAULT_IMAGE_VERSION = "24.0.1";

  /** Java Default Version. */
  private static final String DEFAULT_JAVA_VERSION = "java24";

  /** Dockerfile. */
  private Property<String> dockerFile;

  /** Build Options . */
  private Property<String> buildOptions;

  /** Additional Classpaths comma separated. */
  private Property<String> addClasspath;

  /** Enable using Graalvm Docker Image. */
  private Property<String> dockerImage;

  /** Enable Add All Charsets. */
  private Property<Boolean> enableAddAllCharsets;

  /** Allow image building with an incomplete class path. */
  private Property<Boolean> enableAllowIncompleteClasspath;

  /** Add all security service classes to the generated image. */
  private Property<Boolean> enableAllSecurityServices;

  /** Build stand-alone image if possible. */
  private Property<Boolean> enableAutoFallback;

  /** Check if native-toolchain is known to work with native-image. */
  private Property<Boolean> enableCheckToolchain;

  /** Force building of fallback image. */
  private Property<Boolean> enableForceFallback;

  /** Enable http support in the generated image. */
  private Property<Boolean> enableHttp;

  /** Enable https support in the generated image. */
  private Property<Boolean> enableHttps;

  /** Provide java.lang.Terminator exit handlers for executable images. */
  private Property<Boolean> enableInstallExitHandlers;

  /** Build stand-alone image or report failure. */
  private Property<Boolean> enableNoFallback;

  /** Print analysis call tree. */
  private Property<Boolean> enablePrintAnalysisCallTree;

  /** Enable the type flow saturation analysis performance optimization. */
  private Property<Boolean> enableRemoveSaturatedTypeFlows;

  /** Show exception stack traces for exceptions during image building. */
  private Property<Boolean> enableReportExceptionStackTraces;

  /** Report usage of unsupported methods and fields at run time. */
  private Property<Boolean> enableReportUnsupportedElementsAtRuntime;

  /** Build shared library. */
  private Property<Boolean> enableShared;

  /** Build statically linked executable. */
  private Property<Boolean> enableStatic;

  /** Enable verbose output. */
  private Property<Boolean> enableVerbose;

  /** a comma-separated list of fully qualified Feature implementation classes. */
  private Property<String> features;

  /** Local Image File to Use. */
  private Property<String> imageFile;

  /** Graalvm Version. */
  private Property<String> imageVersion;

  /** List of packages and classes that are initialized during image generation. */
  private ListProperty<String> initializeAtBuildTime;

  /** List of packages and classes that are initialized at runtime. */
  private ListProperty<String> initializeAtRunTime;

  /** Java Version. */
  private Property<String> javaVersion;

  /** JNI Config File. */
  private Property<String> jniConfigurationFiles;

  /** Class Name with main() method. */
  private Property<String> mainClassName;

  /** Output File Name. */
  private Property<String> outputFileName;

  /** Output Image Tag. */
  private Property<String> outputImageTag;

  /** Docker Platform. */
  private Property<String> platform;

  /** Reflection Config File. */
  private Property<String> reflectionConfig;

  /** Resource Configuration Files. */
  private Property<String> resourceConfigurationFiles;

  /** Reflection Config File. */
  private Property<String> serializationConfig;

  /** Java System Properties. */
  private ListProperty<String> systemProperty;

  /** Trace Class Initialization. */
  private Property<String> traceClassInitialization;

  /**
   * constructor.
   *
   * @param objects {@link ObjectFactory}
   */
  @Inject
  public GraalvmNativeExtension(final ObjectFactory objects) {
    this.javaVersion = objects.property(String.class);
    this.imageVersion = objects.property(String.class);
    this.platform = objects.property(String.class);
    this.mainClassName = objects.property(String.class);
    this.reflectionConfig = objects.property(String.class);
    this.serializationConfig = objects.property(String.class);
    this.jniConfigurationFiles = objects.property(String.class);
    this.resourceConfigurationFiles = objects.property(String.class);
    this.enableHttp = objects.property(Boolean.class);
    this.enableHttps = objects.property(Boolean.class);
    this.enableAddAllCharsets = objects.property(Boolean.class);
    this.enableVerbose = objects.property(Boolean.class);
    this.enableAutoFallback = objects.property(Boolean.class);
    this.enableForceFallback = objects.property(Boolean.class);
    this.initializeAtBuildTime = objects.listProperty(String.class);
    this.initializeAtBuildTime = objects.listProperty(String.class);
    this.initializeAtRunTime = objects.listProperty(String.class);
    this.enableInstallExitHandlers = objects.property(Boolean.class);
    this.enableShared = objects.property(Boolean.class);
    this.enableStatic = objects.property(Boolean.class);
    this.enableAllSecurityServices = objects.property(Boolean.class);
    this.traceClassInitialization = objects.property(String.class);
    this.enableRemoveSaturatedTypeFlows = objects.property(Boolean.class);
    this.systemProperty = objects.listProperty(String.class);
    this.enableReportExceptionStackTraces = objects.property(Boolean.class);
    this.enablePrintAnalysisCallTree = objects.property(Boolean.class);
    this.enableCheckToolchain = objects.property(Boolean.class);
    this.enableReportUnsupportedElementsAtRuntime = objects.property(Boolean.class);
    this.imageFile = objects.property(String.class);
    this.addClasspath = objects.property(String.class);
    this.features = objects.property(String.class);
    this.outputFileName = objects.property(String.class);
    this.outputImageTag = objects.property(String.class);
    this.dockerImage = objects.property(String.class);
    this.buildOptions = objects.property(String.class);
    this.dockerFile = objects.property(String.class);
    this.enableAllowIncompleteClasspath = objects.property(Boolean.class);
    this.enableNoFallback = objects.property(Boolean.class);
  }

  /**
   * Returns additional classpaths.
   *
   * @return {@link String}
   */
  public String getAddClasspath() {
    return this.addClasspath.getOrNull();
  }

  /**
   * Is Enable Docker Image Usage.
   *
   * @return {@link String}
   */
  public String getDockerImage() {
    return this.dockerImage.getOrNull();
  }

  /**
   * Get Build Options.
   *
   * @return {@link String}
   */
  public String getBuildOptions() {
    return this.buildOptions.getOrNull();
  }

  /**
   * Get Dockerfile.
   *
   * @return {@link String}
   */
  public String getDockerFile() {
    return this.dockerFile.getOrNull();
  }

  /**
   * Returns Features.
   *
   * @return {@link String}
   */
  public String getFeatures() {
    return this.features.getOrNull();
  }

  /**
   * Returns the Image File to use instead of download.
   *
   * @return The version of GraalVM Community Edition to use.
   */
  public String getImageFile() {
    return this.imageFile.getOrNull();
  }

  /**
   * Returns the version of GraalVM Community Edition to download.
   *
   * @return The version of GraalVM Community Edition to download.
   */
  public String getImageVersion() {
    return this.imageVersion.getOrElse(DEFAULT_IMAGE_VERSION);
  }

  /**
   * Get Initialize-At-Build-Time.
   *
   * @return {@link List} {@link String}
   */
  public List<String> getInitializeAtBuildTime() {
    return this.initializeAtBuildTime.getOrNull();
  }

  /**
   * Get Initialize-At-Run-Time.
   *
   * @return {@link List} {@link String}
   */
  public List<String> getInitializeAtRunTime() {
    return this.initializeAtRunTime.getOrNull();
  }

  /**
   * Returns the version of GraalVM Community Edition to download.
   *
   * @return The version of GraalVM Community Edition to download.
   */
  public String getJavaVersion() {
    return this.javaVersion.getOrElse(DEFAULT_JAVA_VERSION);
  }

  /**
   * Get JNI Configuration Files.
   *
   * @return {@link String}
   */
  public String getJniConfigurationFiles() {
    return this.jniConfigurationFiles.getOrNull();
  }

  /**
   * Get Main Class Name.
   *
   * @return {@link String}
   */
  public String getMainClassName() {
    return this.mainClassName.get();
  }

  /**
   * Returns Output File name.
   *
   * @return {@link String}
   */
  public String getOutputFileName() {
    return this.outputFileName.getOrNull();
  }

  /**
   * Returns Output Image Tag.
   *
   * @return {@link String}
   */
  public String getOutputImageTag() {
    return this.outputImageTag.getOrElse("generated-graalvm-native-plugin");
  }

  /**
   * Returns the version of GraalVM Community Edition to download.
   *
   * @return The version of GraalVM Community Edition to download.
   */
  public String getPlatform() {
    return this.platform.getOrNull();
  }

  /**
   * Get Reflection Config File.
   *
   * @return {@link String}
   */
  public String getReflectionConfig() {
    return this.reflectionConfig.getOrNull();
  }

  /**
   * Get Resource Configuration Files.
   *
   * @return {@link String}
   */
  public String getResourceConfigurationFiles() {
    return this.resourceConfigurationFiles.getOrNull();
  }

  /**
   * Get Serialization Config File.
   *
   * @return {@link String}
   */
  public String getSerializationConfig() {
    return this.serializationConfig.getOrNull();
  }

  /**
   * Get System Property.
   *
   * @return {@link List} {@link String}
   */
  public List<String> getSystemProperty() {
    return this.systemProperty.getOrNull();
  }

  /**
   * Get Trace Class Initialization.
   *
   * @return {@link String}
   */
  public String getTraceClassInitialization() {
    return this.traceClassInitialization.getOrNull();
  }

  /**
   * Is Enable Allow Incomplete Classpath.
   *
   * @return {@link Boolean}
   */
  public Boolean isAllowIncompleteClasspath() {
    return this.enableAllowIncompleteClasspath.getOrElse(Boolean.FALSE);
  }

  /**
   * Enable Add All Charsets.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableAddAllCharsets() {
    return this.enableAddAllCharsets.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Enable All Security Services.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableAllSecurityServices() {
    return this.enableAllSecurityServices.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Auto Fall Back.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableAutofallback() {
    return this.enableAutoFallback.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Check Tool chain.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableCheckToolchain() {
    return this.enableCheckToolchain.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Fallback.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableFallback() {
    return this.enableNoFallback.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Force Fallback.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableForceFallback() {
    return this.enableForceFallback.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Enable Http.
   *
   * @return Boolean
   */
  public Boolean isEnableHttp() {
    return this.enableHttp.getOrElse(Boolean.TRUE);
  }

  /**
   * Is Enable Https.
   *
   * @return Boolean
   */
  public Boolean isEnableHttps() {
    return this.enableHttps.getOrElse(Boolean.TRUE);
  }

  /**
   * Is Install-Exit-Handlers.
   *
   * @return Boolean
   */
  public Boolean isEnableInstallExitHandlers() {
    return this.enableInstallExitHandlers.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Print Analysis Call Tree.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnablePrintAnalysisCallTree() {
    return this.enablePrintAnalysisCallTree.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Remove Saturated Type Flows.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableRemoveSaturatedTypeFlows() {
    return this.enableRemoveSaturatedTypeFlows.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Report Exception Stack Traces.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableReportExceptionStackTraces() {
    return this.enableReportExceptionStackTraces.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Report Unsupported Elements At Runtime.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableReportUnsupportedElementsAtRuntime() {
    return this.enableReportUnsupportedElementsAtRuntime.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Enable Shared.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableShared() {
    return this.enableShared.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Enable Static.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableStatic() {
    return this.enableStatic.getOrElse(Boolean.FALSE);
  }

  /**
   * Is Verbose.
   *
   * @return {@link Boolean}
   */
  public Boolean isEnableVerbose() {
    return this.enableVerbose.getOrElse(Boolean.FALSE);
  }

  /**
   * Set Additional Classpath.
   *
   * @param cp {@link String}
   */
  public void setAddClasspath(final String cp) {
    this.addClasspath.set(cp);
  }

  /**
   * Set Enable Docker Image usage.
   *
   * @param imageName {@link String}
   */
  public void setDockerImage(final String imageName) {
    this.dockerImage.set(imageName);
  }

  /**
   * Set Build Options.
   *
   * @param buildOptions {@link String}
   */
  public void setBuildOptions(final String buildOptions) {
    this.buildOptions.set(buildOptions);
  }

  /**
   * Set Dockerfile.
   *
   * @param dockerfile {@link String}
   */
  public void setDockerFile(final String dockerfile) {
    this.dockerFile.set(dockerfile);
  }

  /**
   * Set Enable AddAllCharsets.
   *
   * @param enabled Boolean
   */
  public void setEnableAddAllCharsets(final Boolean enabled) {
    this.enableAddAllCharsets.set(enabled);
  }

  /**
   * Set Allow Incomplete Classpath.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableAllowIncompleteClasspath(final Boolean enabled) {
    this.enableAllowIncompleteClasspath.set(enabled);
  }

  /**
   * Set Enable All Security Services.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableAllSecurityServices(final Boolean enabled) {
    this.enableAllSecurityServices.set(enabled);
  }

  /**
   * Set Auto Fall Back.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableAutofallback(final Boolean enabled) {
    this.enableAutoFallback.set(enabled);
  }

  /**
   * Set Check Tool chain.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableCheckToolchain(final Boolean enabled) {
    this.enableCheckToolchain.set(enabled);
  }

  /**
   * Set Force Fallback.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableForceFallback(final Boolean enabled) {
    this.enableForceFallback.set(enabled);
  }

  /**
   * Set Enable Http.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableHttp(final Boolean enabled) {
    this.enableHttp.set(enabled);
  }

  /**
   * Set Enable Https.
   *
   * @param enabled Boolean
   */
  public void setEnableHttps(final Boolean enabled) {
    this.enableHttps.set(enabled);
  }

  /**
   * Set Install-Exit-Handlers
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableInstallExitHandlers(final Boolean enabled) {
    this.enableInstallExitHandlers.set(enabled);
  }

  /**
   * Set Enable No Fallback.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableNoFallback(final Boolean enabled) {
    this.enableNoFallback.set(enabled);
  }

  /**
   * Set Print Analysis Call Tree.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnablePrintAnalysisCallTree(final Boolean enabled) {
    this.enablePrintAnalysisCallTree.set(enabled);
  }

  /**
   * Set Remove Saturated Type Flows.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableRemoveSaturatedTypeFlows(final Boolean enabled) {
    this.enableRemoveSaturatedTypeFlows.set(enabled);
  }

  /**
   * Set Report Exception Stack Traces.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableReportExceptionStackTraces(Boolean enabled) {
    this.enableReportExceptionStackTraces.set(enabled);
  }

  /**
   * Set Report Unsupported Elements At Runtime.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableReportUnsupportedElementsAtRuntime(Boolean enabled) {
    this.enableReportUnsupportedElementsAtRuntime.set(enabled);
  }

  /**
   * Set Enable Shared.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableShared(final Boolean enabled) {
    this.enableShared.set(enabled);
  }

  /**
   * Set Enable Static.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableStatic(Boolean enabled) {
    this.enableStatic.set(enabled);
  }

  /**
   * Set Enable Verbose.
   *
   * @param enabled {@link Boolean}
   */
  public void setEnableVerbose(final Boolean enabled) {
    this.enableVerbose.set(enabled);
  }

  /**
   * Set Features.
   *
   * @param feature {@link String}
   */
  public void setFeatures(final String feature) {
    this.features.set(feature);
  }

  /**
   * Set Image File.
   *
   * @param filepath {@link String}
   */
  public void setImageFile(final String filepath) {
    this.imageFile.set(filepath);
  }

  /**
   * Set Image Version.
   *
   * @param version {@link String}
   */
  public void setImageVersion(final String version) {
    this.imageVersion.set(version);
  }

  /**
   * Set Initialize-At-Build-Time.
   *
   * @param list {@link List} {@link String}
   */
  public void setInitializeAtBuildTime(final List<String> list) {
    this.initializeAtBuildTime.set(list);
  }

  /**
   * Set Initialize-At-Run-Time.
   *
   * @param list {@link List} {@link String}
   */
  public void setInitializeAtRunTime(final List<String> list) {
    this.initializeAtRunTime.set(list);
  }

  /**
   * Set Java Version.
   *
   * @param version {@link String}
   */
  public void setJavaVersion(final String version) {
    this.javaVersion.set(version);
  }

  /**
   * Set JNI Config File.
   *
   * @param configFile {@link String}
   */
  public void setJniConfigurationFiles(final String configFile) {
    this.jniConfigurationFiles.set(configFile);
  }

  /**
   * Set Main Class Name.
   *
   * @param className {@link String}
   */
  public void setMainClassName(final String className) {
    this.mainClassName.set(className);
  }

  /**
   * Set Output File name.
   *
   * @param name {@link String}
   */
  public void setOutputFileName(final String name) {
    this.outputFileName.set(name);
  }

  /**
   * Set Output Image Tag.
   *
   * @param name {@link String}
   */
  public void setOutputImageTag(final String name) {
    this.outputImageTag.set(name);
  }

  /**
   * Set Platform.
   *
   * @param targetPlatform {@link String}
   */
  public void setPlatform(final String targetPlatform) {
    this.platform.set(targetPlatform);
  }

  /**
   * Set Reflection Config File.
   *
   * @param configFile {@link String}
   */
  public void setReflectionConfig(final String configFile) {
    this.reflectionConfig.set(configFile);
  }

  /**
   * Set Resource Config File.
   *
   * @param configFile {@link String}
   */
  public void setResourceConfigurationFiles(final String configFile) {
    this.resourceConfigurationFiles.set(configFile);
  }

  /**
   * Set Serialization Config File.
   *
   * @param configFile {@link String}
   */
  public void setSerializationConfig(final String configFile) {
    this.serializationConfig.set(configFile);
  }

  /**
   * Set System Property.
   *
   * @param list {@link List} {@link String}
   */
  public void setSystemProperty(final List<String> list) {
    this.systemProperty.set(list);
  }

  /**
   * Set Trace Class Initialization.
   *
   * @param classInitialization {@link String}
   */
  public void setTraceClassInitialization(final String classInitialization) {
    this.traceClassInitialization.set(classInitialization);
  }
}
