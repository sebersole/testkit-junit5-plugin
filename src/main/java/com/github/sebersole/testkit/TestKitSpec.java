package com.github.sebersole.testkit;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

/**
 * @author Steve Ebersole
 */
public class TestKitSpec {
	public static final String TESTKIT = "testkit";
	public static final String DSL_NAME = TESTKIT;

	private final Project gradleProject;
	private final DirectoryProperty outputDirectory;
	private final SetProperty<Directory> sourceDirectories;

	@Inject
	@SuppressWarnings( "UnstableApiUsage" )
	public TestKitSpec(Project gradleProject) {
		this.gradleProject = gradleProject;

		outputDirectory = gradleProject.getObjects().directoryProperty();
		outputDirectory.convention(
				gradleProject.provider( () -> gradleProject.getLayout().getBuildDirectory().dir( TESTKIT ).get() )
		);

		sourceDirectories = gradleProject.getObjects().setProperty( Directory.class );
	}

	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	public SetProperty<Directory> getSourceDirectories() {
		return sourceDirectories;
	}


	@SuppressWarnings( { "unused", "RedundantSuppression" } )
	public void sourceDirectory(Directory directory) {
		sourceDirectories.add( directory );
	}

	@SuppressWarnings( { "unused", "RedundantSuppression" } )
	public void sourceDirectory(Object path) {
		final ProjectLayout layout = gradleProject.getLayout();
		final Provider<File> fileProvider = gradleProject.provider( () -> gradleProject.file( path ) );
		final Directory directory = layout.dir( fileProvider ).get();
		sourceDirectories.add( directory );
	}
}
