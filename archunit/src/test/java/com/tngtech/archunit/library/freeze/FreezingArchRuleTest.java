package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class FreezingArchRuleTest {

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void delegates_description() {
        ArchRule rule = rule("some description").withoutViolations();

        ArchRule frozen = freeze(rule);

        assertThat(frozen.getDescription()).isEqualTo(rule.getDescription());
    }

    @Test
    public void toString_shows_original_rule_and_FreezingArchRule() {
        ArchRule rule = rule("some description").withoutViolations();

        ArchRule frozen = freeze(rule);

        assertThat(frozen.toString()).isEqualTo(String.format("%s{%s}", FreezingArchRule.class.getSimpleName(), rule.toString()));
    }

    @Test
    public void supports_overriding_description() {
        FreezingArchRule rule = freeze(rule("old").withoutViolations());

        rule = rule.as("new description");

        assertThat(rule)
                .hasDescriptionContaining("new description");
    }

    @Test
    public void supports_because_clause() {
        FreezingArchRule rule = freeze(rule("any description").withoutViolations());

        rule = rule.because("some reason");

        assertThat(rule.getDescription())
                .contains("any description")
                .contains("because some reason");
    }

    @Test
    public void freezes_violations_on_first_call() {
        ArchRule input = rule("some description").withViolations("first violation", "second violation");

        TestViolationStore violationStore = new TestViolationStore();
        ArchRule frozen = freeze(input).persistIn(violationStore);

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        violationStore.verifyStoredRule("some description", "first violation", "second violation");
    }

    @Test
    public void passes_on_consecutive_calls_without_new_violations() {
        ArchRule input = rule("some description").withViolations("first violation", "second violation");

        TestViolationStore violationStore = new TestViolationStore();
        ArchRule frozen = freeze(input).persistIn(violationStore);

        JavaClasses classes = importClasses(getClass());
        frozen.check(classes);
        frozen.check(classes);
        frozen.check(classes);
    }

    @Test
    public void fails_on_violations_additional_to_frozen_ones() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description").withViolations("first violation"));

        ArchRule anotherViolation = rule("some description").withViolations("first violation", "second violation");
        ArchRule frozenWithNewViolation = freeze(anotherViolation).persistIn(violationStore);

        assertThat(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("second violation");
    }

    @Test
    public void automatically_reduces_allowed_violations_if_any_vanish() {
        TestViolationStore violationStore = new TestViolationStore();

        String secondViolation = "second violation";
        createFrozen(violationStore, rule("some description").withViolations("first violation", secondViolation));

        ArchRule secondViolationSolved = rule("some description").withViolations("first violation");
        ArchRule frozenWithLessViolation = freeze(secondViolationSolved).persistIn(violationStore);

        assertThat(frozenWithLessViolation)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        ArchRule secondViolationIsBack = rule("some description").withViolations("first violation", secondViolation);
        ArchRule frozenWithOldViolationBack = freeze(secondViolationIsBack).persistIn(violationStore);

        assertThat(frozenWithOldViolationBack)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(secondViolation);
    }

    // This is e.g. useful to ignore the line number in "message (Foo.java:xxx)"
    @Test
    public void allows_to_specify_a_custom_matcher_to_decide_which_violations_count_as_known() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("some #ignore_this# violation", "second #ignore_this# violation"));

        ArchRule frozen = freeze(rule("some description")
                .withViolations("some #now changed# violation", "second #now changed somehow# violation", "and new"))
                .persistIn(violationStore)
                .associateViolationLinesVia(new ViolationLineMatcher() {
                    @Override
                    public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
                        String storedCleanedUp = lineFromFirstViolation.replaceAll("#.*#", "");
                        String actualCleanedUp = lineFromSecondViolation.replaceAll("#.*#", "");
                        return storedCleanedUp.equals(actualCleanedUp);
                    }
                });

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("and new");
    }

    @Test
    public void allows_to_customize_ViolationStore_by_configuration() {
        ArchConfiguration.get().setProperty("freeze.store", TestViolationStore.class.getName());
        ArchConfiguration.get().setProperty("freeze.store.first.property", "first value");
        ArchConfiguration.get().setProperty("freeze.store.second.property", "second value");
        ArchConfiguration.get().setProperty("freeze.unrelated", "unrelated value");

        freeze(rule("some description")
                .withViolations("first violation", "second violation"))
                .check(importClasses(getClass()));

        TestViolationStore store = TestViolationStore.getLastStoreCreated();
        store.verifyInitializationProperties("first.property", "first value", "second.property", "second value");
        store.verifyStoredRule("some description", "first violation", "second violation");
    }

    @Test
    public void default_violation_store_works() throws IOException {
        File folder = temporaryFolder.newFolder();
        ArchConfiguration.get().setProperty("freeze.store.default.path", folder.getAbsolutePath());

        String[] frozenViolations = {"first violation", "second violation"};
        FreezingArchRule frozen = freeze(rule("some description")
                .withViolations(frozenViolations));

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        frozen = freeze(rule("some description")
                .withViolations(frozenViolations[0], "third violation"));

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("third violation");

        frozen = freeze(rule("some description")
                .withViolations(frozenViolations[0], frozenViolations[1], "third violation"));

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(frozenViolations[1], "third violation");
    }

    private void createFrozen(TestViolationStore violationStore, ArchRule rule) {
        FreezingArchRule frozen = freeze(rule).persistIn(violationStore);

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();
    }

    private static RuleCreator rule(String description) {
        return new RuleCreator(description);
    }

    private static class RuleCreator {
        private final String description;

        private RuleCreator(String description) {
            this.description = description;
        }

        ArchRule withoutViolations() {
            return withViolations(description);
        }

        ArchRule withViolations(final String... messages) {
            return classes().should(new ArchCondition<JavaClass>("") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (String message : messages) {
                        events.add(violated(javaClass, message));
                    }
                }
            }).as(description);
        }
    }

    private static class TestViolationStore implements ViolationStore {
        static AtomicReference<TestViolationStore> lastStoreCreated = new AtomicReference<>();

        private Properties initializationProperties;
        private final Map<String, StoredRule> storedRules = new HashMap<>();

        TestViolationStore() {
            lastStoreCreated.set(this);
        }

        static TestViolationStore getLastStoreCreated() {
            return checkNotNull(lastStoreCreated.get(), "No store has ever been created");
        }

        void verifyStoredRule(String description, String... violations) {
            StoredRule storedRule = storedRules.get(description);
            assertThat(storedRule).as(String.format("stored rule [%s]", description)).isNotNull();
            assertThat(storedRule.violations).containsOnly(violations);
        }

        void verifyInitializationProperties(String... entries) {
            assertThat(initializationProperties).as("Initialization Properties").isNotNull();

            Map<Object, Object> actual = ImmutableMap.copyOf(initializationProperties);
            ImmutableMap.Builder<Object, Object> expected = ImmutableMap.builder();
            for (int i = 0; i < entries.length; i += 2) {
                expected.put(entries[i], entries[i + 1]);
            }
            assertThat(actual).isEqualTo(expected.build());
        }

        @Override
        public void initialize(Properties properties) {
            initializationProperties = properties;
        }

        @Override
        public boolean contains(ArchRule rule) {
            return storedRules.containsKey(rule.getDescription());
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            storedRules.put(rule.getDescription(), new StoredRule(violations));
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            return storedRules.get(rule.getDescription()).violations;
        }

        private static class StoredRule {
            private final List<String> violations;

            private StoredRule(List<String> violations) {
                this.violations = violations;
            }
        }
    }
}