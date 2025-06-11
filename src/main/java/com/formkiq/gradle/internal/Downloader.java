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
package com.formkiq.gradle.internal;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Utility class for Downloading files from a URL. */
public class Downloader {

  private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());

  /**
   * Download File.
   *
   * @param urls {@link Collection} {@link String}
   * @param toFile {@link Path}
   * @throws IOException IOException
   */
  public void download(final Collection<String> urls, final Path toFile) throws IOException {

    boolean found = false;
    if (!toFile.toFile().exists()) {

      for (final String url : urls) {

        if (urlExists(url)) {
          found = true;
          LOGGER.log(Level.INFO, "Downloading " + url + " to " + toFile);
          Path parent = toFile.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }

          URL u = new URL(url);
          try (InputStream stream = u.openStream()) {
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(stream)) {
              try (FileOutputStream fileOutputStream = new FileOutputStream(toFile.toFile())) {
                try (FileChannel fileChannel = fileOutputStream.getChannel()) {
                  fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }
              }
            }
          }

          break;
        }
      }

    } else {
      LOGGER.log(Level.INFO, "Downloaded file {0} already exists", toFile);
    }

    if (!found) {
      throw new FileNotFoundException("Failed to download file from urls " + urls);
    }
  }

  /**
   * Performs an HTTP HEAD request to determine if the given URL exists. Returns true if the
   * response code is in the 2xx or 3xx range; false if it is 4xx/5xx or if any exception occurs
   * (e.g., malformed URL, connection timeout).
   *
   * @param urlString the HTTP or HTTPS URL string to test
   * @return true if the URL exists (i.e. server returns status &lt; 400), false otherwise
   */
  private static boolean urlExists(String urlString) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(urlString);
      connection = (HttpURLConnection) url.openConnection();

      // We only need the headers, not the body
      connection.setRequestMethod("HEAD");
      // Set timeouts (in milliseconds) so it won't hang forever
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.connect();

      int responseCode = connection.getResponseCode();
      // Treat any HTTP status < 400 as “exists”
      return (responseCode < HttpURLConnection.HTTP_BAD_REQUEST);
    } catch (IOException e) {
      // IOException can mean the URL is malformed, host unreachable, etc.
      return false;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
