import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "asia.lira"
version = "0.0.1"
val buildDir = layout.buildDirectory.get().asFile

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
    maven("https://repo.marcloud.net/releases/")
    maven("https://maven.google.com/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

val compileClasspathOnly: Configuration by configurations.creating

val fastutilLib = "it.unimi.dsi:fastutil:8.5.15"
val lombokLib = "org.projectlombok:lombok:1.18.30"

val libraries = listOf(
    "com.github.opai-client:opensource-components:-SNAPSHOT",
    "org.lwjgl:lwjgl:2.9.4-nightly",
    fastutilLib,
    lombokLib,
    "org.jetbrains:annotations:24.0.0",
    "com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta4"
)

dependencies {
    for (it in libraries) {
        implementation(it)
        shadow(it)
    }

    compileOnly(lombokLib)
    annotationProcessor(lombokLib)
    compileClasspathOnly("com.android.tools:r8:8.5.35")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val r8Jar = compileClasspathOnly.filter { it.name.contains("r8") }.singleFile
val classpathSeparator = if (OperatingSystem.current().isWindows) ";" else ":"
val dependenciesJars: MutableSet<File> = configurations.runtimeClasspath.get().files
val javaHome: String = System.getProperty("java.home")
val javaRuntime: String = when {
    javaHome.contains("jdk") || javaHome.contains("openjdk") -> {
        // JDK 9+
        "$javaHome/jmods/java.base.jmod"
    }
    else -> {
        // JDK 8
        "$javaHome/lib/rt.jar"
    }
}

val r8 = tasks.register("r8") {
    group = "optimization"
    description = "Run R8 to minimize the shadowJar output"

    doLast {
        val inputJar = File(buildDir, "libs/%s-%s-unoptimized.jar".format(project.name, version))
        val outputJar = File(buildDir, "libs/%s-%s-universal.jar".format(project.name, version))
        val configFile = File(rootDir, "r8-rules.pro")

        if (!r8Jar.exists()) {
            throw GradleException("R8 JAR not found at ${r8Jar.absolutePath}, please download it manually.")
        }

        exec {
            val command = mutableListOf(
                "java", "-cp", r8Jar.absolutePath,
                "com.android.tools.r8.R8",
                "--classfile",
                "--release",
                "--pg-conf", configFile.absolutePath,
                "--lib", javaHome,
                "--output", outputJar.absolutePath,
                inputJar.absolutePath
            )

            dependenciesJars.forEach { jar ->
                command.add("--classpath")
                command.add(jar.absolutePath)
            }

            commandLine(command)
        }
    }
}


tasks {
    jar {
        enabled = false
    }

    withType<ShadowJar> {
        manifest {
            attributes(
                "Main-Class" to "asia.lira.opaiplus.Main"
            )
        }

        archiveClassifier.set("unoptimized")

        @Suppress("SpellCheckingInspection")
        dependencies {
            include(dependency(fastutilLib))
            exclude("shadowBanner.txt")
            exclude("release-timestamp.txt")
            exclude("README.md")
            exclude("latestchanges.html")
            exclude("changelog.txt")
            exclude("AUTHORS")
            exclude("META-INF/LICENSE.txt")
        }

        from("LICENSE")
        from("THIRD_PARTY_LICENSES")
        from("licenses") {
            into("licenses")
        }

        minimize()
        mergeServiceFiles()
        configurations = listOf(project.configurations.runtimeClasspath.get())

        finalizedBy(r8)
    }

    assemble {
        dependsOn(shadowJar)
    }
}
