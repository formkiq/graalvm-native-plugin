package com.formkiq.gradle.services;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** GraalVm Url Builder. */
public class GraalVmUrlBuilder {

  private final String javaVersion;
  private final String version;
  private final Platform platform;

  private GraalVmUrlBuilder(Builder builder) {
    this.javaVersion = builder.javaVersion;
    this.version = builder.version;
    this.platform = builder.platform;
  }

  /**
   * Builds the full download URL for the specified GraalVM Java version, GraalVM version, and
   * platform.
   *
   * @return the download URL as a String
   * @throws IllegalStateException if javaVersion, version, or platform is not set
   */
  public List<String> build() {
    if (version == null || version.isEmpty()) {
      throw new IllegalStateException("GraalVM version must be specified");
    }
    if (platform == null) {
      throw new IllegalStateException("Platform must be specified");
    }

    String s0 = getGraalvmCommunity();

    String s1 = getGraalvmCeBuild(platform.getSuffix());

    List<String> list = List.of(s0, s1);

    if (Platform.MACOS_X64.equals(platform)) {
      list = new ArrayList<>(list);
      list.add(getGraalvmBuildLegacy("darwin-amd64"));
    }

    if (Platform.LINUX_X64.equals(platform)) {
      list = new ArrayList<>(list);
      list.add(getGraalvmBuildLegacy("linux-amd64"));
    }

    if (Platform.WINDOWS_X64.equals(platform)) {
      list = new ArrayList<>(list);
      list.add(getGraalvmBuildLegacy("windows-amd64"));
    }

    if (Platform.MACOS_AARCH64.equals(platform)) {
      list = new ArrayList<>(list);
      list.add(getGraalvmBuildLegacy("darwin-aarch64"));
    }

    return list;
  }

  private @NotNull String getGraalvmBuildLegacy(final String suffix) {
    return String.format(
        "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-%s/graalvm-ce-%s-%s-%s.%s",
        version, javaVersion, suffix, version, platform.getExtension());
  }

  private @NotNull String getGraalvmCeBuild(final String suffix) {
    return String.format(
        "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-%s/graalvm-ce-%s-%s-%s.%s",
        version, javaVersion, suffix, version, platform.getExtension());
  }

  private @NotNull String getGraalvmCommunity() {
    return String.format(
        "https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-%s/graalvm-community-jdk-%s_%s_bin.%s",
        version, version, platform.getSuffix(), platform.getExtension());
  }

  /**
   * Entry point for the builder.
   *
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for GraalVMUrlBuilder. */
  public static class Builder {
    private String javaVersion;
    private String version;
    private Platform platform;

    /**
     * Specifies the Java version (e.g., "java17", "java11").
     *
     * @param javaVersion {@link String}
     * @return Builder
     */
    public Builder withJavaVersion(final String javaVersion) {
      this.javaVersion = javaVersion;
      return this;
    }

    /**
     * Specifies the GraalVM version (e.g., "24.0.1").
     *
     * @param version {@link String}
     * @return String
     */
    public Builder withVersion(final String version) {
      this.version = version;
      return this;
    }

    /**
     * Specifies the target platform from the Platform enum.
     *
     * @param platform {@link Platform}
     * @return Builder
     */
    public Builder withPlatform(final Platform platform) {
      this.platform = platform;
      return this;
    }

    /**
     * Builds the GraalVMUrlBuilder instance.
     *
     * @return List {@link String}
     */
    public List<String> build() {
      return new GraalVmUrlBuilder(this).build();
    }
  }
}
