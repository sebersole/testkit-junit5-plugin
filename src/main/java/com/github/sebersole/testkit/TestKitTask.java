package com.github.sebersole.testkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * @author Steve Ebersole
 */
public class TestKitTask extends DefaultTask {
	public static final String DSL_NAME = "processTestKitProjects";
	public static final String MARKER_FILE_NAME = "testkit-project-dir-marker.properties";

	private final TestKitSpec spec;
	private final Provider<RegularFile> markerFileAsFileProvider;

	@Inject
	public TestKitTask(TestKitSpec spec) {
		this.spec = spec;

		markerFileAsFileProvider = spec.getOutputDirectory().file( MARKER_FILE_NAME );
	}

	@SuppressWarnings( { "unused", "RedundantSuppression" } )
	@OutputDirectory
	DirectoryProperty getOutputDirectory() {
		return spec.getOutputDirectory();
	}

	@InputFiles
	SetProperty<Directory> getSourceDirectories() {
		// todo : easy enough to make this incremental, though I doubt the benefit is worth the effort
		return spec.getSourceDirectories();
	}

	@TaskAction
	public void processProjects() {
		getProject().copy(
				copySpec -> {
					copySpec.into( spec.getOutputDirectory().get() );
					getSourceDirectories().get().forEach( copySpec::from );
				}
		);

		// every time we run the task, update the file so we know when inputs have changed next time
		generateMarkerFile( markerFileAsFileProvider.get(), getProject() );
	}

	private static void generateMarkerFile(RegularFile markerFile, Project project) {
		final File markerFileFile = prepareMarkerFile( markerFile, project );

		final DateTimeFormatter formatter = ofLocalizedDateTime( FormatStyle.MEDIUM )
				.withLocale( Locale.ROOT )
				.withZone( ZoneOffset.UTC );

		try ( FileWriter writer = new FileWriter( markerFileFile ) ) {
			writer.write( "## Used by tests to locate the TestKit projects dir during test execution via resource lookup" );
			writer.write( Character.LINE_SEPARATOR );
			writer.write( "## Generated : " + formatter.format( Instant.now() ) );
			writer.write( Character.LINE_SEPARATOR );
			writer.flush();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to open marker file output stream `" + markerFileFile.getAbsolutePath() + "`", e );
		}
	}

	private static File prepareMarkerFile(RegularFile markerFile, Project project) {
		final File markerFileAsFile = markerFile.getAsFile();
		try {
			final boolean created = markerFileAsFile.createNewFile();
			if ( ! created ) {
				project.getLogger().lifecycle( "File creation failed, but with no exception" );
			}

			return markerFileAsFile;
		}
		catch (IOException e) {
			throw new IllegalStateException( "Could not create marker file `" + markerFileAsFile.getAbsolutePath() + "`", e );
		}
	}
}
