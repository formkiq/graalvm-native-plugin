# Graalvm Native Gradle plugin

Supports for building Java applications as GraalVM native images.

[https://plugins.gradle.org/plugin/com.formkiq.gradle.graalvm-native-plugin](https://plugins.gradle.org/plugin/com.formkiq.gradle.graalvm-native-plugin)

## Quick start

### Apply Gradle plugin

#### Groovy
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```groovy
plugins {
    id 'com.formkiq.gradle.graalvm-native-plugin' version '1.0.2'
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.formkiq.gradle:graalvm-native-plugin:1.0.2"
  }
}

apply plugin: "com.formkiq.gradle.graalvm-native-plugin"
```

#### Kotlin
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```groovy
plugins {
    id('com.formkiq.gradle.graalvm-native-plugin') version '1.0.2'
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```groovy
buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("com.formkiq.gradle:graalvm-native-plugin:1.0.2")
  }
}

apply(plugin = "com.formkiq.gradle.graalvm-native-plugin")
```

### Specify build arguments
This plugin uses the following example Gradle extension for configuration:
```groovy
nativeImage {
    mainClassName = 'com.example.Application'

    enableHttp          = true
    enableHttps         = true
}
```

More configuration options can be found [here](https://github.com/formkiq/graalvm-native-plugin#configuration).

### Build GraalVM Native Image
1. Run the Gradle task `graalvmNativeImage`
2. The native image can be located at `<buildDir>/graalvm`

## Sample project
[samples](https://github.com/formkiq/graalvm-native-plugin/tree/master/samples) contains various samples that demonstrate the basic usage of this Gradle plugin.

## Configuration
| Property | Type | Description |
|----------|------|-------------|
| `imageVersion` | `String` | The GraalVM Community Edition version to download. Default to `20.2.0`. |
| `javaVersion` | `String` | The JDK version to be downloaded with GraalVM Community Edition. Default to `11`. |
| `imageFile` | `String` | A local Image File to instead of downloading a file based on imageVersion/javaVersion/architecture. |
| `mainClassName` (Required) | `String` | The fully qualified name of the Java class that contains a `main` method for the entry point of the Native Image executable. |
| `enableTraceClassInitialization` | `boolean` | Provides useful information to debug class initialization issues. |
| `enableRemoveSaturatedTypeFlows` | `boolean` | Reduces build time and decrease build memory consumption, especially for big projects. |
| `enableReportExceptionStackTraces` | `boolean` | Provides more detail should something go wrong. |
| `enablePrintAnalysisCallTree` | `boolean` | Helps to find what classes, methods, and fields are used and why. You can find more details in GraalVM [reports documentation](https://github.com/oracle/graal/blob/master/substratevm/REPORTS.md). |
| `enableAllSecurityServices` | `boolean` | Adds all security service classes to the generated image. Required for HTTPS and crypto. |
| `enableHttp` | `boolean` | Enables HTTP support in the generated image. |
| `enableHttps` | `boolean` | Enables HTTPS support in the generated image. |
| `enableVerbose` | `boolean` | Makes image building output more verbose. |
| `enableDocker` | `String` | Enable using Graalvm Docker Image. |
| `enableAllowIncompleteClasspath` | `boolean` | Allow image building with an incomplete class path. |
| `enableNoFallback` | `boolean` | Build stand-alone image or report failure. |
| `enableAutoFallback` | `boolean` | Build stand-alone image if possible. |
| `enableForceFallback` | `boolean` | Force building of fallback image. |
| `enableForceFallback` | `boolean` | Force building of fallback image. |
| `enableInstallExitHandlers` | `boolean` | Provide java.lang.Terminator exit handlers for executable images. |
| `enableShared` | `boolean` | Build shared library. |
| `enableStatic` | `boolean` | Build statically linked executable. |
| `enableCheckToolchain` | `boolean` | Check if native-toolchain is known to work with native-image. |
| `enableReportUnsupportedElementsAtRuntime` | `boolean` | Report usage of unsupported methods and fields at run time. |
| `initializeAtBuildTime` | `List<String>` | Use it with specific classes or package to initialize classes at build time. |
| `initializeAtRunTime` | `List<String>` | Use it with specific classes or package to initialize classes at run time. |
| `reflectionConfig` |  `String` | [GraalVM Reflection Configuration File](https://www.graalvm.org/reference-manual/native-image/Reflection) to enable Java reflection support. |
| `systemProperty` | `List<String>` | Java System Properties to use when building Graalvm Image. |
| `addClasspath` | `String` | Additional Classpaths comma separated. |
| `features` | `String` | a comma-separated list of fully qualified Feature implementation classes. |
| `outputFileName` | `String` | Output File Name. |

## License
[Apache 2](https://github.com/formkiq/graalvm-native-plugin/blob/master/LICENSE)

## References
* [GraalVM](https://www.graalvm.org)
* [GraalVM Native Image](https://www.graalvm.org/docs/reference-manual/native-image)

