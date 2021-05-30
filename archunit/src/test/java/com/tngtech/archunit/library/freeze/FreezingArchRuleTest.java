package com.tngtech.archunit.library.freeze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.cartesianProduct;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
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
        ArchRule rule = rule("some description").withoutViolations().create();

        ArchRule frozen = freeze(rule);

        assertThat(frozen.getDescription()).isEqualTo(rule.getDescription());
    }

    @Test
    public void toString_shows_original_rule_and_FreezingArchRule() {
        ArchRule rule = rule("some description").withoutViolations().create();

        ArchRule frozen = freeze(rule);

        assertThat(frozen.toString()).isEqualTo(String.format("%s{%s}", FreezingArchRule.class.getSimpleName(), rule.toString()));
    }

    @Test
    public void supports_overriding_description() {
        FreezingArchRule rule = freeze(rule("old").withoutViolations().create());

        rule = rule.as("new description");

        assertThatRule(rule)
                .hasDescriptionContaining("new description");
    }

    @Test
    public void supports_because_clause() {
        FreezingArchRule rule = freeze(rule("any description").withoutViolations().create());

        rule = rule.because("some reason");

        assertThat(rule.getDescription())
                .contains("any description")
                .contains("because some reason");
    }

    @Test
    public void freezes_violations_on_first_call() {
        ArchRule input = rule("some description").withViolations("first violation", "second violation").create();

        TestViolationStore violationStore = new TestViolationStore();
        ArchRule frozen = freeze(input).persistIn(violationStore);

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        violationStore.verifyStoredRule("some description", "first violation", "second violation");
    }

    @Test
    public void passes_on_consecutive_calls_without_new_violations() {
        ArchRule input = rule("some description").withViolations("first violation", "second violation").create();

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

        createFrozen(violationStore, rule("some description").withViolations("first violation").create());

        ArchRule anotherViolation = rule("some description").withViolations("first violation", "second violation").create();
        ArchRule frozenWithNewViolation = freeze(anotherViolation).persistIn(violationStore);

        assertThatRule(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("second violation");
    }

    @Test
    public void allows_to_overwrite_frozen_violations_if_configured() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description").withViolations("first violation").create());

        ArchRule anotherViolation = rule("some description").withViolations("first violation", "second violation").create();
        ArchRule frozenWithNewViolation = freeze(anotherViolation).persistIn(violationStore);

        ArchConfiguration.get().setProperty("freeze.refreeze", Boolean.TRUE.toString());

        assertThatRule(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        ArchConfiguration.get().setProperty("freeze.refreeze", Boolean.FALSE.toString());

        assertThatRule(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        ArchRule yetAnotherViolation = rule("some description").withViolations("first violation", "second violation", "third violation").create();
        ArchRule frozenWithYetAnotherViolation = freeze(yetAnotherViolation).persistIn(violationStore);

        assertThatRule(frozenWithYetAnotherViolation)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("third violation");
    }

    @Test
    public void only_reports_relevant_lines_of_multi_line_events() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description").withViolations(
                new ViolatedEvent("first violation1", "second violation1"),
                new ViolatedEvent("first violation2", "second violation2")).create());

        ArchRule anotherViolation = rule("some description").withViolations(
                new ViolatedEvent("first violation1", "second violation1", "third violation1"),
                new ViolatedEvent("first violation2", "second violation2")).create();
        ArchRule frozenWithNewViolation = freeze(anotherViolation).persistIn(violationStore);

        assertThatRule(frozenWithNewViolation)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("third violation1");
    }

    @Test
    public void automatically_reduces_allowed_violations_if_any_vanish() {
        TestViolationStore violationStore = new TestViolationStore();

        String secondViolation = "second violation";
        createFrozen(violationStore, rule("some description").withViolations("first violation", secondViolation).create());

        ArchRule secondViolationSolved = rule("some description").withViolations("first violation").create();
        ArchRule frozenWithLessViolation = freeze(secondViolationSolved).persistIn(violationStore);

        assertThatRule(frozenWithLessViolation)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        ArchRule secondViolationIsBack = rule("some description").withViolations("first violation", secondViolation).create();
        ArchRule frozenWithOldViolationBack = freeze(secondViolationIsBack).persistIn(violationStore);

        assertThatRule(frozenWithOldViolationBack)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(secondViolation);
    }

    // This is e.g. useful to ignore the line number in "message (Foo.java:xxx)"
    @Test
    public void allows_to_specify_a_custom_matcher_to_decide_which_violations_count_as_known() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("some #ignore_this# violation", "second #ignore_this# violation").create());

        ArchRule frozen = freeze(rule("some description")
                .withViolations("some #now changed# violation", "second #now changed somehow# violation", "and new").create())
                .persistIn(violationStore)
                .associateViolationLinesVia(new ViolationLineMatcher() {
                    @Override
                    public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
                        String storedCleanedUp = lineFromFirstViolation.replaceAll("#.*#", "");
                        String actualCleanedUp = lineFromSecondViolation.replaceAll("#.*#", "");
                        return storedCleanedUp.equals(actualCleanedUp);
                    }
                });

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("and new");
    }

    @Test
    public void fails_on_an_increased_violation_count_of_the_same_violation_compared_to_frozen_ones() {
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("violation").create());

        ArchRule frozen = freeze(rule("some description")
                .withViolations("violation", "equivalent one").create())
                .persistIn(violationStore)
                .associateViolationLinesVia(new ViolationLineMatcher() {
                    @Override
                    public boolean matches(String lineFromFirstViolation, String lineFromSecondViolation) {
                        return true;
                    }
                });

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasViolations(1)
                .hasAnyViolationOf("violation", "equivalent one");
    }

    @DataProvider
    public static List<List<String>> different_line_separators_to_store_and_read() {
        String windowsLineSeparator = "\r\n";
        String unixLineSeparator = "\n";

        // Since Mac OS should also use the Unix line separator, this Set should effectively only ever contain two line separators
        // Nevertheless this way we'll see a CI failure on some Mac OS environment if this assumption is wrong
        Set<String> lineSeparators = ImmutableSet.of(windowsLineSeparator, unixLineSeparator, System.lineSeparator());

        return FluentIterable.from(cartesianProduct(lineSeparators, lineSeparators)).filter(new Predicate<List<String>>() {
            @Override
            public boolean apply(List<String> input) {
                return !input.get(0).equals(input.get(1));
            }
        }).toList();
    }

    @Test
    @UseDataProvider("different_line_separators_to_store_and_read")
    public void default_violation_store_works_with_multi_line_rule_texts_with_different_line_separators(
            String lineSeparatorOnFreeze, String lineSeparatorOnCheck) throws IOException {

        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        RuleCreator ruleCreator = rule("any rule${lineSeparator}with several${lineSeparator}lines with several words")
                .withViolations("some violation");

        ArchRule storedRule = freeze(ruleCreator.withStringReplace("${lineSeparator}", lineSeparatorOnFreeze).create());
        assertThatRule(storedRule).checking(importClasses(getClass())).hasNoViolation();

        ArchRule ruleToCheck = ruleCreator.withStringReplace("${lineSeparator}", lineSeparatorOnCheck)
                .withViolations("some violation", "new").create();
        assertThatRule(freeze(ruleToCheck))
                .checking(importClasses(getClass()))
                .hasOnlyViolations("new");
    }

    @Test
    @UseDataProvider("different_line_separators_to_store_and_read")
    public void works_with_multi_line_violations_with_different_line_separators(String lineSeparatorOnFreeze, String lineSeparatorOnCheck) {
        TestViolationStore violationStore = new TestViolationStore();

        RuleCreator ruleCreator = rule("some rule")
                .withViolations(
                        new ViolatedEvent(
                                "first violation1${lineSeparator}with multiple${lineSeparator}lines with several words",
                                "second violation1${lineSeparator}with multiple${lineSeparator}lines with several words"),
                        new ViolatedEvent(
                                "first violation2${lineSeparator}with multiple${lineSeparator}lines with several words",
                                "second violation2${lineSeparator}with multiple${lineSeparator}lines with several words"));

        ArchRule storedRule = ruleCreator.withStringReplace("${lineSeparator}", lineSeparatorOnFreeze).create();
        createFrozen(violationStore, storedRule);

        ArchRule ruleToCheck = ruleCreator.withStringReplace("${lineSeparator}", lineSeparatorOnCheck).create();
        assertThatRule(freeze(ruleToCheck).persistIn(violationStore))
                .checking(importClasses(getClass()))
                .hasNoViolation();
    }

    @Test
    public void allows_to_customize_ViolationStore_by_configuration() {
        ArchConfiguration.get().setProperty("freeze.store", TestViolationStore.class.getName());
        ArchConfiguration.get().setProperty("freeze.store.first.property", "first value");
        ArchConfiguration.get().setProperty("freeze.store.second.property", "second value");
        ArchConfiguration.get().setProperty("freeze.unrelated", "unrelated value");

        freeze(rule("some description")
                .withViolations("first violation", "second violation").create())
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
        freeze(rule("some description").withoutViolations().create()).check(importClasses(getClass()));
    }

    @Test
    public void default_violation_store_works() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        String[] frozenViolations = {"first violation", "second violation"};
        FreezingArchRule frozen = freeze(rule("some description")
                .withViolations(frozenViolations).create());

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasNoViolation();

        frozen = freeze(rule("some description")
                .withViolations(frozenViolations[0], "third violation").create());

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations("third violation");

        frozen = freeze(rule("some description")
                .withViolations(frozenViolations[0], frozenViolations[1], "third violation").create());

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(frozenViolations[1], "third violation");
    }

    @Test
    public void existing_violation_store_can_be_updated_when_creation_is_disabled() throws IOException {
        useTemporaryDefaultStorePath();

        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");
        freeze(rule("first, store must be created").withoutViolations().create()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "false");
        RuleCreator second = rule("second, store exists");
        assertThatRule(freeze(second.withViolations("first").create())).checking(importClasses(getClass()))
                .hasNoViolation();
        assertThatRule(freeze(second.withViolations("first", "second").create())).checking(importClasses(getClass()))
                .hasOnlyViolations("second");
    }

    @Test
    public void allows_to_customize_ViolationLineMatcher_by_configuration() {
        ArchConfiguration.get().setProperty(LINE_MATCHER_PROPERTY_NAME, ConsiderAllLinesWithTheSameStartLetterTheSame.class.getName());
        TestViolationStore violationStore = new TestViolationStore();

        createFrozen(violationStore, rule("some description")
                .withViolations("a violation", "a nother violation", "b violation", "c violation").create());

        String onlyOneDifferentLineByFirstLetter = "d violation";
        FreezingArchRule frozen = freeze(rule("some description")
                .withViolations("a different but counted same", "a nother too", "b too", "c also", onlyOneDifferentLineByFirstLetter).create())
                .persistIn(violationStore);

        assertThatRule(frozen)
                .checking(importClasses(getClass()))
                .hasOnlyViolations(onlyOneDifferentLineByFirstLetter);
    }

    @Test
    public void rejects_illegal_ViolationLineMatcher_configuration() {
        String wrongConfig = "SomeBogus";
        ArchConfiguration.get().setProperty(LINE_MATCHER_PROPERTY_NAME, wrongConfig);

        thrown.expect(ViolationLineMatcherInitializationFailedException.class);
        thrown.expectMessage("freeze.lineMatcher=" + wrongConfig);
        freeze(rule("some description").withoutViolations().create()).check(importClasses(getClass()));
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
                ).create());

        String onlyLineNumberChanged = "first violation one in (SomeClass.java:98) and first violation two in (SomeClass.java:99)";
        String locationClassDoesNotMatch = "second violation in (OtherClass.java:77)";
        String descriptionDoesNotMatch = "unknown violation in (SomeClass.java:77)";
        String lambdaWithDifferentNumber = "Method <MyClass.lambda$myFunction$10()> has a violation in (MyClass.java:123)";
        FreezingArchRule updatedViolations = freeze(rule("some description")
                .withViolations(onlyLineNumberChanged, locationClassDoesNotMatch, descriptionDoesNotMatch, lambdaWithDifferentNumber).create())
                .persistIn(violationStore);

        assertThatRule(updatedViolations)
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
                freeze(rule("some description").withoutViolations().create()).check(importClasses(getClass()));
            }
        }).isInstanceOf(StoreInitializationFailedException.class)
                .hasMessage("Creating new violation store is disabled (enable by configuration " + ALLOW_STORE_CREATION_PROPERTY_NAME + "=true)");
    }

    @Test
    public void can_prevent_default_ViolationStore_from_freezing_unknown_rules() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        freeze(rule("new rule, updates enabled by default").withoutViolations().create()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "true");
        freeze(rule("new rule, updates enabled explicitly").withoutViolations().create()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "false");
        expectStoreUpdateDisabledException();
        freeze(rule("new rule, updates disabled").withoutViolations().create()).check(importClasses(getClass()));
    }

    @Test
    public void can_prevent_default_ViolationStore_from_updating_existing_rules() throws IOException {
        useTemporaryDefaultStorePath();
        ArchConfiguration.get().setProperty(ALLOW_STORE_CREATION_PROPERTY_NAME, "true");

        RuleCreator someRule = rule("some description");
        freeze(someRule.withViolations("remaining", "will be solved").create()).check(importClasses(getClass()));

        ArchConfiguration.get().setProperty(ALLOW_STORE_UPDATE_PROPERTY_NAME, "false");
        expectStoreUpdateDisabledException();
        freeze(someRule.withViolations("remaining").create()).check(importClasses(getClass()));
    }

    private void expectStoreUpdateDisabledException() {
        thrown.expect(StoreUpdateFailedException.class);
        thrown.expectMessage("Updating frozen violations is disabled (enable by configuration " + ALLOW_STORE_UPDATE_PROPERTY_NAME + "=true)");
    }

    private void createFrozen(TestViolationStore violationStore, ArchRule rule) {
        FreezingArchRule frozen = freeze(rule).persistIn(violationStore);

        assertThatRule(frozen)
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
        private final List<ViolatedEvent> events;
        private final Function<String, String> textModifier;

        private RuleCreator(String description) {
            this(description, new ArrayList<ViolatedEvent>(), Functions.<String>identity());
        }

        private RuleCreator(String description, List<ViolatedEvent> events, Function<String, String> textModifier) {
            this.description = description;
            this.textModifier = textModifier;
            this.events = events;
        }

        RuleCreator withoutViolations() {
            return new RuleCreator(description, new ArrayList<ViolatedEvent>(), textModifier);
        }

        RuleCreator withViolations(final String... messages) {
            List<ViolatedEvent> newEvents = new ArrayList<>();
            for (String message : messages) {
                newEvents.add(new ViolatedEvent(message));
            }
            return new RuleCreator(description, newEvents, textModifier);
        }

        RuleCreator withViolations(final ViolatedEvent... events) {
            return new RuleCreator(description, ImmutableList.copyOf(events), textModifier);
        }

        RuleCreator withStringReplace(final String toReplace, final String replaceWith) {
            return new RuleCreator(description, events, new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return input.replace(toReplace, replaceWith);
                }
            });
        }

        ArchRule create() {
            return classes().should(new ArchCondition<JavaClass>("") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
                    for (ViolatedEvent event : RuleCreator.this.events) {
                        conditionEvents.add(event.apply(textModifier));
                    }
                }
            }).as(textModifier.apply(description));
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
        private final List<String> descriptionLines;

        private ViolatedEvent(String... descriptionLines) {
            this(ImmutableList.copyOf(descriptionLines));
        }

        private ViolatedEvent(Collection<String> descriptionLines) {
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
        public List<String> getDescriptionLines() {
            return descriptionLines;
        }

        @Override
        public void handleWith(Handler handler) {
            throw new UnsupportedOperationException("Implement me");
        }

        ViolatedEvent apply(Function<String, String> textModifier) {
            List<String> result = new ArrayList<>();
            for (String line : descriptionLines) {
                result.add(textModifier.apply(line));
            }
            return new ViolatedEvent(result);
        }
    }
}
