package com.github.sebersole.testkit;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;


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
		final GradleRunner gradleRunner = scope.createGradleRunner( "clean", "processTestKitResources" );
		verify( gradleRunner );
		gradleRunner.build();
	}

	@Test
	@Project( "simple" )
	@Disabled( "https://discuss.gradle.org/t/generate-file-into-processresources-output-directory-up-to-date/38059" )
	public void multipleProcessResourcesExecutions(ProjectScope scope) {
		{
			final GradleRunner gradleRunner = scope.createGradleRunner( "clean", "processTestKitResources" );
			verify( gradleRunner );
			final BuildResult buildResult = gradleRunner.build();
			final BuildTask taskResult = buildResult.task( ":processTestKitResources" );
			assertThat( taskResult, notNullValue() );
			assertThat( taskResult.getOutcome(), is( TaskOutcome.SUCCESS ) );
		}

		// run it a second time without cleaning
		{
			final GradleRunner gradleRunner = scope.createGradleRunner( "processTestKitResources" );
			verify( gradleRunner );
			final BuildResult buildResult = gradleRunner.build();
			final BuildTask taskResult = buildResult.task( ":processTestKitResources" );
			assertThat( taskResult, notNullValue() );
			assertThat( taskResult.getOutcome(), is( TaskOutcome.UP_TO_DATE ) );
		}
	}
}
