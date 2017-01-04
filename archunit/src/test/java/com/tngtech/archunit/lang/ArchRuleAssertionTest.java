package com.tngtech.archunit.lang;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.tngtech.archunit.core.JavaClass;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.ArchRuleAssertion.ARCHUNIT_IGNORE_PATTERNS_FILE_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ArchRuleAssertionTest {
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
    public void assertion_should_print_all_event_messages() {
        expectAssertionErrorWithMessages("first", "second");

        all(classes()).should(conditionThatReportsErrors("first", "second"))
                .check(javaClassesViaReflection(ArchRuleAssertionTest.class));
    }

    @Test
    public void assertion_should_filter_messages_to_be_ignored() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        expectAssertionErrorWithMessages("third one more", "fourth");

        all(classes()).should(conditionThatReportsErrors("first one", "second two", "third one more", "fourth"))
                .check(javaClassesViaReflection(ArchRuleAssertionTest.class));
    }

    @Test
    public void if_all_messages_are_ignored_the_test_passes() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        all(classes()).should(conditionThatReportsErrors("first one", "second two"))
                .check(javaClassesViaReflection(ArchRuleAssertionTest.class));
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
        thrown.expect(ArchAssertionError.class);
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
                    events.add(ConditionEvent.violated(message));
                }
            }
        };
    }
}