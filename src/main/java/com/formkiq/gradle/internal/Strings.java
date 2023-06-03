package com.formkiq.gradle.internal;

import java.io.File;
import org.gradle.internal.os.OperatingSystem;

/**
 * 
 * {@link String} utilities.
 *
 */
public class Strings {

  /**
   * Format {@link File} in Unix format.
   * 
   * @param file {@link File}
   * @return {@link String}
   */
  public static String formatToUnix(final File file) {
    String path = file.getAbsolutePath();

    return formatToUnix(path);
  }

  /**
   * Format {@link String} in Unix format.
   * 
   * @param path {@link String}
   * @return {@link String}
   */
  public static String formatToUnix(final String path) {
    String s = path;
    if (OperatingSystem.current().isWindows()) {
      s = s.replace("\\", "/").replace(":", "");
      if (!s.startsWith("/")) {
        s = "/" + s;
      }
    }

    return s;
  }
}
