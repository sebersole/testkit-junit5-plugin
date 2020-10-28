package com.github.sebersole.testkit;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.Test;

import static com.github.sebersole.testkit.TestKitPlugin.COMPILE_DEPENDENCIES_NAME;
import static com.github.sebersole.testkit.TestKitPlugin.RUNTIME_DEPENDENCIES_NAME;
import static com.github.sebersole.testkit.TestKitPlugin.TEST_KIT;

/**
 * @author Steve Ebersole
 */
public class IntegrationTests {
	@Test
	public void testIt() {
		final ProjectContainer projectContainer = new ProjectContainer();
		final ProjectScope projectScope = projectContainer.getProjectScope( "simple" );

		final File projectBaseDir = projectScope.getProjectBaseDirectory();

		final Project project = ProjectBuilder.builder().withProjectDir( projectBaseDir ).build();

		project.getPlugins().apply( TestKitPlugin.class );

		// NOTE : the use of `#getByName` enforces that the thing exists (non-null)

		project.getPlugins().getPlugin( TestKitPlugin.class );
		project.getExtensions().getByName( TestKitSpec.DSL_NAME );
		project.getConfigurations().getByName( COMPILE_DEPENDENCIES_NAME );
		project.getConfigurations().getByName( RUNTIME_DEPENDENCIES_NAME );

		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;
		final SourceSetContainer sourceSetContainer = javaPluginConvention.getSourceSets();
		sourceSetContainer.getByName( TEST_KIT );

		project.getTasks().getByName( "processTestKitResources" );
		project.getTasks().getByName( "compileTestKitJava" );
		project.getTasks().getByName( "testKitTest" );
	}
}
