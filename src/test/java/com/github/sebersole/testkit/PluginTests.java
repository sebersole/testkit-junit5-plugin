package com.github.sebersole.testkit;

import org.gradle.testkit.runner.GradleRunner;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;


@TestKit
public class PluginTests {
	@Test
	@Project( "simple" )
	public void basicTest(ProjectScope scope) {
		final GradleRunner gradleRunner = scope.createGradleRunner( "processTestKitResources" );
		verify( gradleRunner );

		gradleRunner.build();
	}

	private void verify(GradleRunner gradleRunner) {
		assertThat( gradleRunner.getProjectDir().getAbsolutePath(), endsWith( "/simple" ) );
	}

	@Test
	public void implicitTestKitProjectTest(ProjectScope scope) {
		final GradleRunner gradleRunner = scope.createGradleRunner( "processTestKitResources" );
		verify( gradleRunner );

		gradleRunner.build();
	}
}
