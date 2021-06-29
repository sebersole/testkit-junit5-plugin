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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_BASE_DIR;
import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_IMPL_PROJ_NAME;
import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_STAGING_DIR;
import static com.github.sebersole.testkit.TestKitPlugin.TEST_KIT;
import static java.lang.Character.LINE_SEPARATOR;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * @author Steve Ebersole
 */
public abstract class GenerateLocatorFileTask extends DefaultTask {
	private final DirectoryProperty testKitResourcesDirectory;
	private final DirectoryProperty testKitStagingDir;
	private final Property<String> implicitProjectName;
	private final RegularFileProperty locatorFile;

	@Inject
	public GenerateLocatorFileTask() {
		final ObjectFactory objectFactory = getProject().getObjects();
		final ProjectLayout layout = getProject().getLayout();

		testKitResourcesDirectory = objectFactory.directoryProperty();
		testKitStagingDir = objectFactory.directoryProperty();
		implicitProjectName = objectFactory.property( String.class );
		locatorFile = objectFactory.fileProperty();

		final SourceSet mainSourceSet = getProject().getConvention()
				.getPlugin( JavaPluginConvention.class )
				.getSourceSets()
				.getByName( TEST_KIT );

		testKitResourcesDirectory.convention( mainSourceSet.getResources().getDestinationDirectory() );
		testKitStagingDir.convention( layout.getBuildDirectory().dir( "tmp/testKit" ) );
	}

	@InputDirectory
	public DirectoryProperty getTestKitResourcesDirectory() {
		return testKitResourcesDirectory;
	}

	public DirectoryProperty getTestKitStagingDir() {
		return testKitStagingDir;
	}

	@OutputFile
	public RegularFileProperty getLocatorFile() {
		return locatorFile;
	}

	@Input
	public Property<String> getImplicitProjectName() {
		return implicitProjectName;
	}

	@TaskAction
	public void generateFile() {
		final File locatorFile = this.locatorFile.get().getAsFile();
		final File resourcesDir = testKitResourcesDirectory.get().getAsFile();
		final File stagingDir = testKitStagingDir.get().getAsFile();

		getLogger().debug( "TestKit locator file : {}", locatorFile.getAbsolutePath() );
		getLogger().debug( "TestKit resources dir : {}", resourcesDir.getAbsolutePath() );
		getLogger().debug( "TestKit staging dir : {}", stagingDir.getAbsolutePath() );

		prepareMarkerFile( locatorFile, getProject() );

		final DateTimeFormatter formatter = ofLocalizedDateTime( FormatStyle.MEDIUM )
				.withLocale( Locale.ROOT )
				.withZone( ZoneOffset.UTC );

		try ( FileWriter writer = new FileWriter( locatorFile ) ) {
			writer.write( "## Used by tests to locate the TestKit projects dir during test execution via resource lookup" );
			writer.write( LINE_SEPARATOR );
			writer.write( "## Generated : " + formatter.format( Instant.now() ) );
			writer.write( LINE_SEPARATOR );
			writer.write( TESTKIT_BASE_DIR + "=" + resourcesDir.getAbsolutePath().replace("\\","\\\\" ) );
			writer.write( LINE_SEPARATOR );
			writer.write( TESTKIT_STAGING_DIR + "=" + stagingDir.getAbsolutePath().replace("\\","\\\\" ) );
			writer.write( LINE_SEPARATOR );
			writer.write( TESTKIT_IMPL_PROJ_NAME + "=" + getImplicitProjectName().getOrElse( "" ) );
			writer.write( LINE_SEPARATOR );
			writer.flush();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to open marker file output stream `" + locatorFile.getAbsolutePath() + "`", e );
		}
	}

	private static void prepareMarkerFile(File markerFile, Project project) {
		project.getLogger().lifecycle( "Preparing marker : {}", markerFile.getAbsolutePath() );
		try {
			final boolean dirsCreated = markerFile.getParentFile().mkdirs();
			if ( ! dirsCreated ) {
				project.getLogger().lifecycle( "Marker file directory not created" );
			}

			final boolean created = markerFile.createNewFile();
			if ( ! created ) {
				project.getLogger().lifecycle( "File creation failed, but with no exception" );
			}
		}
		catch (IOException e) {
			throw new IllegalStateException( "Could not create marker file `" + markerFile.getAbsolutePath() + "`", e );
		}
	}
}
