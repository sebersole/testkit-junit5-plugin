pluginManagement {
    repositories {
        gradlePluginPortal()

        mavenCentral()
        maven( "https://repository.jboss.org/nexus/content/repositories/snapshots" ) {
            name = "jboss-snapshots-repository"
        }
    }
}

rootProject.name = "testkit-junit5-plugin"

