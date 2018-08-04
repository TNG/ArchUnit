package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;

import static com.tngtech.archunit.junit.FieldSelector.selectField;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

class EngineDiscoveryTestRequest implements EngineDiscoveryRequest {
    private final List<URI> classpathRootsToDiscover = new ArrayList<>();
    private final List<String> packagesToDiscover = new ArrayList<>();
    private final List<Class<?>> classesToDiscover = new ArrayList<>();
    private final List<Method> methodsToDiscover = new ArrayList<>();
    private final List<Field> fieldsToDiscover = new ArrayList<>();
    private final List<UniqueId> idsToDiscover = new ArrayList<>();

    private final List<ClassNameFilter> classNameFilters = new ArrayList<>();
    private final List<PackageNameFilter> packageNameFilters = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked") // compatibility is explicitly checked
    public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
        if (ClasspathRootSelector.class.equals(selectorType)) {
            return (List<T>) createClasspathRootSelectors(classpathRootsToDiscover);
        }
        if (PackageSelector.class.equals(selectorType)) {
            return (List<T>) createPackageSelectors(packagesToDiscover);
        }
        if (ClassSelector.class.equals(selectorType)) {
            return (List<T>) createClassSelectors(classesToDiscover);
        }
        if (MethodSelector.class.equals(selectorType)) {
            return (List<T>) createMethodSelectors(methodsToDiscover);
        }
        if (FieldSelector.class.equals(selectorType)) {
            return (List<T>) createFieldSelectors(fieldsToDiscover);
        }
        if (UniqueIdSelector.class.equals(selectorType)) {
            return (List<T>) createUniqueIdSelectors(idsToDiscover);
        }
        return emptyList();
    }

    private List<ClasspathRootSelector> createClasspathRootSelectors(List<URI> classpathRootsToDiscover) {
        return DiscoverySelectors.selectClasspathRoots(classpathRootsToDiscover.stream().map(Paths::get).collect(toSet()));
    }

    private List<PackageSelector> createPackageSelectors(List<String> packagesToDiscover) {
        return packagesToDiscover.stream().map(DiscoverySelectors::selectPackage).collect(toList());
    }

    private List<ClassSelector> createClassSelectors(List<Class<?>> classesToDiscover) {
        return classesToDiscover.stream().map(DiscoverySelectors::selectClass).collect(toList());
    }

    private List<MethodSelector> createMethodSelectors(List<Method> methodsToDiscover) {
        return methodsToDiscover.stream().map(m -> selectMethod(m.getDeclaringClass(), m)).collect(toList());
    }

    private List<FieldSelector> createFieldSelectors(List<Field> fieldsToDiscover) {
        return fieldsToDiscover.stream().map(f -> selectField(f.getDeclaringClass(), f)).collect(toList());
    }

    private List<UniqueIdSelector> createUniqueIdSelectors(List<UniqueId> idsToDiscover) {
        return idsToDiscover.stream().map(DiscoverySelectors::selectUniqueId).collect(toList());
    }

    @Override
    @SuppressWarnings("unchecked") // compatibility is explicitly checked
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        if (ClassNameFilter.class.equals(filterType)) {
            return (List<T>) classNameFilters;
        }
        if (PackageNameFilter.class.equals(filterType)) {
            return (List<T>) packageNameFilters;
        }
        return emptyList();
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return new EmptyConfigurationParameters();
    }

    EngineDiscoveryTestRequest withClasspathRoot(URI uri) {
        classpathRootsToDiscover.add(uri);
        return this;
    }

    EngineDiscoveryTestRequest withPackage(String pkg) {
        packagesToDiscover.add(pkg);
        return this;
    }

    EngineDiscoveryTestRequest withClass(Class<?> clazz) {
        classesToDiscover.add(clazz);
        return this;
    }

    EngineDiscoveryTestRequest withUniqueId(UniqueId id) {
        idsToDiscover.add(id);
        return this;
    }

    EngineDiscoveryTestRequest withClassNameFilter(ClassNameFilter filter) {
        classNameFilters.add(filter);
        return this;
    }

    EngineDiscoveryTestRequest withPackageNameFilter(PackageNameFilter packageNameFilter) {
        packageNameFilters.add(packageNameFilter);
        return this;
    }

    EngineDiscoveryTestRequest withMethod(Class<?> clazz, String methodName) {
        try {
            methodsToDiscover.add(clazz.getDeclaredMethod(methodName, JavaClasses.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    EngineDiscoveryTestRequest withField(Class<?> clazz, String fieldName) {
        try {
            fieldsToDiscover.add(clazz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
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
