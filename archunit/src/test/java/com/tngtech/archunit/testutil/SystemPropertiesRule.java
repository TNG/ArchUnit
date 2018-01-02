package com.tngtech.archunit.testutil;

import java.util.Properties;

import org.junit.rules.ExternalResource;

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
