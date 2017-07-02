/*
 * Copyright 2017 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.junit.MessageAssertionChain.containsConsecutiveLines;
import static com.tngtech.archunit.junit.MessageAssertionChain.containsLine;
import static com.tngtech.archunit.junit.MessageAssertionChain.matchesLine;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singleton;
import static java.util.regex.Pattern.quote;

public class ExpectedViolation implements TestRule {
    private final MessageAssertionChain assertionChain = new MessageAssertionChain();

    private ExpectedViolation() {
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new ExpectedViolationStatement(base);
    }

    public static ExpectedViolation none() {
        return new ExpectedViolation();
    }

    public ExpectedViolation ofRule(String ruleText) {
        LinkedList<String> ruleLines = new LinkedList<>(Splitter.on(lineSeparator()).splitToList(ruleText));
        checkArgument(!ruleLines.isEmpty(), "Rule text may not be empty");
        if (ruleLines.size() == 1) {
            addSingleLineRuleAssertion(getOnlyElement(ruleLines));
        } else {
            addMultiLineRuleAssertion(ruleLines);
        }
        return this;
    }

    private void addSingleLineRuleAssertion(String ruleText) {
        assertionChain.add(matchesLine(String.format(
                "Architecture Violation .* Rule '%s' was violated.*", quote(ruleText))));
    }

    private void addMultiLineRuleAssertion(LinkedList<String> ruleLines) {
        assertionChain.add(matchesLine(String.format(
                "Architecture Violation .* Rule '%s", quote(ruleLines.pollFirst()))));
        assertionChain.add(matchesLine(String.format("%s' was violated.*", quote(ruleLines.pollLast()))));
        assertionChain.add(containsConsecutiveLines(ruleLines));
    }

    public ExpectedViolation byAccess(ExpectedFieldAccess access) {
        assertionChain.add(containsLine(access.expectedMessage()));
        return this;
    }

    public ExpectedViolation byCall(ExpectedMethodCall call) {
        assertionChain.add(containsLine(call.expectedMessage()));
        return this;
    }

    public ExpectedViolation by(MessageAssertionChain.Link assertion) {
        assertionChain.add(assertion);
        return this;
    }

    public static PackageAssertionCreator javaPackageOf(Class<?> clazz) {
        return new PackageAssertionCreator(clazz);
    }

    @Internal
    public static class PackageAssertionCreator {
        private final Class<?> clazz;

        private PackageAssertionCreator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public MessageAssertionChain.Link notMatching(String packageIdentifier) {
            return containsLine("Class %s doesn't reside in a package '%s'", clazz.getName(), packageIdentifier);
        }
    }

    private class ExpectedViolationStatement extends Statement {
        private final Statement base;

        private ExpectedViolationStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
                throw new NoExpectedViolationException(assertionChain);
            } catch (AssertionError assertionError) {
                assertionChain.evaluate(assertionError);
            }
        }
    }

    private static class NoExpectedViolationException extends RuntimeException {
        private NoExpectedViolationException(MessageAssertionChain assertionChain) {
            super("Rule was not violated in the expected way: Expected " + assertionChain);
        }
    }

    public static ExpectedAccessViolationCreationProcess from(Class<?> origin, String method, Class<?>... paramTypes) {
        return new ExpectedAccessViolationCreationProcess(origin, method, paramTypes);
    }

    @Internal
    public static class ExpectedAccessViolationCreationProcess {
        private Origin origin;

        private ExpectedAccessViolationCreationProcess(Class<?> clazz, String method, Class<?>[] paramTypes) {
            origin = new Origin(clazz, method, paramTypes);
        }

        public ExpectedFieldAccessViolationBuilderStep1 accessing() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, AccessType.GET, AccessType.SET);
        }

        public ExpectedFieldAccessViolationBuilderStep1 getting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, AccessType.GET);
        }

        public ExpectedFieldAccessViolationBuilderStep1 setting() {
            return new ExpectedFieldAccessViolationBuilderStep1(origin, AccessType.SET);
        }

        public ExpectedMethodCallViolationBuilder toMethod(Class<?> target, String member, Class<?>... paramTypes) {
            return new ExpectedMethodCallViolationBuilder(
                    origin, new MethodTarget(target, member, paramTypes));
        }

        public ExpectedMethodCallViolationBuilder toConstructor(Class<?> target, Class<?>... paramTypes) {
            return new ExpectedMethodCallViolationBuilder(
                    origin, new ConstructorTarget(target, paramTypes));
        }
    }

    private abstract static class ExpectedAccessViolationBuilder {
        final Origin origin;
        final Target target;

        private ExpectedAccessViolationBuilder(Origin origin, Target target) {
            this.origin = origin;
            this.target = target;
        }
    }

    @Internal
    public static class ExpectedFieldAccessViolationBuilderStep1 {
        private final Origin origin;
        private final ImmutableSet<AccessType> accessType;

        private ExpectedFieldAccessViolationBuilderStep1(Origin origin, AccessType... accessType) {
            this.origin = origin;
            this.accessType = ImmutableSet.copyOf(accessType);
        }

        public ExpectedFieldAccessViolationBuilderStep2 field(Class<?> target, String member) {
            return new ExpectedFieldAccessViolationBuilderStep2(
                    origin, new FieldTarget(target, member, accessType));
        }
    }

    @Internal
    public static class ExpectedFieldAccessViolationBuilderStep2 extends ExpectedAccessViolationBuilder {
        private ExpectedFieldAccessViolationBuilderStep2(Origin origin, FieldTarget target) {
            super(origin, target);
        }

        public ExpectedFieldAccess inLine(int number) {
            return new ExpectedFieldAccess(origin, target, number);
        }
    }

    @Internal
    public static class ExpectedMethodCallViolationBuilder extends ExpectedAccessViolationBuilder {
        private ExpectedMethodCallViolationBuilder(Origin origin, MethodTarget target) {
            super(origin, target);
        }

        public ExpectedMethodCall inLine(int number) {
            return new ExpectedMethodCall(origin, target, number);
        }
    }

    @Internal
    public abstract static class ExpectedAccess {
        private final Origin origin;
        private final Target target;
        private final int lineNumber;

        private ExpectedAccess(Origin origin, Target target, int lineNumber) {
            this.origin = origin;
            this.target = target;
            this.lineNumber = lineNumber;
        }

        String expectedMessage() {
            return target.messageFor(this);
        }

        @Override
        public String toString() {
            return expectedMessage();
        }
    }

    static class ExpectedFieldAccess extends ExpectedAccess {
        private ExpectedFieldAccess(Origin origin, Target target, int lineNumber) {
            super(origin, target, lineNumber);
        }
    }

    static class ExpectedMethodCall extends ExpectedAccess {
        private ExpectedMethodCall(Origin origin, Target target, int lineNumber) {
            super(origin, target, lineNumber);
        }
    }

    private abstract static class Member {
        private final Class<?> clazz;
        private final String memberName;
        final List<String> params = new ArrayList<>();

        private Member(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            this.clazz = clazz;
            this.memberName = memberName;
            for (Class<?> paramType : paramTypes) {
                params.add(paramType.getName());
            }
        }

        String lineMessage(int number) {
            return String.format("(%s.java:%d)", clazz.getSimpleName(), number);
        }

        @Override
        public String toString() {
            return String.format("%s.%s", clazz.getName(), memberName);
        }
    }

    private static class Origin extends Member {
        private Origin(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            super(clazz, methodName, paramTypes);
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(params));
        }
    }

    private abstract static class Target extends Member {
        private Target(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            super(clazz, memberName, paramTypes);
        }

        String messageFor(ExpectedAccess access) {
            return String.format(template(),
                    access.origin, access.target, access.origin.lineMessage(access.lineNumber));
        }

        abstract String template();
    }

    private static class FieldTarget extends Target {
        private final Map<Set<AccessType>, String> accessDescription = ImmutableMap.of(
                singleton(AccessType.GET), "gets",
                singleton(AccessType.SET), "sets",
                EnumSet.of(AccessType.GET, AccessType.SET), "accesses"
        );

        private final String accesses;

        private FieldTarget(Class<?> clazz, String memberName, ImmutableSet<AccessType> accessTypes) {
            super(clazz, memberName, new Class<?>[0]);
            this.accesses = accessDescription.get(accessTypes);
        }

        @Override
        String template() {
            return "Method <%s> " + accesses + " field <%s> in %s";
        }
    }

    private static class MethodTarget extends Target {
        private MethodTarget(Class<?> clazz, String memberName, Class<?>[] paramTypes) {
            super(clazz, memberName, paramTypes);
        }

        @Override
        String template() {
            return "Method <%s> calls method <%s> in %s";
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", super.toString(), Joiner.on(", ").join(params));
        }
    }

    private static class ConstructorTarget extends MethodTarget {
        private ConstructorTarget(Class<?> clazz, Class<?>[] paramTypes) {
            super(clazz, CONSTRUCTOR_NAME, paramTypes);
        }

        @Override
        String template() {
            return "Method <%s> calls constructor <%s> in %s";
        }
    }
}
