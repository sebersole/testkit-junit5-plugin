package com.github.sebersole.testkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows selecting a specific project by name for injection
 *
 * @see TestKitProjectScope
 */
@Inherited
@Target( { ElementType.TYPE, ElementType.METHOD } )
@Retention( RetentionPolicy.RUNTIME )
@TestKit
public @interface TestKitProject {
	/**
	 * The name of the TestKit project
	 */
	String value();
}
