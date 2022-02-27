package com.tngtech.archunit.lang;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaClassesTest;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.lang.ArchConditionTest.ConditionWithInitAndFinish;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.lang.ArchRule.Assertions.ARCHUNIT_IGNORE_PATTERNS_FILE_NAME;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.testutil.ArchConfigurationRule.FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME;
import static com.tngtech.archunit.testutil.TestUtils.toUri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchRuleTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();

    @Before
    public void setUp() {
        ignoreFile().delete();
    }

    @After
    public void tearDown() {
        ignoreFile().delete();
    }

    @Test
    public void priority_is_passed() {
        thrown.expect(AssertionError.class);
        thrown.expectMessage("Priority: HIGH");

        ArchRuleDefinition.priority(HIGH).classes()
                .should(ALWAYS_BE_VIOLATED)
                .check(JavaClassesTest.ALL_CLASSES);
    }

    @Test
    public void evaluation_should_print_all_event_messages() {
        expectAssertionErrorWithMessages("first", "second");

        classes().should(conditionThatReportsErrors("first", "second"))
                .check(importClassesWithContext(EvaluationResultTest.class));
    }

    @Test
    public void evaluation_should_filter_messages_to_be_ignored() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        expectAssertionErrorWithMessages("third one more", "fourth");

        classes().should(conditionThatReportsErrors("first one", "second two", "third one more", "fourth"))
                .check(importClassesWithContext(EvaluationResultTest.class));
    }

    @Test
    public void if_all_messages_are_ignored_the_test_passes() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        classes().should(conditionThatReportsErrors("first one", "second two"))
                .check(importClassesWithContext(EvaluationResultTest.class));
    }

    @Test
    public void ignored_pattern_with_comment() throws IOException {
        writeIgnoreFileWithPatterns("# comment1", "#comment2", "regular_reg_exp");

        expectAssertionErrorWithMessages("# comment1", "#comment2");

        classes()
                .should(conditionThatReportsErrors("# comment1", "#comment2", "regular_reg_exp"))
                .check(importClassesWithContext(EvaluationResultTest.class));
    }

    @Test
    public void description_can_be_overridden() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        ArchRule ruleWithOverriddenDescription = classes().should(conditionThatReportsErrors("first one", "second two"))
                .as("rule text overridden");
        String description = ruleWithOverriddenDescription.getDescription();

        assertThat(description).isEqualTo("rule text overridden");

        String failures = ruleWithOverriddenDescription
                .evaluate(importClassesWithContext(EvaluationResultTest.class))
                .getFailureReport().toString();
        assertThat(failures).contains("rule text overridden");
    }

    @Test
    public void because_clause_can_be_added_to_description() {
        ArchRule rule = classes().should(ALWAYS_BE_VIOLATED).because("this is the way");

        assertThat(rule.getDescription()).isEqualTo("classes should always be violated, because this is the way");

        rule = classes().should().accessClassesThat().haveFullyQualifiedName("foo")
                .because("this is the way");

        assertThat(rule.getDescription()).isEqualTo(
                "classes should access classes that have fully qualified name 'foo', because this is the way");
    }

    @Test
    public void reports_number_of_violations() {
        EvaluationResult result = classes().should(addFixedNumberOfViolations(3)).evaluate(importClassesWithContext(Object.class, String.class));

        assertThat(result.getFailureReport().toString()).contains("(6 times)");
    }

    @Test
    public void reports_number_of_violations_separately_for_only_cases() {
        EvaluationResult result = ArchRuleDefinition.noClasses()
                .should().accessClassesThat().haveSimpleName("String")
                .evaluate(importClasses(ClassAccessingStringTwoTimes.class));

        assertThat(result.getFailureReport().toString()).contains("(2 times)");
    }

    @Test
    public void rule_evaluation_inits_and_finishes_condition() {
        ConditionWithInitAndFinish condition = new ConditionWithInitAndFinish("irrelevant") {
            @Override
            public void finish(ConditionEvents events) {
                super.finish(events);
                events.add(SimpleConditionEvent.violated("bummer", "bummer"));
            }
        };
        assertThat(condition.allObjectsToTest).isNull();
        assertThat(condition.eventsFromFinish).isNull();

        all(strings()).should(condition).evaluate(importClasses(getClass()));

        assertThat(condition.allObjectsToTest).containsOnly(getClass().getName());
        assertThat(condition.eventsFromFinish.getAllowed()).isEmpty();
        assertThat(condition.eventsFromFinish.getViolating()).hasSize(1);
    }

    @Test
    public void evaluation_fails_because_of_empty_set_of_elements_with_default_fail_on_empty_should() {
        thrown.expect(AssertionError.class);
        thrown.expectMessage("failed to check any classes");
        thrown.expectMessage(FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME);

        createPassingArchRule().evaluate(importEmptyClasses());
    }

    @Test
    public void evaluation_fails_because_of_empty_set_of_elements_after_that_clause_with_default_fail_on_empty_should() {
        thrown.expect(AssertionError.class);
        thrown.expectMessage("failed to check any classes");
        thrown.expectMessage(FAIL_ON_EMPTY_SHOULD_PROPERTY_NAME);

        createPassingArchRule(strings().that(DescribedPredicate.<String>alwaysFalse())).evaluate(importClasses(SomeClass.class));
    }

    @Test
    public void evaluation_passes_on_empty_set_of_elements_with_deactivated_fail_on_empty_should_by_configuration() {
        archConfigurationRule.setFailOnEmptyShould(false);

        createPassingArchRule().evaluate(importEmptyClasses());
    }

    @Test
    public void evaluation_passes_on_empty_set_of_elements_with_activated_fail_on_empty_should_by_configuration_but_overridden_by_rule() {
        archConfigurationRule.setFailOnEmptyShould(true);

        createPassingArchRule().allowEmptyShould(true).evaluate(importEmptyClasses());
    }

    private JavaClasses importEmptyClasses() {
        return importClasses();
    }

    private ArchRule createPassingArchRule() {
        return createPassingArchRule(strings());
    }

    private <T> ArchRule createPassingArchRule(ClassesTransformer<T> classesTransformer) {
        return ArchRule.Factory.create(classesTransformer, ALWAYS_BE_VALID.<T>forSubtype(), Priority.MEDIUM);
    }

    private ClassesTransformer<String> strings() {
        return new AbstractClassesTransformer<String>("strings") {
            @Override
            public Iterable<String> doTransform(JavaClasses collection) {
                SortedSet<String> result = new TreeSet<>();
                for (JavaClass javaClass : collection) {
                    result.add(javaClass.getName());
                }
                return result;
            }
        };
    }

    private void writeIgnoreFileWithPatterns(String... patterns) throws IOException {
        File ignoreFile = ignoreFile();
        ignoreFile.delete();
        Files.write(Joiner.on("\n").join(patterns), ignoreFile, UTF_8);
    }

    private File ignoreFile() {
        return new File(new File(toUri(getClass().getResource("/"))), ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
    }

    private void expectAssertionErrorWithMessages(final String... messages) {
        thrown.expect(AssertionError.class);
        thrown.expectMessage(containingOnlyLinesWith(messages));
    }

    private TypeSafeMatcher<String> containingOnlyLinesWith(final String[] messages) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Only the error messages '%s'", Joiner.on("', '").join(messages)));
            }

            @Override
            protected boolean matchesSafely(String item) {
                List<String> actualMessageLines = getActualMessageLines(item);
                for (String message : messages) {
                    removeFirstActualMessageContaining(message, actualMessageLines);
                }
                return actualMessageLines.isEmpty();
            }

            private List<String> getActualMessageLines(String item) {
                List<String> result = newArrayList(Splitter.on('\n').split(item));
                result.remove(0);
                return result;
            }

            private void removeFirstActualMessageContaining(String message, List<String> actualMessageLines) {
                for (Iterator<String> iterator = actualMessageLines.iterator(); iterator.hasNext(); ) {
                    if (iterator.next().contains(message)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        };
    }

    private ArchCondition<JavaClass> conditionThatReportsErrors(final String... messages) {
        return new ArchCondition<JavaClass>("not have errors " + Joiner.on(", ").join(messages)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (String message : messages) {
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> addFixedNumberOfViolations(final int number) {
        return new ArchCondition<JavaClass>("be violated exactly %d times", number) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (int i = 0; i < number; i++) {
                    events.add(SimpleConditionEvent.violated(item, item.getSimpleName() + " violation " + i));
                }
            }
        };
    }

    private static final ArchCondition<JavaClass> ALWAYS_BE_VIOLATED =
            new ArchCondition<JavaClass>("always be violated") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    events.add(new SimpleConditionEvent(item, false, "I'm violated"));
                }
            };

    private static final ArchCondition<Object> ALWAYS_BE_VALID =
            new ArchCondition<Object>("always be valid") {
                @Override
                public void check(Object item, ConditionEvents events) {
                }
            };

    @SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
    private static class ClassAccessingStringTwoTimes {
        void execute() {
            "foo".length();
            "bar".replaceAll("a", "b");
        }
    }
}
