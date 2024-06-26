import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.nosphere.gradle.github.actions") version "1.3.2"
}
repositories{
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    google()
}

group = "net.ltgt.gradle"
version = "0.0.1"


// Make sure Gradle Module Metadata targets the appropriate JVM version
tasks.withType<JavaCompile>().configureEach {
    options.release.set(kotlinDslPluginOptions.jvmTarget.map { JavaVersion.toVersion(it).majorVersion.toInt() })
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        allWarningsAsErrors = true
    }
}

gradle.taskGraph.whenReady {
    if (hasTask(":publishPlugins")) {
        check("git diff --quiet --exit-code".execute(null, rootDir).waitFor() == 0) { "Working tree is dirty" }
        val process = "git describe --exact-match".execute(null, rootDir)
        check(process.waitFor() == 0) { "Version is not tagged" }
        version = process.text.trim().removePrefix("v")
    }
}

// See https://github.com/gradle/gradle/issues/7974
val additionalPluginClasspath by configurations.creating

val errorpronePluginVersion = "2.0.2"
val errorproneVersion = "2.10.0"

repositories {
    mavenCentral()
    google ()
    gradlePluginPortal()
}
dependencies {
    compileOnly("net.ltgt.gradle:gradle-errorprone-plugin:$errorpronePluginVersion")
    testImplementation("net.ltgt.gradle:gradle-errorprone-plugin:$errorpronePluginVersion")

    testImplementation("com.google.truth.extensions:truth-java8-extension:1.1.3") {
        // See https://github.com/google/truth/issues/333
        exclude(group = "junit", module = "junit")
    }
    testRuntimeOnly("junit:junit:4.13.2") {
        // See https://github.com/google/truth/issues/333
        because("Truth needs it")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    testImplementation("com.google.errorprone:error_prone_check_api:$errorproneVersion") {
        exclude(group = "com.google.errorprone", module = "javac")
    }

    additionalPluginClasspath("net.ltgt.gradle:gradle-errorprone-plugin:$errorpronePluginVersion")

    //implementation for the Annotator Core
    implementation("edu.ucr.cs.riple.annotator:annotator-core:1.3.13-SNAPSHOT")
}

tasks {
    pluginUnderTestMetadata {
        this.pluginClasspath.from(additionalPluginClasspath)
    }
    test {
        val testJavaToolchain = project.findProperty("test.java-toolchain")
        testJavaToolchain?.also {
            javaLauncher.set(
                project.javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(testJavaToolchain.toString()))
                }
            )
        }

        val testGradleVersion = project.findProperty("test.gradle-version")
        testGradleVersion?.also { systemProperty("test.gradle-version", testGradleVersion) }

        systemProperty("errorprone.version", errorproneVersion)

        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

gradlePlugin {
    plugins {
        register("annotator") {
            id = "edu.ucr.cs.riple.annotator.gradle-plugin"
            displayName = "Adds AnnotatorScanner DSL to Gradle Error Prone plugin"
            description = "Adds AnnotatorScanner DSL to Gradle Error Prone plugin"
            implementationClass = "edu.ucr.cs.riple.annotator.gradle.plugin.AnnotatorPlugin"
        }
    }
}

//pluginBundle {
//    website = "https://github.com/tbroyer/gradle-nullaway-plugin"
//    vcsUrl = "https://github.com/tbroyer/gradle-nullaway-plugin"
//    tags = listOf("javac", "error-prone", "nullaway", "nullability")
//}

ktlint {
    version.set("0.45.2")
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
}

fun String.execute(envp: Array<String>?, workingDir: File?) =
    Runtime.getRuntime().exec(this, envp, workingDir)

val Process.text: String
    get() = inputStream.bufferedReader().readText()
