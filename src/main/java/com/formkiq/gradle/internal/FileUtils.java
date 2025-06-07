package com.formkiq.gradle.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** File Utilities. */
public class FileUtils {
  /**
   * Deletes the directory at {@code dirPath} and all files/subdirectories underneath it.
   *
   * @param dirPath the path to the directory to delete
   * @throws IOException if an I/O error occurs during deletion
   */
  public static void deleteRecursively(Path dirPath) throws IOException {
    if (!Files.exists(dirPath)) {
      return; // Nothing to delete
    }

    // Walk the file tree and delete files/directories in post-order
    Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          // Propagate exception if something went wrong visiting entries
          throw exc;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
