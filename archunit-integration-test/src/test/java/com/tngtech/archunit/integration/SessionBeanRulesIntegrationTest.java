package com.tngtech.archunit.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.example.ClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.OtherClassViolatingSessionBeanRules;
import com.tngtech.archunit.example.SecondBeanImplementingSomeBusinessInterface;
import com.tngtech.archunit.example.SomeBusinessInterface;
import com.tngtech.archunit.exampletest.SessionBeanRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.junit.MessageAssertionChain;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.collect.Collections2.filter;
import static com.tngtech.archunit.example.OtherClassViolatingSessionBeanRules.init;
import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;

public class SessionBeanRulesIntegrationTest extends SessionBeanRulesTest {
    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void stateless_session_beans_should_not_have_state() {
        expectedViolation.ofRule("No Stateless Session Bean should have state")
                .by(callFromMethod(ClassViolatingSessionBeanRules.class, "setState", String.class)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(25))
                .by(callFromMethod(OtherClassViolatingSessionBeanRules.class, init)
                        .setting().field(ClassViolatingSessionBeanRules.class, "state")
                        .inLine(13));

        super.stateless_session_beans_should_not_have_state();
    }

    @Test
    @Override
    public void business_interface_implementations_should_be_unique() {
        expectedViolation.ofRule("classes that are business interfaces should have an unique implementation")
                .by(SOME_BUSINESS_INTERFACE_IS_IMPLEMENTED_BY_TWO_BEANS);

        super.business_interface_implementations_should_be_unique();
    }

    private static final MessageAssertionChain.Link SOME_BUSINESS_INTERFACE_IS_IMPLEMENTED_BY_TWO_BEANS =
            new MessageAssertionChain.Link() {
                @Override
                public Result filterMatching(List<String> lines) {
                    Collection<String> interesting = filter(lines, containsPattern(" is implemented by "));
                    if (interesting.size() != 1) {
                        return new Result(false, lines);
                    }
                    String[] parts = interesting.iterator().next().split(" is implemented by ");
                    if (parts.length != 2) {
                        return new Result(false, lines);
                    }

                    if (partsMatchExpectedViolation(parts)) {
                        List<String> resultLines = new ArrayList<>(lines);
                        resultLines.removeAll(interesting);
                        return new Result(true, resultLines);
                    } else {
                        return new Result(false, lines);
                    }
                }

                private boolean partsMatchExpectedViolation(String[] parts) {
                    ImmutableSet<String> violations = ImmutableSet.copyOf(parts[1].split(", "));
                    return parts[0].equals(SomeBusinessInterface.class.getSimpleName()) &&
                            violations.equals(ImmutableSet.of(
                                    ClassViolatingSessionBeanRules.class.getSimpleName(),
                                    SecondBeanImplementingSomeBusinessInterface.class.getSimpleName()));
                }

                @Override
                public String getDescription() {
                    String violatingImplementations = Joiner.on(", ").join(
                            ClassViolatingSessionBeanRules.class.getSimpleName(),
                            SecondBeanImplementingSomeBusinessInterface.class.getSimpleName());

                    return String.format("Message contains: %s is implemented by {%s}",
                            SomeBusinessInterface.class.getSimpleName(), violatingImplementations);
                }
            };
}
