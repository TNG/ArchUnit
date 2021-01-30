package com.tngtech.archunit.testutil;

import java.util.concurrent.Callable;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.rules.ExternalResource;

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
