
plugins {
    id ("org.gradle.groovy")
    id ("org.gradle.java-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation ("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation ("org.hamcrest:hamcrest:3.0")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    test {
        useJUnitPlatform()
        systemProperty ("junit.jupiter.tempdir.cleanup.mode.default", "ON_SUCCESS")
    }
}
