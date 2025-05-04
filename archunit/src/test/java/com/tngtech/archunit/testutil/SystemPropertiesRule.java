package com.tngtech.archunit.testutil;

import java.util.Properties;

import org.junit.rules.ExternalResource;

/**
 * @deprecated use JUnit 5 and {@link SystemPropertiesExtension} instead
 */
public class SystemPropertiesRule extends ExternalResource {
    private Properties properties;

    @Override
    public void before() {
        properties = (Properties) System.getProperties().clone();
    }

    @Override
    public void after() {
        System.setProperties(properties);
    }
}
