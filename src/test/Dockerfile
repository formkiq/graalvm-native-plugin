FROM ghcr.io/graalvm/jdk-community:24.0.1

RUN ls -Ral /

RUN /opt/graalvm-community-java24/bin/native-image --help

# Ensure GraalVM native-image component is installed
RUN if ! command -v ${JAVA_HOME}/bin/native-image >/dev/null 2>&1; then \
      ${JAVA_HOME}/bin/gu install native-image; \
    fi
