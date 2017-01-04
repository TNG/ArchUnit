package com.tngtech.archunit.lang;

import java.util.Set;

import com.google.common.collect.ForwardingSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Restrictable;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClassesTest;
import com.tngtech.archunit.core.properties.HasDescription;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.core.JavaClassesTest.SOME_CLASS;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static java.util.Collections.singleton;

public class ArchRuleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void priority_is_passed_on_open_rule() {
        thrown.expect(ArchAssertionError.class);
        thrown.expect(priority(HIGH));

        ArchRule.Definition.priority(HIGH).all(classes())
                .should(ALWAYS_BE_VIOLATED)
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
        public DummyCollection that(DescribedPredicate<? super JavaClass> predicate) {
            return this;
        }
    }

    private static final ArchCondition<JavaClass> ALWAYS_BE_VIOLATED =
            new ArchCondition<JavaClass>("always be violated") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    events.add(new ConditionEvent(false, "I'm violated"));
                }
            };
}