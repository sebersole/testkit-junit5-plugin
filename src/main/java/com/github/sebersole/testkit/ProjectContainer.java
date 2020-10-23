package com.github.sebersole.testkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import static com.github.sebersole.testkit.TestKitPlugin.MARKER_FILE_NAME;
import static com.github.sebersole.testkit.TestKitPlugin.TEST_KIT;

/**
 * A manager for the TestKit projects
 */
public class ProjectContainer {
	private final File projectBaseDir;
	private final File tmpDir;
	private final Random randomGenerator = new Random();

	private final Set<String> projectNames;

	public ProjectContainer() {
		final URL markerFileUrl = locateMarker();
		if ( markerFileUrl == null ) {
			throw new IllegalStateException( "Could not locate TestKit project dir marker file (`" + MARKER_FILE_NAME + "`)" );
		}

		final String markerFilePath = markerFileUrl.getFile();
		final File markerFile = new File( markerFilePath );

		tmpDir = extractTmpDir( markerFile );

		this.projectBaseDir = markerFile.getParentFile();
		verifyBaseDir( projectBaseDir );

		final File[] projectDirectories = projectBaseDir.listFiles();
		assert projectDirectories != null;
		final Set<String> projectNames = new HashSet<>();
		for ( int i = 0; i < projectDirectories.length; i++ ) {
			if ( projectDirectories[i].exists() && projectDirectories[i].isDirectory() ) {
				projectNames.add( projectDirectories[i].getName() );
			}
		}
		this.projectNames = Collections.unmodifiableSet( projectNames );
	}

	public Set<String> getProjectNames() {
		return projectNames;
	}

	private File extractTmpDir(File markerFile) {
		final Properties properties = new Properties();
		try ( FileInputStream inputStream = new FileInputStream( markerFile ) ) {
			properties.load( inputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to read `" + MARKER_FILE_NAME + "`" );
		}

		final String tmpDirPath = properties.getProperty( "tmp-dir" );
		final File tmpDir = new File( tmpDirPath );
		tmpDir.mkdirs();
		return tmpDir;
	}

	private URL locateMarker() {
		final URL withoutSlash = ProjectScope.class.getResource( MARKER_FILE_NAME );
		if ( withoutSlash != null ) {
			return withoutSlash;
		}

		final URL withSlash = ProjectScope.class.getResource( "/" + MARKER_FILE_NAME );
		if ( withSlash != null ) {
			return withSlash;
		}

		return null;
	}

	private static void verifyBaseDir(File baseDirectory) {
		assert baseDirectory.exists();
		assert baseDirectory.isDirectory();
	}

	public ProjectScope getProjectScope(String projectName) {
		assert projectNames.contains( projectName );

		// Locate the project "source" directory
		final File projectSourceDir = new File( projectBaseDir, projectName );

		// make a new directory in the tmpDir for the test
		final File projectDir = new File( new File( tmpDir, TEST_KIT + randomGenerator.nextInt() ), projectName );
		projectDir.mkdirs();

		// Copy the project from the source dir to the temporary, isolated one
		DirectoryCopier.copy( projectSourceDir.toPath(), projectDir.toPath() );

		return new ProjectScope( projectDir );
	}

	void release() {
		if ( tmpDir.exists() ) {
			tmpDir.delete();
		}
	}
}
