package com.github.sebersole.testkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * Plugin for easier integration of Gradle's TestKit (functional plugin testing)
 * into plugin projects
 */
public class TestKitPlugin implements Plugin<Project> {
	public static final String TEST_KIT = "testKit";

	public static final String COMPILE_DEPENDENCIES_NAME = TEST_KIT + "CompileClasspath";
	public static final String RUNTIME_DEPENDENCIES_NAME = TEST_KIT + "RuntimeClasspath";
	public static final String TEST_TASK_NAME = TEST_KIT + "Test";

	public static final String MARKER_FILE_NAME = "testkit_locator.properties";

	public static final String JUNIT_VERSION = "5.3.1";
	public static final String HAMCREST_VERSION = "1.3";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaGradlePluginPlugin.class );

		// creates Configurations and primes with normal dependencies
		final Configuration compileDependencies = prepareCompileDependencies( project );
		final Configuration runtimeDependencies = prepareRuntimeDependencies( project );

		// create the TestKit SourceSet
		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		final SourceSetContainer sourceSetContainer = javaPluginConvention.getSourceSets();
		final SourceSet sourceSet = sourceSetContainer.create( TEST_KIT );
		sourceSet.setCompileClasspath( sourceSet.getCompileClasspath().plus( compileDependencies ) );
		sourceSet.setRuntimeClasspath( sourceSet.getRuntimeClasspath().plus( runtimeDependencies ).plus( sourceSet.getOutput() ) );

		// create the TestKit test task
		final Test testKitTest = project.getTasks().create( TEST_TASK_NAME, Test.class );
		testKitTest.setTestClassesDirs( sourceSet.getOutput().getClassesDirs() );
		testKitTest.setClasspath( sourceSet.getRuntimeClasspath() );
		testKitTest.dependsOn( "test" );
		final Task copyTask = project.getTasks().getByName( sourceSet.getTaskName( "process", "Resources" ) );
		testKitTest.dependsOn( copyTask );
		final Task compileTask = project.getTasks().getByName( sourceSet.getTaskName( "compile", "Java" ) );
		testKitTest.dependsOn( compileTask );

		// the plugin functionality is based on JUnit5 so force that issue
		project.getLogger().lifecycle( "Forcing use of JUnit 5 platform for `testKitTest` task" );
		testKitTest.useJUnitPlatform();

		// generate the marker file after processing TestKit resources (project container)
		copyTask.doLast( (task) -> generateMarkerFile( sourceSet, project ) );

	}

	private static Configuration prepareCompileDependencies(Project project) {
		final Configuration dependencies = project.getConfigurations().maybeCreate( COMPILE_DEPENDENCIES_NAME );
		dependencies.setDescription( "Compile-time dependencies for the TestKit testing" );
		dependencies.extendsFrom( project.getConfigurations().getByName( "testCompileClasspath" ) );

		final DependencyHandler dependencyHandler = project.getDependencies();

		dependencyHandler.add( dependencies.getName(), dependencyHandler.gradleApi() );
		dependencyHandler.add( dependencies.getName(), dependencyHandler.gradleTestKit() );

		dependencyHandler.add( dependencies.getName(), "org.hamcrest:hamcrest-all:" + HAMCREST_VERSION );

		dependencyHandler.add( dependencies.getName(), "org.junit.jupiter:junit-jupiter-api:" + JUNIT_VERSION );
		dependencyHandler.add( dependencies.getName(), "org.junit.jupiter:junit-jupiter-params:" + JUNIT_VERSION );

		final Configuration buildScriptClasspath = project.getBuildscript().getConfigurations().getByName( "classpath" );
		final Set<ResolvedArtifact> resolvedArtifacts = buildScriptClasspath.getResolvedConfiguration().getResolvedArtifacts();
		for ( ResolvedArtifact resolvedArtifact : resolvedArtifacts ) {
			final ModuleVersionIdentifier dependencyId = resolvedArtifact.getModuleVersion().getId();
			project.getLogger().lifecycle( "Checking buildscript classpath entry: {}", dependencyId );
			if ( "com.github.sebersole".equals( dependencyId.getGroup() ) ) {
				if ( "testkit-junit5-plugin".equals( dependencyId.getName() ) ) {

					// we found this plugin's dependency... add it to the TestKit compile-classpath
					project.getLogger().lifecycle( "  > Found testkit-junit5-plugin dependency : `{}`", resolvedArtifact.getFile() );
					dependencyHandler.add( dependencies.getName(), project.files( resolvedArtifact.getFile() ) );

					break;
				}
			}
		}

		return dependencies;
	}

	private static Configuration prepareRuntimeDependencies(Project project) {
		final Configuration dependencies = project.getConfigurations().maybeCreate( RUNTIME_DEPENDENCIES_NAME );
		dependencies.setDescription( "Run-time dependencies for the TestKit testing" );
		dependencies.extendsFrom( project.getConfigurations().getByName( COMPILE_DEPENDENCIES_NAME ) );
		dependencies.extendsFrom( project.getConfigurations().getByName( "testRuntimeClasspath" ) );

		final DependencyHandler dependencyHandler = project.getDependencies();

		dependencyHandler.add( dependencies.getName(), "org.junit.jupiter:junit-jupiter-engine:" + JUNIT_VERSION );

		return dependencies;
	}

	private static void generateMarkerFile(SourceSet sourceSet, Project project) {
		final File resourcesDir = sourceSet.getOutput().getResourcesDir();
		final File markerFile = new File( resourcesDir, MARKER_FILE_NAME );

		prepareMarkerFile( markerFile, project );

		final DateTimeFormatter formatter = ofLocalizedDateTime( FormatStyle.MEDIUM )
				.withLocale( Locale.ROOT )
				.withZone( ZoneOffset.UTC );
		final Directory tmpDir = project.getLayout().getBuildDirectory().get().dir( "tmp" ).dir( TEST_KIT );

		try ( FileWriter writer = new FileWriter( markerFile ) ) {
			writer.write( "## Used by tests to locate the TestKit projects dir during test execution via resource lookup" );
			writer.write( Character.LINE_SEPARATOR );
			writer.write( "## Generated : " + formatter.format( Instant.now() ) );
			writer.write( Character.LINE_SEPARATOR );
			writer.write( "tmp-dir=" + tmpDir.getAsFile().getAbsolutePath() );
			writer.write( Character.LINE_SEPARATOR );
			writer.flush();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to open marker file output stream `" + markerFile.getAbsolutePath() + "`", e );
		}
	}

	private static void prepareMarkerFile(File markerFile, Project project) {
		try {
			final boolean created = markerFile.createNewFile();
			if ( ! created ) {
				project.getLogger().lifecycle( "File creation failed, but with no exception" );
			}
		}
		catch (IOException e) {
			throw new IllegalStateException( "Could not create marker file `" + markerFile.getAbsolutePath() + "`", e );
		}
	}

}
