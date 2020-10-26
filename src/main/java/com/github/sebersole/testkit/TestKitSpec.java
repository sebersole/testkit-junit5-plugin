package com.github.sebersole.testkit;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 * DSL extension for configuring the testkit-junit5 plugin
 */
public class TestKitSpec {
	public static final String DSL_NAME = "testKit";

	private final Property<String> testKitImplicitProject;

	@Inject
	public TestKitSpec(Project project) {
		this.testKitImplicitProject = project.getObjects().property( String.class );
	}

	public Property<String> getTestKitImplicitProject() {
		return testKitImplicitProject;
	}

	public void implicitProject(String projectName) {
		testKitImplicitProject.set( projectName );
	}

	public void setImplicitProject(String projectName) {
		testKitImplicitProject.set( projectName );
	}
}
