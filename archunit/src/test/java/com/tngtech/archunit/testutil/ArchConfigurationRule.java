package com.tngtech.archunit.testutil;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.rules.ExternalResource;

public class ArchConfigurationRule extends ExternalResource {
    private boolean resolveMissingDependenciesFromClassPath;

    public ArchConfigurationRule resolveAdditionalDependenciesFromClassPath(boolean enabled) {
        resolveMissingDependenciesFromClassPath = enabled;
        return this;
    }

    @Override
    protected void before() throws Throwable {
        ArchConfiguration.get().reset();
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(resolveMissingDependenciesFromClassPath);
    }

    @Override
    protected void after() {
        ArchConfiguration.get().reset();
    }
}
