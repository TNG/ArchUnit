package com.tngtech.archunit.junit.internal.testutil;

import java.util.Properties;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemPropertiesExtension implements BeforeEachCallback, AfterEachCallback {
    private Properties properties;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        properties = (Properties) System.getProperties().clone();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        System.setProperties(properties);
    }
}
