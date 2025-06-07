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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/** File Archive Utilities. */
public class ArchiveUtils {

  /** Buffer Size. */
  private static final int BUFFER_SIZE = 1024;

  /** constructor. */
  public ArchiveUtils() {}

  /**
   * Decompress .tar.gz or .zip files.
   *
   * @param archive   {@link File}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void decompress(@Nonnull final File archive, @Nonnull final File outputDir)
      throws IOException {
    if (archive.toString().endsWith(".zip")) {
      decompressZip(archive, outputDir);
    } else {
      decompressTarGZip(archive, outputDir);
    }
  }

  /**
   * Decompress .zip files.
   *
   * @param archive   {@link File}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void decompressZip(@Nonnull final File archive, @Nonnull final File outputDir)
      throws IOException {
    try (ZipArchiveInputStream in =
        new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
      decompress(in, outputDir);
    }
  }

  /**
   * Decompress .tar.gz files.
   *
   * @param archive   {@link File}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void decompressTarGZip(@Nonnull final File archive, @Nonnull final File outputDir)
      throws IOException {
    try (TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(
        new BufferedInputStream(new FileInputStream(archive)), true))) {
      decompress(in, outputDir);
    }
  }

  /**
   * Decompress .jar files.
   *
   * @param archive {@link File}
   * @param outputDir {@link File}
   * @throws IOException IOException
   */
  public void decompressJar(@Nonnull final File archive, @Nonnull final File outputDir)
      throws IOException {
    try (JarArchiveInputStream in =
        new JarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
      decompress(in, outputDir);
    }
  }

  private void decompress(@Nonnull final ArchiveInputStream<?> in, @Nonnull final File outputDir)
      throws IOException {

    Path directory = Path.of(outputDir.getCanonicalPath());

    if (!outputDir.exists()) {
      Files.createDirectories(directory);
    }

    ArchiveEntry entry;

    while ((entry = in.getNextEntry()) != null) {

      TarArchiveEntry tarEntry = null;
      if (entry instanceof TarArchiveEntry) {
        tarEntry = (TarArchiveEntry) entry;
      }

      if (!entry.isDirectory()) {

        if (tarEntry != null && tarEntry.isSymbolicLink()) {

          try {

            try (FileSystem filesystem = createResource()) {
              Path fullpath =
                  filesystem.getPath(directory.toAbsolutePath().toString(), entry.getName());

              createParentDirectories(fullpath);

              try {
                Files.createSymbolicLink(fullpath, filesystem.getPath(tarEntry.getLinkName()));
              } catch (FileAlreadyExistsException e) {
                // ignore
              }
            }

          } catch (UnsupportedOperationException e) {
            // ignore
          }

        } else {

          Path fullpath = Path.of(directory.toAbsolutePath().toString(), entry.getName());
          File file = fullpath.toFile();

          if (!file.exists()) {

            createParentDirectories(fullpath);

            int count;
            byte[] data = new byte[BUFFER_SIZE];
            FileOutputStream fos = new FileOutputStream(file, false);
            try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
              while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
              }
            }

            if (tarEntry != null && tarEntry.getMode() == 493) {
              file.setExecutable(true);
            }
          }
        }
      }
    }

  }

  private @Nonnull FileSystem createResource() {
    return FileSystems.getDefault();
  }

  private void createParentDirectories(final Path path) throws IOException {

    Path parent = path.getParent();

    if (parent != null) {
      Files.createDirectories(parent);
    }
  }
}
