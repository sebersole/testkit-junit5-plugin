import java.io.FileWriter
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import nu.studer.gradle.credentials.domain.CredentialsContainer

plugins {
    `java-library`
    `java-gradle-plugin`

    // for publishing to portal
    id("com.gradle.plugin-publish") version "0.12.0"
    id("nu.studer.credentials") version "2.1"
}

group = "com.github.sebersole"
version = "1.2-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    val junitVersion by extra("5.3.1")
    val hamcrestVersion by extra("1.3")

    api( gradleApi() )

    implementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    implementation(gradleTestKit())

    testImplementation(group = "org.hamcrest", name = "hamcrest-all", version = hamcrestVersion)
    testImplementation(gradleTestKit())

    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

val pluginId by extra("com.github.sebersole.testkit-junit5")

gradlePlugin {
    plugins {
        create("testkit") {
            id = pluginId
            implementationClass = "com.github.sebersole.testkit.TestKitPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/sebersole/testkit-junit5-plugin"
    vcsUrl = "https://github.com/sebersole/testkit-junit5-plugin"
    tags = arrayListOf("gradle", "testkit", "testing", "sebersole")

    plugins {
        getByName("testkit") {
            displayName = "Gradle TestKit Helper"
            description = "Plugin for easier integration of Gradle's TestKit plugin testing library"
        }
    }
}

val credentials: CredentialsContainer by project.extra

if ( project.hasProperty( "pluginPortalUsername" ) ) {
    credentials.setProperty( "pluginPortalUsername", project.property( "pluginPortalUsername" ) );
}
if ( credentials.getProperty( "pluginPortalUsername" ) != null ) {
    project.setProperty( "gradle.publish.key", credentials.getProperty( "pluginPortalUsername" ) )
}

if ( project.hasProperty( "pluginPortalPassword" ) ) {
    credentials.setProperty( "pluginPortalPassword", project.property( "pluginPortalPassword" ) );
}
if ( credentials.getProperty( "pluginPortalPassword" ) != null ) {
    project.setProperty( "gradle.publish.secret", credentials.getProperty( "pluginPortalPassword" ) )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}


(tasks.javadoc.get().options as CoreJavadocOptions).addStringOption( "Xdoclint:none", "-quiet" )


// ###########################################################################
// we need what the plugin provides for our tests, but... chicken meet egg, so
// we have to do some of it by hand

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
            writer.appendln("testkit.tmp-dir=" + tmpDir.asFile.absolutePath.replace("\\","\\\\") )
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

