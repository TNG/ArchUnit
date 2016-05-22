package com.tngtech.archunit.lang;

import java.util.Set;

import com.google.common.collect.ForwardingSet;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClassesTest;
import com.tngtech.archunit.core.Restrictable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.DescribedPredicate.all;
import static com.tngtech.archunit.core.JavaClassesTest.SOME_CLASS;
import static com.tngtech.archunit.lang.Priority.LOW;
import static java.util.Collections.singleton;

public class ArchRuleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void priority_is_passed_on_closed_rule() {
        thrown.expect(ArchAssertionError.class);
        thrown.expect(priority(LOW));

        ArchRule.all(new DummyCollection()).should("satisfy something")
                .withPriority(LOW)
                .assertedBy(ALWAYS_VIOLATED);
    }

    @Test
    public void priority_is_passed_on_open_rule() {
        thrown.expect(ArchAssertionError.class);
        thrown.expect(priority(LOW));

        ArchRule.rule(all(JavaClass.class)).should("satisfy something")
                .withPriority(LOW)
                .assertedBy(ALWAYS_VIOLATED)
                .check(JavaClassesTest.ALL_CLASSES);
    }

    private static Matcher<ArchAssertionError> priority(final Priority priority) {
        return new TypeSafeDiagnosingMatcher<ArchAssertionError>() {
            @Override
            protected boolean matchesSafely(ArchAssertionError item, Description mismatchDescription) {
                mismatchDescription.appendText(String.format("was '%s'", item.getPriority()));
                return item.getPriority() == priority;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Priority '%s'", priority));
            }
        };
    }

    private static class DummyCollection extends ForwardingSet<JavaClass>
            implements HasDescription, Restrictable<JavaClass, DummyCollection> {
        private Set<JavaClass> delegate = singleton(SOME_CLASS);

        @Override
        protected Set<JavaClass> delegate() {
            return delegate;
        }

        @Override
        public String getDescription() {
            return "Some Description";
        }

        @Override
        public DummyCollection that(DescribedPredicate<JavaClass> predicate) {
            return this;
        }
    }

    private static final ArchCondition<JavaClass> ALWAYS_VIOLATED = new ArchCondition<JavaClass>() {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            events.add(new ConditionEvent(false, "I'm violated"));
        }
    };
}