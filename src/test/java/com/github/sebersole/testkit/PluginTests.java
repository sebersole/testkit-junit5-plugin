package com.github.sebersole.testkit;

import java.io.File;

import org.gradle.testkit.runner.GradleRunner;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;

/**
 * @author Steve Ebersole
 */
@TestKit
public class PluginTests {

	@Test
	public void baseline() {
		System.out.println( "Here I am" );
	}

	@Test
	public void testBaseScope(TestKitBaseScope scope) {
		assertThat( scope.getBaseDirectory().getAbsolutePath(), endsWith( "testkit" ) );

		final TestKitProjectScope simpleProjectScope = scope.getProjectScope( "simple" );
		final File projectBaseDirectory = simpleProjectScope.getProjectBaseDirectory();

		assertThat( projectBaseDirectory.getAbsolutePath(), endsWith( "testkit/simple" ) );

		final GradleRunner gradleRunner = simpleProjectScope.createGradleRunner( TestKitTask.DSL_NAME, "build" );
		gradleRunner.build();
	}

	@Test
	@TestKitProject( "simple" )
	public void testProjectScope(TestKitProjectScope scope) {
		assertThat( scope.getProjectBaseDirectory().getAbsolutePath(), endsWith( "testkit/simple" ) );

		final GradleRunner gradleRunner = scope.createGradleRunner( "build" );
		gradleRunner.build();
	}

	@Test
	@TestKitProject( "simple" )
	public void testProjectScope(File projectBaseDirectoryAsFile) {
		assertThat( projectBaseDirectoryAsFile.getAbsolutePath(), endsWith( "testkit/simple" ) );

		final GradleRunner gradleRunner = TestKitProjectScope.createGradleRunner( projectBaseDirectoryAsFile, "build" );
		gradleRunner.build();
	}
}
