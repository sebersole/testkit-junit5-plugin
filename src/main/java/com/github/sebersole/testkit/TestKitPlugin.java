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

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
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
	public static final String TESTKIT_TMP_DIR = "testkit.tmp-dir";
	public static final String TESTKIT_IMPL_PROJ_NAME = "testkit.implicit-project-name";

	public static final String JUNIT_VERSION = "5.3.1";
	public static final String HAMCREST_VERSION = "1.3";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaGradlePluginPlugin.class );

		final TestKitSpec testKitSpec = project.getExtensions().create( TestKitSpec.DSL_NAME, TestKitSpec.class, project );

		final Test mainTestTask = (Test) project.getTasks().getByName( "test" );

		// creates Configurations and primes with normal dependencies
		final Configuration compileDependencies = prepareCompileDependencies( project );
		final Configuration runtimeDependencies = prepareRuntimeDependencies( project );

		// create the TestKit SourceSet
		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;

		final SourceSetContainer sourceSetContainer = javaPluginConvention.getSourceSets();
		final SourceSet sourceSet = sourceSetContainer.create( TEST_KIT );
		sourceSet.setCompileClasspath( sourceSet.getCompileClasspath().plus( compileDependencies ).plus( mainTestTask.getClasspath() ) );
		sourceSet.setRuntimeClasspath( sourceSet.getRuntimeClasspath().plus( runtimeDependencies ).plus( sourceSet.getOutput() ) );

		// create the TestKit tasks

		final Copy copyTask = (Copy) project.getTasks().getByName( sourceSet.getTaskName( "process", "Resources" ) );
		copyTask.setGroup( TEST_KIT );
		copyTask.setDescription( "Copies the TestKit projects" );

		final Task compileTask = project.getTasks().getByName( sourceSet.getTaskName( "compile", "Java" ) );
		compileTask.setGroup( TEST_KIT );
		compileTask.setDescription( "Compiles the TestKit sources" );

		final Test testKitTest = project.getTasks().create( TEST_TASK_NAME, Test.class );
		compileTask.setGroup( TEST_KIT );
		compileTask.setDescription( "Executes the TestKit tests" );
		testKitTest.setTestClassesDirs( sourceSet.getOutput().getClassesDirs() );
		testKitTest.setClasspath( sourceSet.getRuntimeClasspath() );

		copyTask.dependsOn( compileDependencies );
		copyTask.dependsOn( mainTestTask );
		compileTask.dependsOn( mainTestTask );
		testKitTest.dependsOn( copyTask );
		testKitTest.dependsOn( compileTask );
		testKitTest.dependsOn( "pluginDescriptors" );
		testKitTest.dependsOn( "pluginUnderTestMetadata" );

		project.getTasks().getByName( "check" ).dependsOn( testKitTest );

		// the plugin functionality is based on JUnit5...
		testKitTest.useJUnitPlatform();

		sourceSet.getResources().getDestinationDirectory().convention(
				project.getLayout().getBuildDirectory().dir( "resources/testKit" )
		);

		final Provider<RegularFile> markerFile = sourceSet.getResources().getDestinationDirectory().file( MARKER_FILE_NAME );

		// generate the marker file after processing TestKit resources (project container)
		copyTask.getOutputs().file( markerFile );
		copyTask.doLast(
				new Action<Task>() {
					@Override
					public void execute(Task task) {
						generateMarkerFile( markerFile, testKitSpec, project );
					}
				}
		);

		// NOTE : writing the marker file to the resources output dir causes the ProcessResources
		// 		task that wrote there to become out-of-date
		// todo : fix this ^^
		//		- write to another directory and adjust the "downstream classpaths"
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
			project.getLogger().debug( "Checking buildscript classpath entry: {}", dependencyId );
			if ( "com.github.sebersole".equals( dependencyId.getGroup() ) ) {
				if ( "testkit-junit5-plugin".equals( dependencyId.getName() ) ) {

					// we found this plugin's dependency... add it to the TestKit compile-classpath
					project.getLogger().debug( "  > Found testkit-junit5-plugin dependency : `{}`", resolvedArtifact.getFile() );
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

	private static void generateMarkerFile(Provider<RegularFile> markerFileRef, TestKitSpec testKitSpec, Project project) {
		final File markerFile = markerFileRef.get().getAsFile();
		project.getLogger().lifecycle( "Generating TestKit marker file - {}", markerFile.getAbsolutePath() );

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
			writer.write( TESTKIT_TMP_DIR + "=" + tmpDir.getAsFile().getAbsolutePath().replace("\\","\\\\" ) );
			writer.write( Character.LINE_SEPARATOR );
			writer.write( TESTKIT_IMPL_PROJ_NAME + "=" + testKitSpec.getImplicitProjectName().getOrElse( "" ) );
			writer.write( Character.LINE_SEPARATOR );
			writer.flush();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to open marker file output stream `" + markerFile.getAbsolutePath() + "`", e );
		}
	}

	private static void prepareMarkerFile(File markerFile, Project project) {
		try {
			final boolean dirsCreated = markerFile.getParentFile().mkdirs();
			if ( ! dirsCreated ) {
				project.getLogger().lifecycle( "Marker file directory not created" );
			}

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
