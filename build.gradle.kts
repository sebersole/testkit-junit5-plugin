import java.io.FileWriter

plugins {
    java
    `java-gradle-plugin`

    // for publishing to portal
    id( "com.gradle.plugin-publish" ) version "0.12.0"
    id( "nu.studer.credentials" ) version "2.1"

    // for publishing snapshots
    id( "maven-publish" )
    id("org.hibernate.build.maven-repo-auth") version "3.0.4"
}

val pluginId by extra( "com.github.sebersole.testkit-junit5" )
val pluginVersion by extra( "0.1-SNAPSHOT" )

group = "com.github.sebersole"
version = pluginId

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create( "testkit" ) {
            id = pluginId
            implementationClass = "com.github.sebersole.testkit.TestKitPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/hibernate/hibernate-orm/tree/master/tooling/hibernate-gradle-plugin"
    vcsUrl = "https://github.com/hibernate/hibernate-orm/tree/master/tooling/hibernate-gradle-plugin"
    tags = arrayListOf( "gradle", "testkit", "sebersole" )

    plugins {
        getByName("testkit" ) {
            displayName = "Gradle TestKit Helper"
            description = "Plugin for easier integration of Gradle's TestKit plugin testing library"
        }
    }
}


repositories {
    mavenCentral()
}

dependencies {
    val junitVersion by extra( "5.3.1" )
    val hamcrestVersion by extra( "1.3" )

    implementation( group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion )
    implementation( gradleTestKit() )

    testImplementation( group = "org.hamcrest", name = "hamcrest-all", version = hamcrestVersion )

    testRuntimeOnly( group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion )
}

tasks.test {
    useJUnitPlatform()
}


// ###########################################################################
// we need what the plugin provides for our tests, but... chicken meet egg
//      - so we do it by hand

val taskName = "processTestKitProject"
val testKitOutDirPath = "testkit"
val testKitSrcDirPath = "src/test/testkit"

val testKitOutDir = layout.buildDirectory.get().dir( testKitOutDirPath )
val testKitSrcDir = layout.projectDirectory.dir( testKitSrcDirPath )

dependencies {
    testRuntimeOnly( files( testKitOutDir ) )
}

tasks.register<Copy>( taskName ) {
    from( testKitSrcDir )
    into( testKitOutDir )
}

tasks.processTestResources.get().dependsOn( taskName )


// ###########################################################################

