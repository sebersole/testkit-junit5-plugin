package com.github.sebersole.testkit;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit5 extension for TesKit-based testing
 */
public class TestKitJunitExtension implements ParameterResolver, AfterEachCallback, BeforeAllCallback, AfterAllCallback {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		if ( ! extensionContext.getTestMethod().isPresent() ) {
			return false;
		}

		final ProjectContainer projectContainer = resolveProjectContainer( extensionContext );
		final Project projectSelectionAnn = findTestKitProject( projectContainer, parameterContext, extensionContext );
		final Class<?> parameterJavaType = parameterContext.getParameter().getType();

		return projectSelectionAnn != null && parameterJavaType.isAssignableFrom( ProjectScope.class );
	}

	private Project findTestKitProject(
			ProjectContainer projectContainer,
			ParameterContext parameterContext,
			ExtensionContext extensionContext) {
		final Project paramAnnotation = parameterContext.findAnnotation( Project.class ).orElse( null );
		if ( paramAnnotation != null ) {
			return paramAnnotation;
		}

		if ( extensionContext.getRequiredTestMethod().isAnnotationPresent( Project.class ) ) {
			return extensionContext.getRequiredTestMethod().getDeclaredAnnotation( Project.class );
		}

		if ( extensionContext.getRequiredTestClass().isAnnotationPresent( Project.class ) ) {
			return extensionContext.getRequiredTestClass().getDeclaredAnnotation( Project.class );
		}

		if ( projectContainer.getProjectNames().size() == 1 ) {
			final String singleProjectName = projectContainer.getProjectNames().iterator().next();
			return new Project() {
				@Override
				public String value() {
					return singleProjectName;
				}

				@Override
				public Class<? extends Annotation> annotationType() {
					return Project.class;
				}
			};
		}

		return null;
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		if ( ! extensionContext.getTestMethod().isPresent() ) {
			throw new IllegalStateException( "Expecting test method context" );
		}

		final ProjectContainer projectContainer = resolveProjectContainer( extensionContext );

		final Project projectSelectionAnn = findTestKitProject( projectContainer, parameterContext, extensionContext );
		// we already checked while checking support
		assert projectSelectionAnn != null;
		final String projectName = projectSelectionAnn.value();

		final ProjectScope projectScope = projectContainer.getProjectScope( projectName );

		final ExtensionContext.Store store = extensionContext.getStore( ExtensionContext.Namespace.create( TestKitPlugin.TEST_KIT ) );
		store.put( extensionContext.getRequiredTestMethod(), projectScope );

		return projectScope;
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) {
		final ExtensionContext.Store store = extensionContext.getStore( ExtensionContext.Namespace.create( TestKitPlugin.TEST_KIT ) );
		final ProjectScope scope = (ProjectScope) store.remove( extensionContext.getRequiredTestMethod() );
		if ( scope != null ) {
			scope.release();
		}
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) {
		resolveProjectContainer( extensionContext );
	}

	private ProjectContainer resolveProjectContainer(ExtensionContext extensionContext) {
		final ExtensionContext.Store globalStore = extensionContext.getRoot().getStore( ExtensionContext.Namespace.GLOBAL );
		return globalStore.getOrComputeIfAbsent( ProjectContainer.class );
	}

	@Override
	public void afterAll(ExtensionContext extensionContext) {
		final ExtensionContext.Store globalStore = extensionContext.getRoot().getStore( ExtensionContext.Namespace.GLOBAL );
		final ProjectContainer removed = (ProjectContainer) globalStore.remove( ProjectContainer.class );
		if ( removed != null ) {
			removed.release();
		}
	}
}
