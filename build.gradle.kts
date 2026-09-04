plugins {
    kotlin("jvm") version "2.4.10"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
}

// group and version come from gradle.properties (single source of truth).

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Keep these at the latest published validator/executor. They are `api`
    // deps, so whatever is pinned here is what every consumer of the harness
    // runs its scripts on — and an executor older than the validator silently
    // downgrades what a script can assert. At 0.1.3 the executor had no
    // `count()` / `includes()` (spec S8.1) while the validator already parsed
    // them, so a script using either one ran, evaluated the condition to null,
    // and reported a pass. Bump both together.
    api("dev.lacelang:lacelang-kotlin-executor:0.1.6")
    api("dev.lacelang:kotlin-validator:0.1.5")

    api("org.junit.jupiter:junit-jupiter-api:5.11.4")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")

    compileOnly("org.testcontainers:testcontainers:1.20.4")
    compileOnly("org.testcontainers:junit-jupiter:1.20.4")

    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("dev.lacelang", "kotlin-lacetest", version.toString())

    pom {
        name.set("Lace Kotlin Testing Library")
        description.set(
            "Run Lace probe scripts as JUnit 5 test cases — use .lace scripts as the source for Kotlin integration and unit tests.",
        )
        inceptionYear.set("2026")
        url.set("https://lacelang.dev")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("lacelang")
                name.set("Lace")
                url.set("https://lacelang.dev")
            }
        }
        scm {
            url.set("https://github.com/tracedown/kotlin-lacetest")
            connection.set("scm:git:https://github.com/tracedown/kotlin-lacetest.git")
            developerConnection.set("scm:git:ssh://git@github.com/tracedown/kotlin-lacetest.git")
        }
    }
}
