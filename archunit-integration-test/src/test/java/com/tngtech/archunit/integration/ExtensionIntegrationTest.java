package com.tngtech.archunit.integration;

import java.util.Map;
import java.util.Objects;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.exampletest.extension.EvaluatedRuleEvent;
import com.tngtech.archunit.exampletest.extension.ExampleExtension;
import com.tngtech.archunit.lang.ArchRule;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ExtensionIntegrationTest {
    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingCodingRules.class);

    @BeforeEach
    void setUp() {
        ExampleExtension.reset();
    }

    @AfterEach
    void tearDown() {
        ExampleExtension.reset();
        ArchConfiguration.get().configureExtension(ExampleExtension.UNIQUE_IDENTIFIER)
                .setProperty("enabled", false);
    }

    @Test
    void evaluation_results_are_dispatched_to_extensions() {
        ArchConfiguration.get().configureExtension(ExampleExtension.UNIQUE_IDENTIFIER)
                .setProperty("enabled", true);

        ArchRule rule = noClasses().should().haveFullyQualifiedName(ClassViolatingCodingRules.class.getName());
        checkRuleAndIgnoreFailure(classes, rule);

        assertThat(ExampleExtension.getConfigurationEvents()).hasSize(1);
        assertThat(ExampleExtension.getConfigurationEvents())
                .extracting("properties").are(containingEntry("example-prop", "exampleValue"));

        EvaluatedRuleEvent event = getOnlyElement(ExampleExtension.getEvaluatedRuleEvents());
        assertThat(event.contains(rule)).as("Rule was passed").isTrue();
        assertThat(event.contains(classes)).as("Classes were passed").isTrue();
        assertThat(event.hasViolationFor(ClassViolatingCodingRules.class))
                .as("Has violation for " + ClassViolatingCodingRules.class.getSimpleName()).isTrue();
    }

    @Test
    void evaluation_results_are_only_dispatched_to_enabled_extensions() {
        ArchConfiguration.get().configureExtension(ExampleExtension.UNIQUE_IDENTIFIER)
                .setProperty("enabled", false);

        ArchRule rule = noClasses().should().haveFullyQualifiedName(ClassViolatingCodingRules.class.getName());
        checkRuleAndIgnoreFailure(classes, rule);

        assertThat(ExampleExtension.getConfigurationEvents()).isEmpty();
        assertThat(ExampleExtension.getEvaluatedRuleEvents()).isEmpty();
    }

    private static Condition<Object> containingEntry(final String propKey, final String propValue) {
        return new Condition<Object>(String.format("containing entry {%s=%s}", propKey, propValue)) {
            @Override
            public boolean matches(Object value) {
                return Objects.equals(((Map<?, ?>) value).get(propKey), propValue);
            }
        };
    }

    private static void checkRuleAndIgnoreFailure(JavaClasses classes, ArchRule rule) {
        try {
            rule.check(classes);
            throw new RuntimeException("Should have thrown an " + AssertionError.class.getSimpleName());
        } catch (AssertionError ignored) {
        }
    }
}
