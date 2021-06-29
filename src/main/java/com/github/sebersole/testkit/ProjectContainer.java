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
import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_BASE_DIR;
import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_IMPL_PROJ_NAME;
import static com.github.sebersole.testkit.TestKitPlugin.TESTKIT_STAGING_DIR;
import static com.github.sebersole.testkit.TestKitPlugin.TEST_KIT;

/**
 * A manager for the TestKit projects
 */
public class ProjectContainer {
	private final File projectBaseDir;
	private final File projectStagingDir;
	private final String implicitProjectName;

	private final Random randomGenerator = new Random();

	private final Set<String> projectNames;

	public ProjectContainer() {
		final URL markerFileUrl = locateMarker();
		if ( markerFileUrl == null ) {
			throw new IllegalStateException( "Could not locate TestKit project dir marker file (`" + MARKER_FILE_NAME + "`)" );
		}

		final String markerFilePath = markerFileUrl.getFile();
		final File markerFile = new File( markerFilePath );
		final Properties properties = loadProperties( markerFile );

		projectBaseDir = extractBaseDir( properties );
		projectStagingDir = extractStagingDir( properties );

		System.out.printf( "TestKit project directory : %s\n", projectBaseDir.getAbsolutePath() );

		String implicitProjectName = extractImplicitProjectName( properties );
		final File[] projectDirectories = projectBaseDir.listFiles();
		assert projectDirectories != null;
		final Set<String> projectNames = new HashSet<>();
		for ( int i = 0; i < projectDirectories.length; i++ ) {
			if ( projectDirectories[i].exists() && projectDirectories[i].isDirectory() ) {
				projectNames.add( projectDirectories[i].getName() );
			}
		}
		this.projectNames = Collections.unmodifiableSet( projectNames );
		if ( implicitProjectName == null && projectNames.size() == 1 ) {
			implicitProjectName = projectNames.iterator().next();
		}
		this.implicitProjectName = implicitProjectName;
	}

	public File getProjectBaseDir() {
		return projectBaseDir;
	}

	public File getProjectStagingDir() {
		return projectStagingDir;
	}

	public String getImplicitProjectName() {
		return implicitProjectName;
	}

	public Set<String> getProjectNames() {
		return projectNames;
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

	private Properties loadProperties(File markerFile) {
		final Properties properties = new Properties();
		try ( FileInputStream inputStream = new FileInputStream( markerFile ) ) {
			properties.load( inputStream );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to read `" + MARKER_FILE_NAME + "`" );
		}
		return properties;
	}

	private static File extractBaseDir(Properties properties) {
		final String baseDirPath = properties.getProperty( TESTKIT_BASE_DIR );
		if ( baseDirPath == null ) {
			throw new IllegalStateException( "Could not find `" + TESTKIT_BASE_DIR + "` in marker file" );
		}

		final File baseDir = new File( baseDirPath );
		if ( ! baseDir.exists() ) {
			throw new IllegalStateException( "TestKit base directory (`" + baseDirPath + "`) did not exist" );
		}
		if ( ! baseDir.isDirectory() ) {
			throw new IllegalStateException( "TestKit base directory (`" + baseDirPath + "`) is not a directory" );
		}

		assert baseDir.exists();
		assert baseDir.isDirectory();

		return baseDir;
	}

	private File extractStagingDir(Properties properties) {
		final String stagingDirPath = properties.getProperty( TESTKIT_STAGING_DIR );
		if ( stagingDirPath == null ) {
			throw new IllegalStateException( "Could not find `" + TESTKIT_STAGING_DIR + "` in marker file" );
		}

		final File stagingDir = new File( stagingDirPath );
		stagingDir.mkdirs();

		assert stagingDir.isDirectory();

		return stagingDir;
	}

	private String extractImplicitProjectName(Properties properties) {
		return properties.getProperty( TESTKIT_IMPL_PROJ_NAME );
	}

	public ProjectScope getProjectScope(String projectName) {
		assert projectNames.contains( projectName );

		// Locate the project "source" directory
		final File projectSourceDir = new File( projectBaseDir, projectName );

		// make a new directory in the tmpDir for the test
		final File projectDir = new File( new File( projectStagingDir, TEST_KIT + randomGenerator.nextInt() ), projectName );
		projectDir.mkdirs();


		// Copy the project from the source dir to the temporary, isolated one
		DirectoryCopier.copy( projectSourceDir.toPath(), projectDir.toPath() );

		return new ProjectScope( projectDir );
	}

	void release() {
		if ( projectStagingDir.exists() ) {
			projectStagingDir.delete();
		}
	}
}
