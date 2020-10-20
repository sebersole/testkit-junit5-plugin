package com.github.sebersole.testkit;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

/**
 * @author Steve Ebersole
 */
public class TestKitPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaGradlePluginPlugin.class );

		final TestKitSpec testKitSpec = project.getExtensions().create( TestKitSpec.DSL_NAME, TestKitSpec.class, project );
		final TestKitTask processTestKitProjectsTask = project.getTasks().create( TestKitTask.DSL_NAME, TestKitTask.class, testKitSpec );

		final Test testTask = (Test) project.getTasks().getByName( "test" );
		testTask.dependsOn( processTestKitProjectsTask );

		// the plugin functionality is based on JUnit5 so force that issue
		project.getLogger().lifecycle( "Forcing use of JUnit 5 platform for `test` task" );
		testTask.useJUnitPlatform();

		// adjust the project's test classpath to include the testkit project-base-dir
		final Directory outputDirectory = testKitSpec.getOutputDirectory().get();
		project.getDependencies().add(
				"testRuntimeOnly",
				project.files( outputDirectory )
		);
	}
}
