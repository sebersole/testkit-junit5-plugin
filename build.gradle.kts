import java.io.FileWriter
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import org.gradle.external.javadoc.CoreJavadocOptions

plugins {
    java
    `java-gradle-plugin`

    // for publishing to portal
    id("com.gradle.plugin-publish") version "0.12.0"
    id("nu.studer.credentials") version "2.1"

    // for publishing snapshots
    id("maven-publish")
    id("org.hibernate.build.maven-repo-auth") version "3.0.4"
}

val pluginId by extra("com.github.sebersole.testkit-junit5")
val pluginVersion by extra("1.0-SNAPSHOT" )

group = "com.github.sebersole"
version = pluginVersion

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("testkit") {
            id = pluginId
            implementationClass = "com.github.sebersole.testkit.TestKitPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/hibernate/hibernate-orm/tree/master/tooling/hibernate-gradle-plugin"
    vcsUrl = "https://github.com/hibernate/hibernate-orm/tree/master/tooling/hibernate-gradle-plugin"
    tags = arrayListOf("gradle", "testkit", "sebersole")

    plugins {
        getByName("testkit") {
            displayName = "Gradle TestKit Helper"
            description = "Plugin for easier integration of Gradle's TestKit plugin testing library"
        }
    }
}


repositories {
    mavenCentral()
}

dependencies {
    val junitVersion by extra("5.3.1")
    val hamcrestVersion by extra("1.3")

    implementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    implementation(gradleTestKit())

    testImplementation(group = "org.hamcrest", name = "hamcrest-all", version = hamcrestVersion)
    testImplementation(gradleTestKit())

    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}


publishing {
    publications {
        create("testKitPlugin", MavenPublication::class) {
            from(components.getByName("java"))
        }
    }
    repositories {
        maven("https://repository.jboss.org/nexus/content/repositories/snapshots") {
            name = "jboss-snapshots-repository"
        }
    }
}

(tasks.javadoc.get().options as CoreJavadocOptions).addStringOption( "Xdoclint:none", "-quiet" )


// ###########################################################################
// we need what the plugin provides for our tests, but... chicken meet egg
//      - so we have to do some of it by hand

tasks.test {
    useJUnitPlatform()
}

val generateMarkerFileTask = task( "generateMarkerFile" ) {
    val markerFile = File( tasks.processTestResources.get().destinationDir, "testkit_locator.properties" )
    inputs.files( tasks.processTestResources )
    outputs.file(markerFile)

    doLast {
        generateMarkerFile( sourceSets.test.get(), project )
    }
}

tasks.processTestResources.get().finalizedBy( generateMarkerFileTask )

fun generateMarkerFile(sourceSet: SourceSet, project: Project) {
    val resourcesDir = sourceSet.output.resourcesDir
    val markerFile = File( resourcesDir, "testkit_locator.properties" )
    prepareMarkerFile(markerFile, project)
    val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.ROOT)
            .withZone(ZoneOffset.UTC)
    val tmpDir: Directory = project.layout.buildDirectory.get().dir("tmp" ).dir( "testKit" )
    try {
        FileWriter(markerFile).use { writer ->
            writer.appendln("## Used by tests to locate the TestKit projects dir during test execution via resource lookup" )
            writer.appendln("## Generated @ " + formatter.format( Instant.now() ) )
            writer.appendln("testkit.tmp-dir=" + tmpDir.asFile.absolutePath )
            writer.appendln("testkit.implicit-project-name=simple" )
            writer.flush()
        }
    } catch (e: IOException) {
        throw IllegalStateException("Unable to open marker file output stream `" + markerFile.absolutePath + "`", e)
    }
}

fun prepareMarkerFile(markerFile: File, project: Project) {
    try {
        val created = markerFile.createNewFile()
        if (!created) {
            project.logger.lifecycle("File creation failed, but with no exception")
        }
    } catch (e: IOException) {
        throw IllegalStateException("Could not create marker file `" + markerFile.absolutePath + "`", e)
    }
}

// ###########################################################################

