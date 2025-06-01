package com.formkiq.gradle.services;

import java.util.Locale;

/** Supported platforms, with URL suffix and file extension. */
public enum Platform {
  /** Linux X64. */
  LINUX_X64("linux-x64", "tar.gz"),
  /** Linux Aarch64. */
  LINUX_AARCH64("linux-aarch64", "tar.gz"),
  /** Macos X64. */
  MACOS_X64("macos-x64", "tar.gz"),
  /** MacOS Aarch64. */
  MACOS_AARCH64("macos-aarch64", "tar.gz"),
  /** Windows X64. */
  WINDOWS_X64("windows-x64", "zip");

  private final String suffix;
  private final String extension;

  Platform(String suffix, String extension) {
    this.suffix = suffix;
    this.extension = extension;
  }

  /**
   * Detects the current platform by examining System properties: - os.name → Linux / Mac / Windows
   * - os.arch → x86_64/amd64 / aarch64/arm64
   *
   * @return one of {LINUX_X64, LINUX_AARCH64, MACOS_X64, MACOS_AARCH64, WINDOWS_X64}
   * @throws IllegalArgumentException if it cannot match the OS or architecture
   */
  public static Platform detect() {
    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

    // Determine OS category
    boolean isWindows = osName.contains("windows");
    boolean isMac = osName.contains("mac"); // matches “Mac OS X”, “Mac OS”, etc.
    boolean isLinux = osName.contains("linux");

    // Determine architecture
    boolean isX64 = osArch.contains("x86_64") || osArch.contains("amd64") || osArch.equals("x64");
    boolean isAarch64 = osArch.contains("aarch64") || osArch.contains("arm64");

    if (isLinux) {
      if (isAarch64) {
        return LINUX_AARCH64;
      }
      if (isX64) {
        return LINUX_X64;
      }
    }

    if (isMac) {
      if (isAarch64) {
        return MACOS_AARCH64;
      }
      if (isX64) {
        return MACOS_X64;
      }
    }

    if (isWindows) {
      if (isX64) {
        return WINDOWS_X64;
      }
      // NOTE: Windows-on-ARM64 exists (e.g. Windows on certain ARM laptops), but our enum does not
      // include a WINDOWS_AARCH64 variant. If you need it, you could add a new enum constant.
    }

    // If we reach here, we couldn’t match exactly one of the five supported combos:
    throw new IllegalArgumentException(
        "Unsupported OS or architecture: os.name=\"" + osName + "\", os.arch=\"" + osArch + "\"");
  }

  /**
   * Get Suffix.
   *
   * @return String
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Get Extension.
   *
   * @return String
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Returns the Platform enum whose suffix matches the given string (ignoring case).
   *
   * @param suffix the suffix to look up (e.g. "linux-x64", "windows-x64", etc.)
   * @return the matching Platform constant
   * @throws IllegalArgumentException if no Platform has the given suffix
   */
  public static Platform fromSuffix(String suffix) {
    for (Platform p : values()) {
      if (p.suffix.equalsIgnoreCase(suffix)) {
        return p;
      }
    }
    throw new IllegalArgumentException("No Platform with suffix=\"" + suffix + "\"");
  }
}
