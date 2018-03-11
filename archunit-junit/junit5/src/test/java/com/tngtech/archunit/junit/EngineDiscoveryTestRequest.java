package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.ClassSelector;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class EngineDiscoveryTestRequest implements EngineDiscoveryRequest {
    private final List<Class<?>> classesToDiscover = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked") // compatibility is explicitly checked by assignableFrom(..)
    public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
        if (ClassSelector.class.isAssignableFrom(selectorType)) {
            return (List<T>) createClassSelectors(classesToDiscover);
        }
        return Collections.emptyList();
    }

    private List<ClassSelector> createClassSelectors(List<Class<?>> classesToDiscover) {
        List<ClassSelector> result = new ArrayList<>();
        for (Class<?> clazz : classesToDiscover) {
            result.add(selectClass(clazz));
        }
        return result;
    }

    @Override
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        return Collections.emptyList();
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return new EmptyConfigurationParameters();
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
