package com.github.sebersole.testkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simple marker interface for applying the proper JUnit extensions.  Tests can
 * simply apply the extension directly if they prefer
 *
 * @see Project
 * @see ProjectContainer
 */
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@ExtendWith( TestKitJunitExtension.class )
public @interface TestKit {
}
