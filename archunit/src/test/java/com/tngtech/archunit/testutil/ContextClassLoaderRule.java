package com.tngtech.archunit.testutil;

import org.junit.rules.ExternalResource;

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
