plugins {
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
    // Auto-formatting (palantirJavaFormat) -- gates the build via `check`
    id("com.diffplug.spotless") version "7.0.2"
}

// Configure the extension
// License: GPL-3.0-or-later -- links GPLv3 QuPath core (and adapts code from
// Apache-2.0 QP-CAT, which is GPL-compatible). The GPL driver is the QuPath link,
// NOT QP-CAT. The shared 3D viewer lives in the Apache-2.0 cluster3d-core library;
// this repo is a thin GPL shell that shades core into the extension jar.
// A definitive license-check is run at the release gate.
qupathExtension {
    name = "qupath-extension-cluster-3d-navigator"
    group = "io.github.uw-loci"
    version = "0.1.3"
    description = "Interactive in-QuPath 3D point cloud of clustered cells; click a point to select and center that cell in the viewer. Generic across any clustering tool."
    automaticModule = "io.github.uw.loci.extension.cluster3dnav"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "SciJava"
            url = uri("https://maven.scijava.org/content/repositories/releases")
        }
        maven {
            name = "OME-Artifacts"
            url = uri("https://artifacts.openmicroscopy.org/artifactory/maven/")
        }
    }
}

val javafxVersion = "17.0.2"

dependencies {
    // Main dependencies for QuPath extensions (provided by QuPath at runtime).
    // This extension is PURE JAVA -- no Appose, no Python, no bundled native libs.
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // The shared 3D viewer lives in the Apache-2.0 cluster3d-core library. It is the
    // extension's OWN code (not host-provided), so it is an `implementation` dependency
    // and gets SHADED into the -all.jar. Build cluster3d-core with publishToMavenLocal
    // first (resolved here from mavenLocal). isTransitive=false: core's published POM
    // lists QuPath/JavaFX (injected by qupath-conventions) but the QuPath host provides
    // those at runtime -- bundling them would balloon the jar, so we shade only core.
    implementation("io.github.uw-loci:cluster3d-core:0.1.3") { isTransitive = false }

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation("io.github.qupath:qupath-app:0.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(libs.bundles.logging)
    testImplementation(libs.qupath.fxtras)
    testImplementation("org.openjfx:javafx-base:$javafxVersion")
    testImplementation("org.openjfx:javafx-graphics:$javafxVersion")
    testImplementation("org.openjfx:javafx-controls:$javafxVersion")
}

// NOTE on the shaded cluster3d-core package: this extension deliberately does NOT relocate
// it. QP-CAT relocates ITS copy to qupath.ext.qpcat.internal.cluster3d, so the two extensions
// no longer share the qupath.ext.cluster3d package -> no class collision / version-skew when
// both are installed. (Relocating here too is blocked by a shadow RelocatorRemapper ASM bug on
// this extension's own Cluster3DNavigatorExtension bytecode; relocating one side is sufficient.)

tasks.withType<JavaCompile> {
    options.release.set(21) // QuPath 0.7 runs on Java 21; pin bytecode target so any build JDK emits loadable classes
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.test {
    useJUnitPlatform()
    // Move JavaFX JARs from classpath to module path so --add-modules can find them.
    // Temurin JDK does not bundle JavaFX, so the modules are only available
    // as dependency JARs which Gradle places on the classpath by default.
    doFirst {
        val cp = classpath.files
        val fxJars = cp.filter { it.name.startsWith("javafx-") }
        if (fxJars.isNotEmpty()) {
            classpath = files(cp - fxJars)
            jvmArgs(
                "--module-path", fxJars.joinToString(File.pathSeparator),
                "--add-modules", "javafx.base,javafx.graphics,javafx.controls",
                "--add-opens", "javafx.graphics/javafx.stage=ALL-UNNAMED"
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Spotless -- auto-formatting (gates the build via `check`)
// ---------------------------------------------------------------------------
spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.90.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ---------------------------------------------------------------------------
// ASCII-only enforcement (CLAUDE.md policy: no chars > 0x7F in Java sources).
// Prevents Windows cp1252 encoding failures.
// ---------------------------------------------------------------------------
tasks.register("checkAsciiOnly") {
    description = "Fails if any Java source file contains non-ASCII characters (> 0x7F)"
    group = "verification"
    val srcDirs = fileTree("src") { include("**/*.java") }
    inputs.files(srcDirs)
    doLast {
        val violations = mutableListOf<String>()
        srcDirs.forEach { file ->
            file.readText().lines().forEachIndexed { idx, line ->
                line.forEachIndexed { col, ch ->
                    if (ch.code > 0x7F) {
                        violations.add(
                            "${file.relativeTo(projectDir)}:${idx + 1}:${col + 1}  " +
                                    "'$ch' (U+${"04X".format(ch.code)})"
                        )
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Non-ASCII characters found (will break on Windows cp1252):\n" +
                        violations.joinToString("\n")
            )
        }
        logger.lifecycle("checkAsciiOnly: all Java sources are ASCII-clean")
    }
}
tasks.named("check") { dependsOn("checkAsciiOnly") }

// QuPath 0.7.0's maven artifacts are published as requiring JVM 25 (org.gradle.jvm.version=25),
// even though the QuPath app runs on Java 21. options.release=21 makes Gradle resolve a
// JVM-21-compatible classpath, which then rejects those JVM-25 artifacts on a clean build. Force
// the resolvable classpaths to request JVM 25 so the deps resolve; bytecode target (21) is
// unaffected, so the jar still loads on Java 21. (Upstream QuPath metadata bug; remove if fixed.)
configurations.configureEach {
    if (isCanBeResolved) {
        attributes {
            attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}
