package com.tngtech.archunit.testutil;

import org.junit.rules.ExternalResource;

/**
 * @deprecated use JUnit 5 and {@link ContextClassLoaderExtension} instead
 */
public class ContextClassLoaderRule extends ExternalResource {
    private ClassLoader contextClassLoader;

    @Override
    public void before() {
        contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void after() {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
}
