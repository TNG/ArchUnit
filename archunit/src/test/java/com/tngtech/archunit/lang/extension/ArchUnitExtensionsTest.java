package com.tngtech.archunit.lang.extension;

import java.io.IOException;
import java.util.Properties;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.lang.extension.examples.TestExtension;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.archunit.testutil.LogTestRule;
import org.apache.logging.log4j.Level;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.testutil.TestUtils.properties;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ArchUnitExtensionsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();
    @Rule
    public final LogTestRule logTestRule = new LogTestRule();

    @Mock
    private EvaluatedRule evaluatedRule;
    @Mock
    private ArchUnitExtensionLoader extensionLoader;

    @InjectMocks
    private ArchUnitExtensions extensions;

    @Test
    public void extensions_are_configured() throws IOException {
        TestExtension extensionOne = new TestExtension("one");
        TestExtension extensionTwo = new TestExtension("two");
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.<ArchUnitExtension>of(extensionOne, extensionTwo));
        ArchConfiguration.get().setExtensionProperties(extensionOne.getUniqueIdentifier(),
                properties("enabled", "true", "one", "valueOne"));
        ArchConfiguration.get().setExtensionProperties(extensionTwo.getUniqueIdentifier(),
                properties("enabled", "true", "two", "valueTwo"));

        extensions.dispatch(evaluatedRule);

        assertThat(extensionOne.getConfiguredProperties())
                .hasSize(2).containsEntry("one", "valueOne");
        assertThat(extensionTwo.getConfiguredProperties())
                .hasSize(2).containsEntry("two", "valueTwo");
    }

    @Test
    public void evaluated_rules_are_dispatched_after_extension_has_been_configured() {
        TestExtension extension = enabled(new TestExtension("test") {
            @Override
            public void handle(EvaluatedRule evaluatedRule) {
                assertPropertiesAreConfigured();
                super.handle(evaluatedRule);
            }

            private void assertPropertiesAreConfigured() {
                checkNotNull(getConfiguredProperties());
            }
        });
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.<ArchUnitExtension>of(extension));

        extensions.dispatch(evaluatedRule);

        assertThat(extension.getEvaluatedRule()).isEqualTo(evaluatedRule);
    }

    @Test
    public void evaluated_rules_are_dispatched_to_all_extensions() {
        TestExtension extensionOne = enabled(new TestExtension("one"));
        TestExtension extensionTwo = enabled(new TestExtension("two"));
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.<ArchUnitExtension>of(extensionOne, extensionTwo));

        extensions.dispatch(evaluatedRule);

        assertThat(extensionOne.getEvaluatedRule()).isEqualTo(evaluatedRule);
        assertThat(extensionTwo.getEvaluatedRule()).isEqualTo(evaluatedRule);
    }

    @Test
    public void only_dispatches_to_enabled_extensions() {
        TestExtension extensionOne = newExtensionWithEnabled("one", false);
        TestExtension extensionTwo = newExtensionWithEnabled("two", true);

        when(extensionLoader.getAll()).thenReturn(ImmutableSet.<ArchUnitExtension>of(extensionOne, extensionTwo));

        logTestRule.watch(ArchUnitExtensions.class, Level.DEBUG);

        extensions.dispatch(evaluatedRule);

        assertThat(extensionOne.wasNeverCalled()).as("Extension 'one' was never called").isTrue();
        assertThat(extensionTwo.wasNeverCalled()).as("Extension 'two' was never called").isFalse();
        logTestRule.assertLogMessage(Level.DEBUG,
                "Extension 'one' is disabled, skipping... (to enable this extension, configure extension.one.enabled=true)");
    }

    @Test
    public void exception_during_configuration_of_extension_is_handled() {
        final String expectedExceptionMessage = "Bummer";
        ArchUnitExtension evilExtension = enabled(new TestExtension() {
            @Override
            public void configure(Properties properties) {
                throw new TestException(expectedExceptionMessage);
            }
        });
        evaluateExtensionAndVerifyLog(expectedExceptionMessage, evilExtension);
    }

    @Test
    public void exception_during_evaluation_of_extension_is_handled() {
        final String expectedExceptionMessage = "Bummer";
        ArchUnitExtension evilExtension = enabled(new TestExtension() {
            @Override
            public void handle(EvaluatedRule evaluatedRule) {
                throw new TestException(expectedExceptionMessage);
            }
        });
        evaluateExtensionAndVerifyLog(expectedExceptionMessage, evilExtension);
    }

    private TestExtension enabled(TestExtension extension) {
        ArchConfiguration.get().configureExtension(extension.getUniqueIdentifier()).setProperty("enabled", true);
        return extension;
    }

    private TestExtension newExtensionWithEnabled(String identifier, boolean enabled) {
        TestExtension extensionOne = enabled(new TestExtension(identifier));
        ArchConfiguration.get().configureExtension(identifier).setProperty("enabled", enabled);
        return extensionOne;
    }

    private void evaluateExtensionAndVerifyLog(String expectedExceptionMessage, ArchUnitExtension evilExtension) {
        when(extensionLoader.getAll()).thenReturn(singleton(evilExtension));

        logTestRule.watch(ArchUnitExtensions.class, Level.WARN);

        extensions.dispatch(evaluatedRule);

        logTestRule.assertLogMessage(Level.WARN, evilExtension.getUniqueIdentifier());
        logTestRule.assertException(Level.WARN, TestException.class, expectedExceptionMessage);
    }

    private static class TestException extends RuntimeException {
        TestException(String message) {
            super(message);
        }
    }
}
