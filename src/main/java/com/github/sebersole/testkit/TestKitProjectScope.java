package com.github.sebersole.testkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.gradle.testkit.runner.GradleRunner;

public class TestKitProjectScope {
	private final File projectBaseDirectory;

	public TestKitProjectScope(File projectBaseDirectory) {
		this.projectBaseDirectory = projectBaseDirectory;
	}

	public TestKitProjectScope(TestKitBaseScope testKitBaseScope, String projectName) {
		projectBaseDirectory = new File( testKitBaseScope.getBaseDirectory(), projectName );
		assert projectBaseDirectory.exists();
		assert projectBaseDirectory.isDirectory();
	}

	public File getProjectBaseDirectory() {
		return projectBaseDirectory;
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
				.withProjectDir( projectBaseDir )
				.withArguments( arguments );
	}
}
