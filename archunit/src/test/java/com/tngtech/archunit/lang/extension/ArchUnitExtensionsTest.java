package com.tngtech.archunit.lang.extension;

import java.util.Properties;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.lang.extension.examples.TestExtension;
import com.tngtech.archunit.testutil.ArchConfigurationExtension;
import com.tngtech.archunit.testutil.LogTestExtension;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.testutil.TestUtils.properties;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArchUnitExtensionsTest {
    @RegisterExtension
    final ArchConfigurationExtension configuration = new ArchConfigurationExtension();
    @RegisterExtension
    final LogTestExtension logTest = new LogTestExtension();

    @Mock
    private EvaluatedRule evaluatedRule;
    @Mock
    private ArchUnitExtensionLoader extensionLoader;

    @InjectMocks
    private ArchUnitExtensions extensions;

    @Test
    public void extensions_are_configured() {
        TestExtension extensionOne = new TestExtension("one");
        TestExtension extensionTwo = new TestExtension("two");
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.of(extensionOne, extensionTwo));
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
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.of(extension));

        extensions.dispatch(evaluatedRule);

        assertThat(extension.getEvaluatedRule()).isEqualTo(evaluatedRule);
    }

    @Test
    public void evaluated_rules_are_dispatched_to_all_extensions() {
        TestExtension extensionOne = enabled(new TestExtension("one"));
        TestExtension extensionTwo = enabled(new TestExtension("two"));
        when(extensionLoader.getAll()).thenReturn(ImmutableSet.of(extensionOne, extensionTwo));

        extensions.dispatch(evaluatedRule);

        assertThat(extensionOne.getEvaluatedRule()).isEqualTo(evaluatedRule);
        assertThat(extensionTwo.getEvaluatedRule()).isEqualTo(evaluatedRule);
    }

    @Test
    public void only_dispatches_to_enabled_extensions() {
        TestExtension extensionOne = newExtensionWithEnabled("one", false);
        TestExtension extensionTwo = newExtensionWithEnabled("two", true);

        when(extensionLoader.getAll()).thenReturn(ImmutableSet.of(extensionOne, extensionTwo));

        logTest.watch(ArchUnitExtensions.class, Level.DEBUG);

        extensions.dispatch(evaluatedRule);

        assertThat(extensionOne.wasNeverCalled()).as("Extension 'one' was never called").isTrue();
        assertThat(extensionTwo.wasNeverCalled()).as("Extension 'two' was never called").isFalse();
        logTest.assertLogMessage(Level.DEBUG,
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

        logTest.watch(ArchUnitExtensions.class, Level.WARN);

        extensions.dispatch(evaluatedRule);

        logTest.assertLogMessage(Level.WARN, evilExtension.getUniqueIdentifier());
        logTest.assertException(Level.WARN, TestException.class, expectedExceptionMessage);
    }

    private static class TestException extends RuntimeException {
        TestException(String message) {
            super(message);
        }
    }
}
