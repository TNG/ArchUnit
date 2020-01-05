/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;

/**
 * Allows access to configured properties in {@value ARCHUNIT_PROPERTIES_RESOURCE_NAME}.
 */
public final class ArchConfiguration {
    @Internal // {@value ...} does not work on non public constants outside of the package
    public static final String ARCHUNIT_PROPERTIES_RESOURCE_NAME = "archunit.properties";
    @Internal // {@value ...} does not work on non public constants outside of the package
    public static final String RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH = "resolveMissingDependenciesFromClassPath";
    static final String CLASS_RESOLVER = "classResolver";
    static final String CLASS_RESOLVER_ARGS = "classResolver.args";
    @Internal
    public static final String ENABLE_MD5_IN_CLASS_SOURCES = "enableMd5InClassSources";
    private static final String EXTENSION_PREFIX = "extension";

    private static final Logger LOG = LoggerFactory.getLogger(ArchConfiguration.class);

    private static final Supplier<ArchConfiguration> INSTANCE = Suppliers.memoize(new Supplier<ArchConfiguration>() {
        @Override
        public ArchConfiguration get() {
            return new ArchConfiguration();
        }
    });

    @PublicAPI(usage = ACCESS)
    public static ArchConfiguration get() {
        return INSTANCE.get();
    }

    private final String propertiesResourceName;
    private final PropertiesOverwritableBySystemProperties properties = new PropertiesOverwritableBySystemProperties();

    private ArchConfiguration() {
        this(ARCHUNIT_PROPERTIES_RESOURCE_NAME);
    }

    private ArchConfiguration(String propertiesResourceName) {
        this.propertiesResourceName = propertiesResourceName;
        readProperties(propertiesResourceName);
    }

    private void readProperties(String propertiesResourceName) {
        properties.clear();

        URL archUnitPropertiesUrl = getCurrentClassLoader(getClass()).getResource(propertiesResourceName);
        if (archUnitPropertiesUrl == null) {
            LOG.debug("No configuration found in classpath at {} => Using default configuration", propertiesResourceName);
            return;
        }

        try (InputStream inputStream = archUnitPropertiesUrl.openStream()) {
            LOG.info("Reading ArchUnit properties from {}", archUnitPropertiesUrl);
            properties.load(inputStream);
        } catch (IOException e) {
            LOG.warn("Error reading ArchUnit properties from " + archUnitPropertiesUrl, e);
        }
    }

    @PublicAPI(usage = ACCESS)
    public void reset() {
        readProperties(propertiesResourceName);
    }

    @PublicAPI(usage = ACCESS)
    public boolean resolveMissingDependenciesFromClassPath() {
        return Boolean.parseBoolean(properties.getProperty(RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH));
    }

    @PublicAPI(usage = ACCESS)
    public void setResolveMissingDependenciesFromClassPath(boolean newValue) {
        properties.setProperty(RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, String.valueOf(newValue));
    }

    @PublicAPI(usage = ACCESS)
    public boolean md5InClassSourcesEnabled() {
        return Boolean.parseBoolean(properties.getProperty(ENABLE_MD5_IN_CLASS_SOURCES));
    }

    @PublicAPI(usage = ACCESS)
    public void setMd5InClassSourcesEnabled(boolean enabled) {
        properties.setProperty(ENABLE_MD5_IN_CLASS_SOURCES, String.valueOf(enabled));
    }

    @PublicAPI(usage = ACCESS)
    public Optional<String> getClassResolver() {
        return Optional.fromNullable(properties.getProperty(CLASS_RESOLVER));
    }

    @PublicAPI(usage = ACCESS)
    public void setClassResolver(Class<? extends ClassResolver> classResolver) {
        properties.setProperty(CLASS_RESOLVER, classResolver.getName());
    }

    @PublicAPI(usage = ACCESS)
    public void unsetClassResolver() {
        properties.remove(CLASS_RESOLVER);
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getClassResolverArguments() {
        return Splitter.on(",").trimResults().omitEmptyStrings()
                .splitToList(properties.getProperty(CLASS_RESOLVER_ARGS, ""));
    }

    @PublicAPI(usage = ACCESS)
    public void setClassResolverArguments(String... args) {
        properties.setProperty(CLASS_RESOLVER_ARGS, Joiner.on(",").join(args));
    }

    @PublicAPI(usage = ACCESS)
    public void setExtensionProperties(String extensionIdentifier, Properties properties) {
        String propertyPrefix = getFullExtensionPropertyPrefix(extensionIdentifier);
        clearPropertiesWithPrefix(propertyPrefix);
        for (String propertyName : properties.stringPropertyNames()) {
            String fullPropertyName = propertyPrefix + "." + propertyName;
            this.properties.setProperty(fullPropertyName, properties.getProperty(propertyName));
        }
    }

    private void clearPropertiesWithPrefix(String propertyPrefix) {
        for (String propertyToRemove : filterNamesWithPrefix(properties.stringPropertyNames(), propertyPrefix)) {
            properties.remove(propertyToRemove);
        }
    }

    @PublicAPI(usage = ACCESS)
    public Properties getExtensionProperties(String extensionIdentifier) {
        String propertyPrefix = getFullExtensionPropertyPrefix(extensionIdentifier);
        return getSubProperties(propertyPrefix);
    }

    private String getFullExtensionPropertyPrefix(String extensionIdentifier) {
        return EXTENSION_PREFIX + "." + extensionIdentifier;
    }

    @PublicAPI(usage = ACCESS)
    public ExtensionProperties configureExtension(String extensionIdentifier) {
        return new ExtensionProperties(extensionIdentifier);
    }

    /**
     * Returns a set of properties where all keys share a common prefix. The prefix is removed from those property names. Example:
     * <pre><code>
     * some.custom.prop1=value1
     * some.custom.prop2=value2
     * unrelated=irrelevant</code></pre>
     * Then {@code getSubProperties("some.custom")} would return the properties
     * <pre><code>
     * prop1=value1
     * prop2=value2</code></pre>
     *
     * @param propertyPrefix A prefix for a set of properties
     * @return All properties with this prefix, where the prefix is removed from the keys.
     */
    @PublicAPI(usage = ACCESS)
    public Properties getSubProperties(String propertyPrefix) {
        return getSubProperties(propertyPrefix, properties.getMergedProperties());
    }

    private static Properties getSubProperties(String propertyPrefix, Properties properties) {
        Properties result = new Properties();
        for (String key : filterNamesWithPrefix(properties.stringPropertyNames(), propertyPrefix)) {
            String extensionPropertyKey = removePrefix(key, propertyPrefix);
            result.put(extensionPropertyKey, properties.getProperty(key));
        }
        return result;
    }

    private static Iterable<String> filterNamesWithPrefix(Iterable<String> propertyNames, String prefix) {
        List<String> result = new ArrayList<>();
        String fullPrefix = prefix + ".";
        for (String propertyName : propertyNames) {
            if (propertyName.startsWith(fullPrefix)) {
                result.add(propertyName);
            }
        }
        return result;
    }

    private static String removePrefix(String string, String prefix) {
        return string.substring(prefix.length() + 1);
    }

    /**
     * @param propertyName Full name of a property
     * @return true, if and only if the property is configured within the global ArchUnit configuration.
     * @see #getProperty(String)
     * @see #setProperty(String, String)
     */
    @PublicAPI(usage = ACCESS)
    public boolean containsProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * @param propertyName Full name of a property
     * @return A property of the global ArchUnit configuration. This method will throw an exception if the property is not set within the configuration.
     * @see #containsProperty(String)
     * @see #setProperty(String, String)
     */
    @PublicAPI(usage = ACCESS)
    public String getProperty(String propertyName) {
        return checkNotNull(properties.getProperty(propertyName), "Property '%s' is not configured", propertyName);
    }

    /**
     * Overwrites a property of the global ArchUnit configuration. Note that this change will persist for the whole life time of this JVM
     * unless overwritten another time.
     *
     * @param propertyName Full name of a property
     * @param value The new value to set. Overwrites any existing property with the same name.
     * @see #containsProperty(String)
     * @see #getProperty(String)
     */
    @PublicAPI(usage = ACCESS)
    public void setProperty(String propertyName, String value) {
        properties.setProperty(propertyName, value);
    }

    private static class PropertiesOverwritableBySystemProperties {
        private static final Properties PROPERTY_DEFAULTS = createProperties(ImmutableMap.of(
                RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, Boolean.TRUE.toString(),
                ENABLE_MD5_IN_CLASS_SOURCES, Boolean.FALSE.toString()
        ));

        private final Properties properties = createProperties(PROPERTY_DEFAULTS);

        void clear() {
            properties.clear();
            properties.putAll(PROPERTY_DEFAULTS);
        }

        void load(InputStream inputStream) throws IOException {
            properties.load(inputStream);
        }

        Set<String> stringPropertyNames() {
            return getMergedProperties().stringPropertyNames();
        }

        boolean containsKey(String propertyName) {
            return getMergedProperties().containsKey(propertyName);
        }

        String getProperty(String propertyName) {
            return getMergedProperties().getProperty(propertyName);
        }

        String getProperty(String propertyName, String defaultValue) {
            return getMergedProperties().getProperty(propertyName, defaultValue);
        }

        void setProperty(String propertyName, String value) {
            properties.setProperty(propertyName, value);
        }

        void remove(String propertyName) {
            properties.remove(propertyName);
        }

        Properties getMergedProperties() {
            Properties result = createProperties(this.properties);
            Properties overwritten = getSubProperties("archunit", System.getProperties());
            if (!overwritten.isEmpty()) {
                LOG.info("Merging properties: The following properties have been overwritten by system properties: {}", overwritten);
            }
            result.putAll(overwritten);
            return result;
        }

        private static Properties createProperties(Map<?, ?> entries) {
            Properties result = new Properties();
            result.putAll(entries);
            return result;
        }
    }

    public final class ExtensionProperties {
        private final String extensionIdentifier;

        private ExtensionProperties(String extensionIdentifier) {
            this.extensionIdentifier = extensionIdentifier;
        }

        @PublicAPI(usage = ACCESS)
        public ExtensionProperties setProperty(String key, Object value) {
            String fullKey = Joiner.on(".").join(EXTENSION_PREFIX, extensionIdentifier, key);
            properties.setProperty(fullKey, String.valueOf(value));
            return this;
        }
    }
}
