/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class ArchConfiguration {
    @Internal // {@value ...} doesn't work on non public constants outside of the package
    public static final String ARCHUNIT_PROPERTIES_RESOURCE_NAME = "/archunit.properties";
    static final String RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH = "resolveMissingDependenciesFromClassPath";
    static final String CLASS_RESOLVER = "classResolver";
    static final String CLASS_RESOLVER_ARGS = "classResolver.args";
    @Internal
    public static final String ENABLE_MD5_IN_CLASS_SOURCES = "enableMd5InClassSources";

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

    @PublicAPI(usage = ACCESS)
    public static ArchConfiguration get() {
        return INSTANCE.get();
    }

    private final String propertiesResourceName;
    private boolean resolveMissingDependenciesFromClassPath;
    private Optional<String> classResolver = Optional.absent();
    private List<String> classResolverArguments = Collections.emptyList();
    private boolean enableMd5InClassSources;

    private final Map<String, Properties> extensionProperties = new ConcurrentHashMap<>();

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

    @PublicAPI(usage = ACCESS)
    public void reset() {
        readProperties(propertiesResourceName);
    }

    @PublicAPI(usage = ACCESS)
    public boolean resolveMissingDependenciesFromClassPath() {
        return resolveMissingDependenciesFromClassPath;
    }

    @PublicAPI(usage = ACCESS)
    public void setResolveMissingDependenciesFromClassPath(boolean newValue) {
        resolveMissingDependenciesFromClassPath = newValue;
    }

    private void set(Properties properties) {
        resolveMissingDependenciesFromClassPath = Boolean.valueOf(
                propertyOrDefault(properties, RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH));
        classResolver = Optional.fromNullable(properties.getProperty(CLASS_RESOLVER));
        classResolverArguments = Splitter.on(",").trimResults().omitEmptyStrings()
                .splitToList(properties.getProperty(CLASS_RESOLVER_ARGS, ""));
        enableMd5InClassSources = Boolean.valueOf(
                propertyOrDefault(properties, ENABLE_MD5_IN_CLASS_SOURCES));
    }

    @PublicAPI(usage = ACCESS)
    public boolean md5InClassSourcesEnabled() {
        return enableMd5InClassSources;
    }

    @PublicAPI(usage = ACCESS)
    public void setMd5InClassSourcesEnabled(boolean enabled) {
        this.enableMd5InClassSources = enabled;
    }

    @PublicAPI(usage = ACCESS)
    public Optional<String> getClassResolver() {
        return classResolver;
    }

    @PublicAPI(usage = ACCESS)
    public void setClassResolver(Class<? extends ClassResolver> classResolver) {
        this.classResolver = Optional.of(classResolver.getName());
    }

    @PublicAPI(usage = ACCESS)
    public void unsetClassResolver() {
        this.classResolver = Optional.absent();
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getClassResolverArguments() {
        return classResolverArguments;
    }

    @PublicAPI(usage = ACCESS)
    public void setClassResolverArguments(String... args) {
        classResolverArguments = ImmutableList.copyOf(args);
    }

    @PublicAPI(usage = ACCESS)
    public void setExtensionProperties(String extensionIdentifier, Properties properties) {
        extensionProperties.put(extensionIdentifier, properties);
    }

    @PublicAPI(usage = ACCESS)
    public Properties getExtensionProperties(String extensionIdentifier) {
        return extensionProperties.containsKey(extensionIdentifier) ?
                copy(extensionProperties.get(extensionIdentifier)) :
                new Properties();
    }

    private Properties copy(Properties properties) {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    private String propertyOrDefault(Properties properties, String propertyName) {
        return properties.getProperty(propertyName, PROPERTY_DEFAULTS.get(propertyName));
    }
}
