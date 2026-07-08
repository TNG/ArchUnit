package com.tngtech.archunit.testutils;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ContextClassLoaderExtension implements BeforeEachCallback, AfterEachCallback {
    private ClassLoader contextClassLoader;

    @Override
    public void beforeEach(ExtensionContext context) {
        contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
}
