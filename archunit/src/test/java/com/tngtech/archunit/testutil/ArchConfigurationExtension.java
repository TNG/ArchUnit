package com.tngtech.archunit.testutil;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.ArchConfiguration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ArchConfigurationExtension implements Extension, BeforeEachCallback, AfterEachCallback {

    public static final String FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME = "archRule.failOnEmptyShould";

    private boolean beforeHasBeenExecuted = false;
    private final List<Runnable> configurationInitializers = new ArrayList<>();

    public void resolveAdditionalDependenciesFromClassPath(boolean enabled) {
        addConfigurationInitializer(() -> ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(enabled));
    }

    public ArchConfigurationExtension setFailOnEmptyShould(boolean failOnEmptyShould) {
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
    public void beforeEach(ExtensionContext context) {
        ArchConfiguration.get().reset();
        for (Runnable initializer : configurationInitializers) {
            initializer.run();
        }
        beforeHasBeenExecuted = true;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ArchConfiguration.get().reset();
    }
}