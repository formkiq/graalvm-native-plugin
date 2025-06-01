FROM ghcr.io/graalvm/graalvm-ce:ol9-java17-22.3.1

# Ensure GraalVM native-image component is installed
RUN sh -c "if ! command -v native-image >/dev/null 2>&1; then gu install native-image; fi"

WORKDIR /workspace

COPY build/graalvm/java/main/ .

# Build native-image with parameters
RUN native-image --enable-http --enable-https --static -H:ReflectionConfigurationFiles=META-INF/graal/reflect.json com.formkiq.lambda.runtime.graalvm.LambdaRuntime
