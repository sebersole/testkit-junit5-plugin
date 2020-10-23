package com.github.sebersole.testkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the TestKit project to use.  For TestKit setups with just
 * a single TestKit project, this annotation is optional - the single
 * project is acts as an implicit `@Project`
 *
 * @see ProjectScope
 */
@Inherited
@Target( { ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER } )
@Retention( RetentionPolicy.RUNTIME )
@TestKit
public @interface Project {
	/**
	 * The name of the TestKit project
	 */
	String value();
}
