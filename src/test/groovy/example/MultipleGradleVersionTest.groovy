
package example

import org.gradle.api.JavaVersion
import org.gradle.internal.os.OperatingSystem
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.nio.file.Files
import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assumptions.assumeFalse
import static org.junit.jupiter.api.Assumptions.assumeTrue

class MultipleGradleVersionTest {

    @SuppressWarnings('unused')
    static List<String> gradleVersions() {
        return [
            '8.0.2', '8.1.1', '8.2.1', '8.3', '8.4', '8.5', '8.6', '8.7', '8.8', '8.9',
            '8.10.2', '8.11.1', '8.12.1', '8.13'
        ]
    }

    static List<String> releasedJavaVersions() {
        return ['1.8', '1.9', '1.10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23']
    }

    private static Map<String, String> GRADLE_JAVA_VERSIONS = [
        '6.3': '14',
        '6.7-rc-1': '15',
        '7.0-rc-1': '16',
        '7.3': '17',
        '7.5-rc-1': '18',
        '7.6-rc-1': '19',
        '8.3-rc-1': '20',
        '8.5': '21',
        '8.8-rc-1': '22',
        '8.10': '23'
    ].asUnmodifiable()

    static List<String> supportedByGradle(String version) {
        def gradleVersion = GradleVersion.version(version)
        def javaVersions = releasedJavaVersions()
        GRADLE_JAVA_VERSIONS.each { entry ->
            if (gradleVersion < GradleVersion.version(entry.key)) {
                javaVersions.remove(entry.value)
            }
        }
        return javaVersions
    }

    private BuildResult executeBuild(String version, String task) {
        if (OperatingSystem.current() == OperatingSystem.LINUX) {
            ConnectorServices.reset()
        }

        if (GradleVersion.version(version) >= GradleVersion.version('8.10')) {
            assumeFalse(OperatingSystem.current() == OperatingSystem.WINDOWS, "Skipping test with Gradle ${version} on Windows")
            if (JavaVersion.current() < JavaVersion.VERSION_17) {
                File gradleProperties = Files.createFile(projectDir.resolve('gradle.properties')).toFile()
                gradleProperties << """
                org.gradle.java.home=${System.getProperty("java17.home")}
                """
                def gradleDir = createDirectory('gradle').toPath()
                File gradleDaemonJvmProperties = Files.createFile(gradleDir.resolve('gradle-daemon-jvm.properties')).toFile()
                gradleDaemonJvmProperties << """
                toolchainVersion=17
                """
            }
        }

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments('--warning-mode', 'fail', task)
            .withPluginClasspath()
            .withGradleVersion(version)
            .forwardOutput()
            .build()
        return result
    }

    private File createFile(String name) {
        Files.createFile(projectDir.resolve(name)).toFile()
    }

    private File createDirectory(String name) {
        Files.createDirectories(projectDir.resolve(name)).toFile()
    }

    @TempDir
    public Path projectDir

    File buildFile
    File settingsFile

    @BeforeEach
    void init() throws IOException {
        buildFile = createFile("build.gradle.kts")
        settingsFile = createFile('settings.gradle.kts')
        settingsFile << """
        rootProject.name = "experiment"
        """
        buildFile << """              
        tasks.register("showInfo") {
            doLast {
                println("Show info")
                println("Java version: \${System.getProperty("java.version")}")
                println("Gradle version: \${GradleVersion.current()}")
            }
        }
        """
    }

    @DisplayName('show runtime info')
    @ParameterizedTest(name = 'with Gradle {0}')
    @MethodSource('example.MultipleGradleVersionTest#gradleVersions')
    void 'show info task'(String gradleVersion) {
        println "Java version: ${System.getProperty('java.version')}"
        println "Gradle version: ${GradleVersion.current()}"
        def releasedJavaVersions = releasedJavaVersions()
        def javaVersion = JavaVersion.current().toString()
        assertThat(releasedJavaVersions, hasItem(javaVersion))

        def supportedJavaVersions = supportedByGradle(gradleVersion)
        assumeTrue(supportedJavaVersions.contains(javaVersion), "Skipping test using Java version ${javaVersion}")

        BuildResult result = executeBuild(gradleVersion, 'showInfo')

        assertThat(result.getOutput(), containsString('Show info'))
        assertThat(result.task(":showInfo").getOutcome(), equalTo(SUCCESS))
    }

    @DisplayName('show Java toolchains')
    @ParameterizedTest(name = 'with Gradle {0}')
    @MethodSource('example.MultipleGradleVersionTest#gradleVersions')
    void 'java toolchains task'(String gradleVersion)  {
        BuildResult result = executeBuild(gradleVersion, 'javaToolchains')

        assertThat(result.task(':javaToolchains').getOutcome(), equalTo(SUCCESS))
    }
}
