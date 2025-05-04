package com.tngtech.archunit.testutil;

import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemPropertiesExtension implements BeforeEachCallback, AfterEachCallback {
    private Properties properties;

    @Override
    public void beforeEach(ExtensionContext context) {
        properties = (Properties) System.getProperties().clone();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.setProperties(properties);
    }
}
