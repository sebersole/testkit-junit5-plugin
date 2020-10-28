package com.github.sebersole.testkit;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 * DSL extension for configuring the testkit-junit5 plugin
 */
public class TestKitSpec {
	public static final String DSL_NAME = "testKit";

	private final Property<String> implicitProjectName;

	@Inject
	public TestKitSpec(Project project) {
		this.implicitProjectName = project.getObjects().property( String.class );
	}

	public Property<String> getImplicitProjectName() {
		return implicitProjectName;
	}

	public void implicitProject(String projectName) {
		implicitProjectName.set( projectName );
	}

	public void setImplicitProject(String projectName) {
		implicitProjectName.set( projectName );
	}
}
