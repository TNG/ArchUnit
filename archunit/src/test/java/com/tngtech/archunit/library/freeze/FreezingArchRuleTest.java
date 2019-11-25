package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CollectsLines;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class FreezingArchRuleTest {

    private static final String STORE_DEFAULT_PATH_PROPERTY_NAME = "freeze.store.default.path";
    private static final String ALLOW_STORE_CREATION_PROPERTY_NAME = "freeze.store.default.allowStoreCreation";
    private static final String ALLOW_STORE_UPDATE_PROPERTY_NAME = "freeze.store.default.allowStoreUpdate";
    private static final String LINE_MATCHER_PROPERTY_NAME = "freeze.lineMatcher";

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
    public void only_reports_relevant_lines_of_multi_line_events() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description").withViolations(
                new ViolatedEvent("first violation1", "second violation1"),
                new ViolatedEvent("first violation2", "second violation2")));

        ArchRule anotherViolation = rule("some description").withViolations(
                new ViolatedEvent("first violation1", "second violation1", "third violation1"),
                new ViolatedEvent("first violation2", "second violation2"));
        ArchRule frozenWithNewViolation = freeze(anotherViolation).persistIn(violationStore);

        assertThat(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("third violation1");
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
    public void fails_on_an_increased_violation_count_of_the_same_violation_compared_to_frozen_ones() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("violation"));

        ArchRule frozen = freeze(rule("some description")
                .withViolations("violation", "equivalent one"))
                .persistIn(violationStore)
                .associateViolationLinesVia(new ViolationLineMatcher() {
                    @Override
                    public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
                        return true;
                    }
                });

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasViolations(1)
                .hasAnyViolationOf("violation", "equivalent one");
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
    public void rejects_illegal_ViolationStore_configuration() {
        String wrongConfig = "SomeBogus";
        ArchConfiguration.get().setProperty("freeze.store", wrongConfig);

        thrown.expect(StoreInitializationFailedException.class);
        thrown.expectMessage("freeze.store=" + wrongConfig);
        freeze(rule("some description").withoutViolations()).check(importClasses(getClass()));
    }

    @Test
    public void default_violation_store_works() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

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

    @Test
    public void existing_violation_store_can_be_updated_when_creation_is_disabled() throws IOException {
        useTemporaryDefaultStorePath();

        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");
        freeze(rule("first, store must be created").withoutViolations()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "false");
        RuleCreator second = rule("second, store exists");
        assertThat(freeze(second.withViolations("first"))).checking(importClasses(getClass()))
                .hasNoViolation();
        assertThat(freeze(second.withViolations("first", "second"))).checking(importClasses(getClass()))
                .hasOnlyViolations("second");
    }

    @Test
    public void allows_to_customize_ViolationLineMatcher_by_configuration() {
        ArchConfiguration.get().setProperty(LINE_MATCHER_PROPERTY_NAME, ConsiderAllLinesWithTheSameStartLetterTheSame.class.getName());
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("a violation", "a nother violation", "b violation", "c violation"));

        String onlyOneDifferentLineByFirstLetter = "d violation";
        FreezingArchRule frozen = freeze(rule("some description")
                .withViolations("a different but counted same", "a nother too", "b too", "c also", onlyOneDifferentLineByFirstLetter))
                .persistIn(violationStore);

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(onlyOneDifferentLineByFirstLetter);
    }

    @Test
    public void rejects_illegal_ViolationLineMatcher_configuration() {
        String wrongConfig = "SomeBogus";
        ArchConfiguration.get().setProperty(LINE_MATCHER_PROPERTY_NAME, wrongConfig);

        thrown.expect(ViolationLineMatcherInitializationFailedException.class);
        thrown.expectMessage("freeze.lineMatcher=" + wrongConfig);
        freeze(rule("some description").withoutViolations()).check(importClasses(getClass()));
    }

    @Test
    public void default_ViolationLineMatcher_ignores_line_numbers_and_auto_generated_numbers() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations(
                        "first violation one in (SomeClass.java:12) and first violation two in (SomeClass.java:13)",
                        "second violation in (SomeClass.java:77)",
                        "third violation in (OtherClass.java:123)",
                        "Method <MyClass.lambda$myFunction$2()> has a violation in (MyClass.java:123)"
                ));

        String onlyLineNumberChanged = "first violation one in (SomeClass.java:98) and first violation two in (SomeClass.java:99)";
        String locationClassDoesNotMatch = "second violation in (OtherClass.java:77)";
        String descriptionDoesNotMatch = "unknown violation in (SomeClass.java:77)";
        String lambdaWithDifferentNumber = "Method <MyClass.lambda$myFunction$10()> has a violation in (MyClass.java:123)";
        FreezingArchRule updatedViolations = freeze(rule("some description")
                .withViolations(onlyLineNumberChanged, locationClassDoesNotMatch, descriptionDoesNotMatch, lambdaWithDifferentNumber))
                .persistIn(violationStore);

        assertThat(updatedViolations)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(locationClassDoesNotMatch, descriptionDoesNotMatch);
    }

    @Test
    public void can_prevent_default_ViolationStore_from_creation() throws IOException {
        useTemporaryDefaultStorePath();

        // Store creation is disabled by default
        assertExceptionDueToDisabledViolationStoreCreationWhenCheckingFreezingArchRule();

        // Explicitly disabling store creation has the same effect
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "false");
        assertExceptionDueToDisabledViolationStoreCreationWhenCheckingFreezingArchRule();
    }

    private void assertExceptionDueToDisabledViolationStoreCreationWhenCheckingFreezingArchRule() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                freeze(rule("some description").withoutViolations()).check(importClasses(getClass()));
            }
        }).isInstanceOf(StoreInitializationFailedException.class)
                .hasMessage("Creating new violation store is disabled (enable by configuration " + ALLOW_STORE_CREATION_PROPERTY_NAME + "=true)");
    }

    @Test
    public void can_prevent_default_ViolationStore_from_freezing_unknown_rules() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        freeze(rule("new rule, updates enabled by default").withoutViolations()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "true");
        freeze(rule("new rule, updates enabled explicitly").withoutViolations()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "false");
        expectStoreUpdateDisabledException();
        freeze(rule("new rule, updates disabled").withoutViolations()).check(importClasses(getClass()));
    }

    @Test
    public void can_prevent_default_ViolationStore_from_updating_existing_rules() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        RuleCreator someRule = rule("some description");
        freeze(someRule.withViolations("remaining", "will be solved")).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "false");
        expectStoreUpdateDisabledException();
        freeze(someRule.withViolations("remaining")).check(importClasses(getClass()));
    }

    private void expectStoreUpdateDisabledException() {
        thrown.expect(StoreUpdateFailedException.class);
        thrown.expectMessage("Updating frozen violations is disabled (enable by configuration " + ALLOW_STORE_UPDATE_PROPERTY_NAME + "=true)");
    }

    private void createFrozen(TestViolationStore violationStore, ArchRule rule) {
        FreezingArchRule frozen = freeze(rule).persistIn(violationStore);

        assertThat(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();
    }

    private void useTemporaryDefaultStorePath() throws IOException {
        File folder = temporaryFolder.newFolder();
        ArchConfiguration.get().setProperty(STORE_DEFAULT_PATH_PROPERTY_NAME, folder.getAbsolutePath());
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
            final Collection<ViolatedEvent> violatedEvents = new ArrayList<>();
            for (String message : messages) {
                violatedEvents.add(new ViolatedEvent(message));
            }
            return createArchRuleWithViolations(violatedEvents);
        }

        ArchRule withViolations(final ViolatedEvent... events) {
            return createArchRuleWithViolations(ImmutableList.copyOf(events));
        }

        private ArchRule createArchRuleWithViolations(final Collection<ViolatedEvent> violatedEvents) {
            return classes().should(new ArchCondition<JavaClass>("") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (ViolatedEvent event : violatedEvents) {
                        events.add(event);
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

    private static class ConsiderAllLinesWithTheSameStartLetterTheSame implements ViolationLineMatcher {
        @Override
        public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
            return lineFromFirstViolation.charAt(0) == lineFromSecondViolation.charAt(0);
        }
    }

    private static class ViolatedEvent implements ConditionEvent {
        private List<String> descriptionLines;

        private ViolatedEvent(String... descriptionLines) {
            this.descriptionLines = ImmutableList.copyOf(descriptionLines);
        }

        @Override
        public boolean isViolation() {
            return true;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            throw new UnsupportedOperationException("Implement me");
        }

        @Override
        public void describeTo(CollectsLines messages) {
            throw new UnsupportedOperationException("Obsolete");
        }

        @Override
        public List<String> getDescriptionLines() {
            return descriptionLines;
        }

        @Override
        public void handleWith(Handler handler) {
            throw new UnsupportedOperationException("Implement me");
        }
    }
}
