package com.tngtech.archunit.integration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.SecondBeanImplementingSomeBusinessInterface;
import com.tngtech.archunit.example.SomeBusinessInterface;
import com.tngtech.archunit.exampletest.SessionBeanRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedViolation.from;

public class SessionBeanRulesIntegrationTest extends SessionBeanRulesTest {
    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void stateless_session_beans_should_not_have_state() {
        expectedViolation.ofRule("Stateless Session Beans should not have state")
                .byAccess(from(ClassViolatingSessionBeanRules.class, "setState", String.class)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(25));

        super.stateless_session_beans_should_not_have_state();
    }

    @Test
    @Override
    public void business_interface_implementations_should_be_unique() {
        expectedViolation.ofRule("Business Interfaces should have an unique implementation")
                .byViolation(SOME_BUSINESS_INTERFACE_IS_IMPLEMENTED_BY_TWO_BEANS);

        super.business_interface_implementations_should_be_unique();
    }

    private static final TypeSafeMatcher<String> SOME_BUSINESS_INTERFACE_IS_IMPLEMENTED_BY_TWO_BEANS = new TypeSafeMatcher<String>() {
        @Override
        public void describeTo(Description description) {
            String violatingImplementations = Joiner.on(", ").join(
                    ClassViolatingSessionBeanRules.class.getSimpleName(),
                    SecondBeanImplementingSomeBusinessInterface.class.getSimpleName());

            description.appendText(String.format("%s is implemented by {%s}",
                    SomeBusinessInterface.class.getSimpleName(), violatingImplementations));
        }

        @Override
        protected boolean matchesSafely(String item) {
            String[] parts = item.replaceAll(String.format(".*%n"), "").split(" is implemented by ");
            if (parts.length != 2) {
                return false;
            }
            ImmutableSet<String> violations = ImmutableSet.copyOf(parts[1].split(", "));
            return parts[0].equals(SomeBusinessInterface.class.getSimpleName()) &&
                    violations.equals(ImmutableSet.of(
                            ClassViolatingSessionBeanRules.class.getSimpleName(),
                            SecondBeanImplementingSomeBusinessInterface.class.getSimpleName()));
        }
    };
}
