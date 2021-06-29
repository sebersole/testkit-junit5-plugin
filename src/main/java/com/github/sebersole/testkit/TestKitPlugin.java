package com.github.sebersole.testkit;

import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

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
	public static final String TESTKIT_BASE_DIR = "testkit.base-dir";
	public static final String TESTKIT_STAGING_DIR = "testkit.staging-dir";
	public static final String TESTKIT_IMPL_PROJ_NAME = "testkit.implicit-project-name";

	public static final String JUNIT_VERSION = "5.3.1";
	public static final String HAMCREST_VERSION = "1.3";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaGradlePluginPlugin.class );

		final TestKitSpec testKitSpec = project.getExtensions().create( TestKitSpec.DSL_NAME, TestKitSpec.class, project );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create Configurations and primes with normal dependencies

		final Configuration compileDependencies = prepareCompileDependencies( project );
		final Configuration runtimeDependencies = prepareRuntimeDependencies( project );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the TestKit SourceSet

		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;

		final SourceSetContainer sourceSetContainer = javaPluginConvention.getSourceSets();
		final SourceSet testKitSourceSet = sourceSetContainer.create( TEST_KIT );
		final Test mainTestTask = (Test) project.getTasks().getByName( "test" );
		testKitSourceSet.setCompileClasspath( testKitSourceSet.getCompileClasspath().plus( compileDependencies ).plus( mainTestTask.getClasspath() ) );
		testKitSourceSet.setRuntimeClasspath( testKitSourceSet.getRuntimeClasspath().plus( runtimeDependencies ).plus( testKitSourceSet.getOutput() ) );

		testKitSourceSet.getResources().getDestinationDirectory().convention(
				project.getLayout().getBuildDirectory().dir( "resources/testKit" )
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the TestKit tasks

		final Copy copyTask = (Copy) project.getTasks().getByName( testKitSourceSet.getTaskName( "process", "Resources" ) );
		copyTask.setGroup( TEST_KIT );
		copyTask.setDescription( "Copies the TestKit projects" );

		final Task compileTask = project.getTasks().getByName( testKitSourceSet.getTaskName( "compile", "Java" ) );
		compileTask.setGroup( TEST_KIT );
		compileTask.setDescription( "Compiles the TestKit sources" );

		final Test testKitTest = project.getTasks().create( TEST_TASK_NAME, Test.class );
		compileTask.setGroup( TEST_KIT );
		compileTask.setDescription( "Executes the TestKit tests" );
		testKitTest.setTestClassesDirs( testKitSourceSet.getOutput().getClassesDirs() );
		testKitTest.setClasspath( testKitSourceSet.getRuntimeClasspath() );

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

		final GenerateLocatorFileTask generateLocatorFileTask = createGenerateMarkerFileTask( project, testKitSourceSet, testKitSpec );
		copyTask.finalizedBy( generateLocatorFileTask );
		testKitSourceSet.getResources().srcDir( generateLocatorFileTask.getLocatorFile().get().getAsFile().getParentFile() );
	}

	private static GenerateLocatorFileTask createGenerateMarkerFileTask(
			Project project,
			SourceSet testKitSourceSet,
			TestKitSpec testKitSpec) {
		final GenerateLocatorFileTask generateLocatorFileTask = project.getTasks().create(
				"generateTestKitMarkerFile",
				GenerateLocatorFileTask.class
		);

		generateLocatorFileTask.getLocatorFile().set(
				project.getLayout().getBuildDirectory().file( "testKit/locator/" + MARKER_FILE_NAME )
		);
		generateLocatorFileTask.getTestKitResourcesDirectory().set(
				testKitSourceSet.getResources().getDestinationDirectory()
		);
		generateLocatorFileTask.getTestKitStagingDir().set(
				project.getLayout().getBuildDirectory().dir( "tmp/" + TEST_KIT )
		);
		if ( testKitSpec.getImplicitProjectName().isPresent() ) {
			generateLocatorFileTask.getImplicitProjectName().set( testKitSpec.getImplicitProjectName().get() );
		}
		else {
			generateLocatorFileTask.getImplicitProjectName().set( "" );
		}

		return generateLocatorFileTask;
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

}
