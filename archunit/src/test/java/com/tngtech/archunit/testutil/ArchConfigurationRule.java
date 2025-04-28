package com.tngtech.archunit.testutil;

import java.util.concurrent.Callable;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

/**
 * @deprecated use JUnit 5 and {@link ArchConfigurationExtension} instead
 */
@Deprecated
public class ArchConfigurationRule extends ExternalResource {
    private final ArchConfigurationExtension configuration = new ArchConfigurationExtension();

    public ArchConfigurationRule resolveAdditionalDependenciesFromClassPath(boolean enabled) {
        configuration.resolveAdditionalDependenciesFromClassPath(enabled);
        return this;
    }

    public ArchConfigurationRule setFailOnEmptyShould(boolean failOnEmptyShould) {
        configuration.setFailOnEmptyShould(failOnEmptyShould);
        return this;
    }

    @Override
    protected void before() {
        ExtensionContext unusedContext = null; // good enough for now, and ArchConfigurationRule is deprecated anyways
        configuration.beforeEach(unusedContext);
    }

    @Override
    protected void after() {
        ExtensionContext unusedContext = null; // good enough for now, and ArchConfigurationRule is deprecated anyways
        configuration.afterEach(unusedContext);
    }

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
}
