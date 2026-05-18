plugins {
    kotlin("jvm") version "2.0.21"
    `java-library`
}

group = "dev.lacelang"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    api("dev.lacelang:lacelang-kt-executor:0.1.0")
    api("dev.lacelang:lacelang-kt-validator:0.1.0")

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
