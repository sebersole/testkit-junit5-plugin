package com.github.sebersole.testkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simple marker interface for applying the proper JUnit extensions.  Tests can
 * simply apply the extensions directly if they prefer
 *
 * @see TestKitProject
 * @see TestKitBaseScope
 */
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@ExtendWith( TestKitParameterResolver.class )
public @interface TestKit {
}
