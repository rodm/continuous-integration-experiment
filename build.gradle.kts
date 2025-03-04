
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

val testJavaVersion = (findProperty("test.java.version") as String?) ?: "8"

val java17Home = javaToolchains
    .launcherFor { languageVersion.set(JavaLanguageVersion.of("17")) }.get()
    .metadata.installationPath.toString()

tasks {
    test {
        useJUnitPlatform()
        javaLauncher.set(project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(testJavaVersion))
        })
        systemProperty ("junit.jupiter.tempdir.cleanup.mode.default", "ON_SUCCESS")
        systemProperty ("java17.home", java17Home)
    }
}
