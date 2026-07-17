import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.0.21"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

// group and version come from gradle.properties (single source of truth).

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api("dev.lacelang:lacelang-kotlin-executor:0.1.3")
    api("dev.lacelang:kotlin-validator:0.1.3")

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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates("dev.lacelang", "kotlin-lacetest", version.toString())

    pom {
        name.set("Lace Kotlin Test Harness")
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
