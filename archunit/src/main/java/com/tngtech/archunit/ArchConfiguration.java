package com.tngtech.archunit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

public class ArchConfiguration {
    static final String ARCHUNIT_PROPERTIES_RESOURCE_NAME = "/archunit.properties";
    static final String RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH = "resolveMissingDependenciesFromClassPath";
    static final String ENABLE_MD5_IN_CLASS_SOURCES = "enableMd5InClassSources";

    private static final Map<String, String> PROPERTY_DEFAULTS = ImmutableMap.of(
            RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, "" + false,
            ENABLE_MD5_IN_CLASS_SOURCES, "" + false
    );

    private static final Supplier<ArchConfiguration> INSTANCE = Suppliers.memoize(new Supplier<ArchConfiguration>() {
        @Override
        public ArchConfiguration get() {
            return new ArchConfiguration();
        }
    });

    public static ArchConfiguration get() {
        return INSTANCE.get();
    }

    private final String propertiesResourceName;
    private boolean resolveMissingDependenciesFromClassPath;
    private boolean enableMd5InClassSources;

    private ArchConfiguration() {
        this(ARCHUNIT_PROPERTIES_RESOURCE_NAME);
    }

    private ArchConfiguration(String propertiesResourceName) {
        this.propertiesResourceName = propertiesResourceName;
        readProperties(propertiesResourceName);
    }

    private void readProperties(String propertiesResourceName) {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream(propertiesResourceName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignore) {
        }
        set(properties);
    }

    public void reset() {
        readProperties(propertiesResourceName);
    }

    public boolean resolveMissingDependenciesFromClassPath() {
        return resolveMissingDependenciesFromClassPath;
    }

    public void setResolveMissingDependenciesFromClassPath(boolean newValue) {
        resolveMissingDependenciesFromClassPath = newValue;
    }

    private void set(Properties properties) {
        resolveMissingDependenciesFromClassPath = Boolean.valueOf(
                propertyOrDefault(properties, RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH));
        enableMd5InClassSources = Boolean.valueOf(
                propertyOrDefault(properties, ENABLE_MD5_IN_CLASS_SOURCES));
    }

    public boolean md5InClassSourcesEnabled() {
        return enableMd5InClassSources;
    }

    public void setMd5InClassSourcesEnabled(boolean enabled) {
        this.enableMd5InClassSources = enabled;
    }

    private String propertyOrDefault(Properties properties, String propertyName) {
        return properties.getProperty(propertyName, PROPERTY_DEFAULTS.get(propertyName));
    }
}
