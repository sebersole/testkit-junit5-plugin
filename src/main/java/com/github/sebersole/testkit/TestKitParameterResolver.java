package com.github.sebersole.testkit;

import java.io.File;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Steve Ebersole
 */
public class TestKitParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		if ( parameterContext.getParameter().getType().isAssignableFrom( TestKitBaseScope.class ) ) {
			return true;
		}

		final TestKitProject projectSelectionAnn = findTestKitProject( parameterContext, extensionContext );

		if ( projectSelectionAnn != null ) {
			// we support both `TestKitProjectScope` and `File` if `@TestKitProject` is specified
			final Class<?> parameterJavaType = parameterContext.getParameter().getType();
			return parameterJavaType.isAssignableFrom( TestKitProjectScope.class )
					|| parameterJavaType.isAssignableFrom( File.class );
		}

		return false;
	}

	private TestKitProject findTestKitProject(ParameterContext parameterContext, ExtensionContext extensionContext) {
		if ( extensionContext.getRequiredTestMethod().isAnnotationPresent( TestKitProject.class ) ) {
			return extensionContext.getRequiredTestMethod().getDeclaredAnnotation( TestKitProject.class );
		}

		return parameterContext.findAnnotation( TestKitProject.class ).orElse( null );
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		final ExtensionContext.Store store = extensionContext.getRoot().getStore( ExtensionContext.Namespace.GLOBAL );
		final TestKitBaseScope baseScope = store.getOrComputeIfAbsent( TestKitBaseScope.class );

		final Class<?> parameterJavaType = parameterContext.getParameter().getType();

		if ( parameterJavaType.isAssignableFrom( TestKitBaseScope.class ) ) {
			return baseScope;
		}

		final TestKitProject projectSelectionAnn = findTestKitProject( parameterContext, extensionContext );
		final String projectName = projectSelectionAnn.value();

		final TestKitProjectScope projectScope = baseScope.getProjectScope( projectName );

		if ( parameterJavaType.isAssignableFrom( File.class ) ) {
			return projectScope.getProjectBaseDirectory();
		}

		assert parameterJavaType.isAssignableFrom( TestKitProjectScope.class );

		return projectScope;
	}
}
