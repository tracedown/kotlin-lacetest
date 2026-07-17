# lacelang-kt-lacetest

Kotlin conformance test harness for [Lace](https://lacelang.dev). It turns
`.lace` scripts into test cases: a script's `expect()` outcomes become
pass/fail, so you can drive a service — typically one spun up with
Testcontainers — with real Lace probes and assert on the results.

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
fun conformance() =
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
