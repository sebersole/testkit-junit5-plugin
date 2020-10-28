package com.github.sebersole.testkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.gradle.testkit.runner.GradleRunner;

/**
 * Provides the test with access to a TestKit {@link GradleRunner}
 * for the specified project.
 *
 * Manages a temp directory for the project for a specific test
 */
public class ProjectScope {
	private final File projectBaseDirectory;

	public ProjectScope(File projectBaseDirectory) {
		this.projectBaseDirectory = projectBaseDirectory;
	}

	public File getProjectBaseDirectory() {
		return projectBaseDirectory;
	}

	void release() {
		// delete the directory after we are done with it
		projectBaseDirectory.deleteOnExit();
	}

	public GradleRunner createGradleRunner(String... args) {
		return createGradleRunner( projectBaseDirectory, args );
	}

	public static GradleRunner createGradleRunner(File projectBaseDir, String... args) {
		final ArrayList<String> arguments = new ArrayList<>( Arrays.asList( args ) );
		arguments.add( "--stacktrace" );

		return GradleRunner.create()
				.withPluginClasspath()
				.withDebug( true )
				.forwardOutput()
				.withProjectDir( projectBaseDir )
				.withArguments( arguments );
	}
}
