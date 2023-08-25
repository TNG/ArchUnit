package com.tngtech.archunit.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.rules.ExternalResource;

public class ArchConfigurationRule extends ExternalResource {
    public static final String FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME = "archRule.failOnEmptyShould";

    private boolean beforeHasBeenExecuted = false;
    private final List<Runnable> configurationInitializers = new ArrayList<>();

    public ArchConfigurationRule resolveAdditionalDependenciesFromClassPath(boolean enabled) {
        addConfigurationInitializer(() -> ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(enabled));
        return this;
    }

    public ArchConfigurationRule setFailOnEmptyShould(boolean failOnEmptyShould) {
        addConfigurationInitializer(() -> ArchConfiguration.get().setProperty(FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME, String.valueOf(failOnEmptyShould)));
        return this;
    }

    private void addConfigurationInitializer(Runnable initializer) {
        if (beforeHasBeenExecuted) {
            initializer.run();
        } else {
            configurationInitializers.add(initializer);
        }
    }

    @Override
    protected void before() {
        ArchConfiguration.get().reset();
        for (Runnable initializer : configurationInitializers) {
            initializer.run();
        }
        beforeHasBeenExecuted = true;
    }

    @Override
    protected void after() {
        ArchConfiguration.get().reset();
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
