# lacelang-kt-lacetest

[![Maven Central](https://img.shields.io/maven-central/v/dev.lacelang/kotlin-lacetest)](https://central.sonatype.com/artifact/dev.lacelang/kotlin-lacetest)

A Kotlin testing library that runs [Lace](https://lacelang.dev) probe scripts as
JUnit 5 test cases. Point it at a directory of `.lace` scripts and a target
service — often one spun up with Testcontainers — and each script becomes a
test: an `expect()` failure fails the test, a `check()` failure is logged as a
warning. Use it to write integration and unit tests with Lace scripts as the
test source.

Published to Maven Central as `dev.lacelang:kotlin-lacetest`.

```kotlin
testImplementation("dev.lacelang:kotlin-lacetest:0.1.0")
```

## Usage

Two builder-based entry points; both discover `.lace` scripts under a directory.

**As JUnit 5 dynamic tests** — one test per script. An `expect()` failure fails
the test; a `check()` failure is logged as a warning and the test still passes.

```kotlin
@TestFactory
fun apiTests() =
    LaceTestSuite.builder()
        .scriptsDir("src/test/resources/lace")
        .baseUrl(container.baseUrl)   // e.g. a Testcontainers target
        .build()
        .dynamicTests()
```

**As a standalone report** — for CI output outside JUnit:

```kotlin
val report = LaceTestReport.builder()
    .scriptsDir("scripts")
    .baseUrl("http://localhost:8080")
    .build()
    .run()
report.print()
```

Common builder options (both entry points): `scriptsDir`, `baseUrl`,
`var(k, v)` / `vars(map)`, `config(map)`, `extension(name)` /
`extensionPath(path)`, `prevResults(map)`, `recursive(Boolean)`, `quiet()`.

## Requirements

JDK 17+. Pulls in the Lace Kotlin [validator](https://github.com/tracedown/lacelang-kotlin-validator)
and [executor](https://github.com/tracedown/lacelang-kotlin-executor); JUnit 5
is on the API classpath and Testcontainers is optional (`compileOnly`).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
