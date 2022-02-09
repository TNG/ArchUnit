package com.tngtech.archunit.testutil;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.rules.ExternalResource;

import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;

public class ArchConfigurationRule extends ExternalResource {
    private boolean resolveMissingDependenciesFromClassPath = ArchConfiguration.get().resolveMissingDependenciesFromClassPath();

    public static <T> T resetConfigurationAround(Callable<T> callable) {
        ArchConfiguration.get().reset();
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    public ArchConfigurationRule resolveAdditionalDependenciesFromClassPath(boolean enabled) {
        resolveMissingDependenciesFromClassPath = enabled;
        return this;
    }

    public void removeProperty(String propertyName) {
        try {
            Object properties = accessible(field(ArchConfiguration.class, "properties")).get(ArchConfiguration.get());
            accessible(method(properties.getClass(), "remove", String.class)).invoke(properties, propertyName);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends AccessibleObject> T accessible(T member) {
        member.setAccessible(true);
        return member;
    }

    @Override
    protected void before() {
        ArchConfiguration.get().reset();
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(resolveMissingDependenciesFromClassPath);
    }

    @Override
    protected void after() {
        ArchConfiguration.get().reset();
    }
}
