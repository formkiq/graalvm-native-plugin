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
package com.formkiq.gradle.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * 
 * Utility class for Downloading files from a URL.
 *
 */
public class Downloader {

  /**
   * Download File.
   * 
   * @param url {@link String}
   * @param toFile {@link Path}
   * @return {@link Path}
   * @throws IOException IOException
   */
  public Path download(final String url, final Path toFile) throws IOException {

    if (!toFile.toFile().exists()) {

      Path parent = toFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      URL u = new URL(url);
      try (InputStream stream = createInputStream(u)) {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(stream)) {
          FileOutputStream fileOutputStream = new FileOutputStream(toFile.toFile());
          try (FileChannel fileChannel = fileOutputStream.getChannel()) {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
          }
        }
      }
    }

    return toFile;
  }

  private @Nonnull InputStream createInputStream(final URL u) throws IOException {
    return u.openStream();
  }
}
