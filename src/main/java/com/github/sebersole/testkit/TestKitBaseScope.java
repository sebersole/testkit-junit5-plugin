package com.github.sebersole.testkit;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class TestKitBaseScope {
	private final File baseDirectory;
	private final Map<String, TestKitProjectScope> projectScopes;

	public TestKitBaseScope() {
		final URL marker = locateMarker();
		if ( marker == null ) {
			throw new IllegalStateException( "Could not locate TestKit project dir marker file (`" + TestKitTask.MARKER_FILE_NAME + "`)" );
		}
		this.baseDirectory = new File( marker.getFile() ).getParentFile();
		verifyBaseDir( baseDirectory );

		projectScopes = new HashMap<>();
	}

	private URL locateMarker() {
		final URL withoutSlash = TestKitProjectScope.class.getResource( TestKitTask.MARKER_FILE_NAME );
		if ( withoutSlash != null ) {
			return withoutSlash;
		}

		final URL withSlash = TestKitProjectScope.class.getResource( "/" + TestKitTask.MARKER_FILE_NAME );
		if ( withSlash != null ) {
			return withSlash;
		}

		return null;
	}

	private static void verifyBaseDir(File baseDirectory) {
		assert baseDirectory.exists();
		assert baseDirectory.isDirectory();
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}

	public TestKitProjectScope getProjectScope(String projectName) {
		return projectScopes.computeIfAbsent(
				projectName,
				s -> new TestKitProjectScope( this, projectName )
		);
	}
}
