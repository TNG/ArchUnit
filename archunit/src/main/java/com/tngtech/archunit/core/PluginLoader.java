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
package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Supplier;
import com.google.common.collect.Ordering;
import com.tngtech.archunit.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;

@Internal
public class PluginLoader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final Supplier<T> supplier;

    private PluginLoader(Class<T> pluginType, Map<JavaVersion, String> versionToPlugins, T fallback) {
        supplier = memoize(new PluginSupplier<>(pluginType, versionToPlugins, fallback));
    }

    public static <T> Creator<T> forType(Class<T> pluginType) {
        return new Creator<>(pluginType);
    }

    public T load() {
        return supplier.get();
    }

    private static class PluginSupplier<T> implements Supplier<T> {
        private final Class<T> pluginType;
        private final Map<JavaVersion, String> versionToPlugins;
        private final T fallback;

        private PluginSupplier(Class<T> pluginType, Map<JavaVersion, String> versionToPlugins, T fallback) {
            this.pluginType = pluginType;
            this.versionToPlugins = versionToPlugins;
            this.fallback = fallback;
        }

        @Override
        public T get() {
            String currentVersion = System.getProperty("java.version");
            LOG.info("Detected Java version {}", currentVersion);

            for (JavaVersion version : JavaVersion.sortFromNewestToOldest(versionToPlugins.keySet())) {
                if (version.isLessOrEqualThan(currentVersion)) {
                    String className = versionToPlugins.get(version);
                    LOG.debug("Current Java version is compatible to {} => Loading Plugin {}", version, className);
                    return create(className);
                }
            }
            LOG.debug("Using legacy import plugin");
            return fallback;
        }

        @SuppressWarnings("unchecked") // We explicitly check that the loaded class is assignable to T (i.e. pluginType)
        @MayResolveTypesViaReflection(reason = "This only resolves ArchUnit's own classes, it's not dependent on any project classpath")
        private T create(String className) {
            try {
                Class<?> clazz = Class.forName(className);
                checkCompatibility(className, clazz);
                return (T) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new PluginLoadingFailedException(e, "Couldn't load plugin of type %s", className);
            }
        }

        private void checkCompatibility(String className, Class<?> clazz) {
            if (!pluginType.isAssignableFrom(clazz)) {
                throw new PluginLoadingFailedException("Class %s must implement %s", className, pluginType.getName());
            }
        }
    }

    @Internal
    public static class Creator<T> {
        private final Map<JavaVersion, String> versionToPlugins = new HashMap<>();
        private final Class<T> pluginType;

        private Creator(Class<T> pluginType) {
            this.pluginType = pluginType;
        }

        private Creator<T> addPlugin(JavaVersion version, String pluginClassName) {
            versionToPlugins.put(checkNotNull(version), checkNotNull(pluginClassName));
            return this;
        }

        public PluginLoader<T> fallback(T fallback) {
            return new PluginLoader<>(pluginType, versionToPlugins, fallback);
        }

        public PluginEntry ifVersionGreaterOrEqualTo(JavaVersion version) {
            return new PluginEntry(version);
        }

        @Internal
        public class PluginEntry {
            private final PluginLoader.JavaVersion version;

            private PluginEntry(JavaVersion version) {
                this.version = version;
            }

            public Creator<T> load(String pluginClassName) {
                return Creator.this.addPlugin(version, pluginClassName);
            }
        }
    }

    @Internal
    public enum JavaVersion {
        JAVA_9 {
            @Override
            public boolean isLessOrEqualThan(String version) {
                // The new versioning scheme starting with JDK 9 is 9.x, before it was sth. like 1.8.0_122
                return parseFirstGroupOfJavaVersion(version) >= 9;
            }

            private int parseFirstGroupOfJavaVersion(String javaVersion) {
                Matcher matcher = VERSION_PATTERN.matcher(javaVersion);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Can't parse Java version " + javaVersion);
                }
                return Integer.parseInt(matcher.group(1));
            }
        };

        private static final Ordering<JavaVersion> FROM_NEWEST_TO_OLDEST_ORDERING = Ordering.explicit(JAVA_9);
        private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+).*");

        public abstract boolean isLessOrEqualThan(String version);

        static List<JavaVersion> sortFromNewestToOldest(Set<JavaVersion> javaVersions) {
            return FROM_NEWEST_TO_OLDEST_ORDERING.sortedCopy(javaVersions);
        }
    }

    static class PluginLoadingFailedException extends RuntimeException {
        PluginLoadingFailedException(String message, Object... args) {
            super(String.format(message, args));
        }

        PluginLoadingFailedException(Exception e, String message, Object... args) {
            super(String.format(message, args), e);
        }
    }
}
