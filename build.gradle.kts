import nu.studer.gradle.credentials.domain.CredentialsContainer
import java.io.FileWriter
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

plugins {
    `java-library`
    `java-gradle-plugin`

    // for publishing to portal
    id("com.gradle.plugin-publish") version "0.12.0"

    // to be able to publish locally.  see `publishing {}` below
    `maven-publish`

    id("nu.studer.credentials") version "2.1"
}

group = "com.github.sebersole"
version = "1.3.0"


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

    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()
    }

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

val generateMarkerFileTask = task( "generateLocalMarkerFile" ) {
    val locatorDir = project.file( "$buildDir/testKit/locator" )
    val locatorFile = File( locatorDir, "testkit_locator.properties" )
    val resourcesDir: File = project.file( "$buildDir/build/resources/test" )
    val stagingDir: File = project.file( "$buildDir/tmp/testKit" )

//    val resources = sourceSets.getByName( "test" ).resources
//    resources.srcDir( locatorDir )
    tasks.test.get().classpath += project.files( locatorDir )
//    sourceSets.getByName( "test" ).resources.srcDir( locatorDir )

    outputs.file( locatorFile )
    inputs.files( "$buildDir/build/resources/test" )

    doLast {
        logger.lifecycle("TestKit locator file : {}", locatorFile)
        logger.lifecycle("TestKit resources dir : {}", resourcesDir)
        logger.lifecycle("TestKit staging dir : {}", stagingDir)

        try {
            val created = locatorFile.createNewFile()
            if (!created) {
                project.logger.lifecycle("File creation failed, but with no exception")
            }
        }
        catch (e: IOException) {
            throw IllegalStateException("Could not create marker file `" + locatorFile.absolutePath + "`", e)
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.ROOT)
                .withZone(ZoneOffset.UTC)
        val tmpDir: Directory = project.layout.buildDirectory.get().dir("tmp" ).dir( "testKit" )
        try {
            FileWriter(locatorFile).use { writer ->
                writer.appendln("## Used by tests to locate the TestKit projects dir during test execution via resource lookup" )
                writer.appendln("## Generated @ " + formatter.format( Instant.now() ) )
                writer.appendln("testkit.base-dir=" + project.file( "$buildDir/resources/test" ).absolutePath.replace("\\","\\\\") )
                writer.appendln("testkit.staging-dir=" + tmpDir.asFile.absolutePath.replace("\\","\\\\") )
                writer.appendln("testkit.implicit-project-name=simple" )
                writer.flush()
            }
        } catch (e: IOException) {
            throw IllegalStateException("Unable to open marker file output stream `" + locatorFile.absolutePath + "`", e)
        }
    }
}



tasks.processTestResources.get().finalizedBy( generateMarkerFileTask )

// ###########################################################################


publishing {
    // Used to publish the plugin locally for testing.  To consume the plugin
    // from here, the applying project needs to add this as a plugin repo.
    // See https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#custom-plugin-repositories
    //
    // publishes to `~/.gradle/tmp/plugins`
    //
    // Use `gradlew publish` (instead of `gradlew publishPlugins`) to publish the
    // plugin to this local plugin repo
    repositories {
        maven {
            name = "local"
            url = uri( "${gradle.gradleUserHomeDir}/tmp/plugins" )
        }
    }
}
