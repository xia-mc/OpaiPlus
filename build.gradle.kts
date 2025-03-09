import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import java.util.ArrayList

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "asia.lira"
val buildDir = layout.buildDirectory.get().asFile

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
    maven("https://repo.marcloud.net/releases/")
    maven("https://maven.google.com/")
    maven("https://repo.carm.cc/repository/maven-public")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

val compileClasspathOnly: Configuration by configurations.creating

val fastutilLib = "it.unimi.dsi:fastutil:8.5.15"
val lombokLib = "org.projectlombok:lombok:1.18.30"

val extraLibs = mutableListOf(
    "org.ow2.asm:asm:9.7.1",
    "org.ow2.asm:asm-util:9.7.1",
    "org.ow2.asm:asm-commons:9.7.1",
    "org.ow2.asm:asm-tree:9.7.1",
    fastutilLib
)

val libraries = arrayListOf(
    "com.github.opai-client:opensource-components:-SNAPSHOT",
//    "org.lwjgl:lwjgl:2.9.4-nightly",
    "org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209",
    "org.jetbrains:annotations:24.0.0",
    "com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta4",
    files("libs/allatori-annotations.jar")
)
libraries.addAll(extraLibs)

dependencies {
    libraries.forEach(::compileOnly)
    extraLibs.forEach {
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
val dependenciesJars: MutableSet<File> = configurations.compileClasspath.get().files
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
val isRelease = project.hasProperty("release")

fun doOptimize(inputJar: File, outputJar: File, configFile: File) {
    if (!r8Jar.exists()) {
        throw GradleException("R8 JAR not found at ${r8Jar.absolutePath}, please reconfigure the gradle project.")
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

val optimize = tasks.register("optimize") {
    group = "optimization"
    description = "Run R8 to minimize the shadowJar output"

    onlyIf { isRelease }
    doLast {
        doOptimize(
            File(buildDir, "libs/%s-unoptimized.jar".format(project.name)),
            File(buildDir, "libs/%s-universal.jar".format(project.name)),
            File(rootDir, "r8-rules.pro")
        )
    }

    finalizedBy(obfuscate)
}

val allatoriJar = File(rootDir, "tools/allatori.jar")

val obfuscate = tasks.register("obfuscate") {
    group = "obfuscation"
    description = "Run Allatori to obfuscate the R8 output"

    onlyIf { isRelease }
    doLast {
        if (!allatoriJar.exists()) {
            throw GradleException("Allatori JAR not found at ${allatoriJar.absolutePath}, please download it manually.")
        }

        val configTemplate = File(rootDir, "allatori.xml")
        val configFile = File(buildDir, "tmp/allatori.xml")

        val classpathEntries = dependenciesJars.joinToString(separator = "\n") { jar ->
            """        <jar name="${jar.absolutePath}"/>"""
        }

        configFile.writeText(configTemplate.readText().replace("%CLASSPATH%", classpathEntries))

        exec {
            commandLine("java", "-jar", allatoriJar.absolutePath, configFile)
        }
    }

    finalizedBy(postOptimize)
}

val postOptimize = tasks.register("postOptimize") {
    group = "PostOptimization"
    description = "Run R8 to minimize the Allatori output"

    onlyIf { isRelease }
    doLast {
        doOptimize(
            File(buildDir, "libs/%s-obfuscated.jar".format(project.name)),
            File(buildDir, "libs/%s.jar".format(project.name)),
            File(rootDir, "post-r8-rules.pro")
        )
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
            extraLibs.forEach { include(dependency(it)) }
            // byd为什么这个类会有main方法，直接赖着不走
            exclude("it/unimi/dsi/fastutil/BigArrays.class")
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

        finalizedBy(optimize)
    }

    assemble {
        dependsOn(shadowJar)
    }
}
