package com.formkiq.gradle.services;

import static com.formkiq.gradle.internal.NativeImageExecutor.GRAALVM_JAVA_MAIN;

import com.formkiq.gradle.GraalvmNativeExtension;
import com.formkiq.gradle.GraalvmParameterToStrings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Utility to generate a Dockerfile with GraalVM native-image installation and invocation. */
public class DockerfileGenerator {
  private final String baseImage;
  private final List<String> nativeImageArgs;
  private final String mainClass;

  private DockerfileGenerator(Builder builder) {
    this.baseImage = builder.baseImage;
    this.nativeImageArgs = List.copyOf(builder.nativeImageArgs);
    this.mainClass = builder.mainClass;
  }

  /**
   * Generates the Dockerfile contents as a String.
   *
   * @return Dockerfile content
   */
  public String generateContents() {
    StringBuilder sb = new StringBuilder();
    sb.append("FROM ").append(baseImage).append("\n\n")
        .append("# Ensure GraalVM native-image component is installed\n").append("RUN sh -c \"")
        .append("if ! command -v native-image >/dev/null 2>&1; then gu install native-image; fi")
        .append("\"\n");

    sb.append("\nWORKDIR /workspace").append("\n");

    if (Path.of("build", GRAALVM_JAVA_MAIN).toFile().exists()) {
      sb.append("\nCOPY . .").append("\n");
    }

    if (mainClass != null || !nativeImageArgs.isEmpty()) {
      sb.append("\n# Build native-image with parameters\n");
      sb.append("RUN native-image");
      for (String arg : nativeImageArgs) {
        sb.append(" ").append(arg);
      }

      if (mainClass != null && !mainClass.isEmpty()) {
        sb.append(" ").append(mainClass);
      }

      sb.append("\n");
    }

    System.out.println("-----------------------");
    System.out.println("Generated contents: " + sb.toString());
    return sb.toString();
  }

  /**
   * Writes the generated Dockerfile to the specified path.
   *
   * @param outputPath where to write the Dockerfile
   * @throws IOException if an I/O error occurs
   */
  public void writeTo(Path outputPath) throws IOException {
    Files.writeString(outputPath, generateContents(), StandardCharsets.UTF_8);
  }

  /**
   * Creates a new Builder instance for DockerfileGenerator.
   *
   * @return a fresh Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for DockerfileGenerator. */
  public static final class Builder {
    private String baseImage;
    private final List<String> nativeImageArgs = new ArrayList<>();
    private String mainClass;

    private Builder() {}

    /**
     * Sets the base image for the Dockerfile.
     *
     * @param baseImage Docker base image (e.g. "oracle/graalvm-ce:22.3.0")
     * @return this Builder
     */
    public Builder baseImage(String baseImage) {
      this.baseImage = baseImage;
      return this;
    }

    /**
     * Adds a single argument for the native-image command.
     *
     * @param arg a native-image CLI parameter (e.g. "--no-fallback")
     * @return this Builder
     */
    public Builder addNativeImageArg(String arg) {
      this.nativeImageArgs.add(arg);
      return this;
    }

    /**
     * Adds multiple arguments for the native-image command.
     *
     * @param args native-image CLI parameters
     * @return this Builder
     */
    public Builder addNativeImageArgs(List<String> args) {
      this.nativeImageArgs.addAll(args);
      return this;
    }

    /**
     * Sets the main class for the native-image build.
     *
     * @param mainClass fully-qualified main class name (e.g. "com.example.Main")
     * @return this Builder
     */
    public Builder mainClass(String mainClass) {
      this.mainClass = mainClass;
      return this;
    }

    /**
     * Validates builder state and constructs a DockerfileGenerator.
     *
     * @return configured DockerfileGenerator
     */
    public DockerfileGenerator build() {
      if (baseImage == null || baseImage.isEmpty()) {
        throw new IllegalStateException("baseImage must be provided");
      }
      return new DockerfileGenerator(this);
    }

    /**
     * Add Native Image Args.
     *
     * @param extension {@link GraalvmNativeExtension}
     * @return Builder
     */
    public Builder addNativeImageArgs(final GraalvmNativeExtension extension) {
      List<String> params = new GraalvmParameterToStrings().apply(extension);
      this.nativeImageArgs.addAll(params);
      return this;
    }
  }
}
