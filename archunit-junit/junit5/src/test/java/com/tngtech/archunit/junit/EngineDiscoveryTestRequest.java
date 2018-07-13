package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

class EngineDiscoveryTestRequest implements EngineDiscoveryRequest {
    private final List<UniqueId> idsToDiscover = new ArrayList<>();
    private final List<Class<?>> classesToDiscover = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked") // compatibility is explicitly checked by assignableFrom(..)
    public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
        if (UniqueIdSelector.class.isAssignableFrom(selectorType)) {
            return (List<T>) createUniqueIdSelectors(idsToDiscover);
        }
        if (ClassSelector.class.isAssignableFrom(selectorType)) {
            return (List<T>) createClassSelectors(classesToDiscover);
        }
        return emptyList();
    }

    private List<UniqueIdSelector> createUniqueIdSelectors(List<UniqueId> idsToDiscover) {
        return idsToDiscover.stream().map(DiscoverySelectors::selectUniqueId).collect(toList());
    }

    private List<ClassSelector> createClassSelectors(List<Class<?>> classesToDiscover) {
        return classesToDiscover.stream().map(DiscoverySelectors::selectClass).collect(toList());
    }

    @Override
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        return emptyList();
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return new EmptyConfigurationParameters();
    }

    EngineDiscoveryTestRequest withUniqueId(UniqueId id) {
        idsToDiscover.add(id);
        return this;
    }

    EngineDiscoveryTestRequest withClass(Class<?> clazz) {
        classesToDiscover.add(clazz);
        return this;
    }

    private static class EmptyConfigurationParameters implements ConfigurationParameters {
        @Override
        public Optional<String> get(String key) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> getBoolean(String key) {
            return Optional.empty();
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
