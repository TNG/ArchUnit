package com.tngtech.archunit.lang;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassesTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.domain.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.lang.ArchRule.Assertions.ARCHUNIT_IGNORE_PATTERNS_FILE_NAME;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ClassesIdentityTransformer.classes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchRuleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

        ArchRuleDefinition.priority(HIGH).all(classes())
                .should(ALWAYS_BE_VIOLATED)
                .check(JavaClassesTest.ALL_CLASSES);
    }

    @Test
    public void evaluation_should_print_all_event_messages() {
        expectAssertionErrorWithMessages("first", "second");

        all(classes()).should(conditionThatReportsErrors("first", "second"))
                .check(javaClassesViaReflection(EvaluationResultTest.class));
    }

    @Test
    public void evaluation_should_filter_messages_to_be_ignored() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        expectAssertionErrorWithMessages("third one more", "fourth");

        all(classes()).should(conditionThatReportsErrors("first one", "second two", "third one more", "fourth"))
                .check(javaClassesViaReflection(EvaluationResultTest.class));
    }

    @Test
    public void if_all_messages_are_ignored_the_test_passes() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        all(classes()).should(conditionThatReportsErrors("first one", "second two"))
                .check(javaClassesViaReflection(EvaluationResultTest.class));
    }

    @Test
    public void description_can_be_overridden() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        ArchRule ruleWithOverriddenDescription = all(classes()).should(conditionThatReportsErrors("first one", "second two"))
                .as("rule text overridden");
        String description = ruleWithOverriddenDescription.getDescription();

        assertThat(description).isEqualTo("rule text overridden");

        String failures = ruleWithOverriddenDescription
                .evaluate(javaClassesViaReflection(EvaluationResultTest.class))
                .getFailureReport().toString();
        assertThat(failures).contains("rule text overridden");
    }

    @Test
    public void because_clause_can_be_added_to_description() {
        ArchRule rule = all(classes()).should(ALWAYS_BE_VIOLATED).because("this is the way");

        assertThat(rule.getDescription()).isEqualTo("classes should always be violated, because this is the way");

        rule = ArchRule.Factory.create(classes(), ALWAYS_BE_VIOLATED, Priority.MEDIUM).because("this is the way");

        assertThat(rule.getDescription()).isEqualTo("classes should always be violated, because this is the way");

        rule = ArchRuleDefinition.classes().should().accessClassesThat().haveFullyQualifiedName("foo")
                .because("this is the way");

        assertThat(rule.getDescription()).isEqualTo(
                "classes should access classes that have fully qualified name 'foo', because this is the way");
    }

    private void writeIgnoreFileWithPatterns(String... patterns) throws IOException {
        File ignoreFile = ignoreFile();
        ignoreFile.delete();
        Files.write(Joiner.on("\n").join(patterns), ignoreFile, UTF_8);
    }

    private File ignoreFile() {
        return new File(new File(getClass().getResource("/").getFile()), ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
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

    private static final ArchCondition<JavaClass> ALWAYS_BE_VIOLATED =
            new ArchCondition<JavaClass>("always be violated") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    events.add(new SimpleConditionEvent<>(item, false, "I'm violated"));
                }
            };
}