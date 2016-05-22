package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.exampletest.SessionBeanRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
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
                        .inLine(23));

        super.stateless_session_beans_should_not_have_state();
    }
}
