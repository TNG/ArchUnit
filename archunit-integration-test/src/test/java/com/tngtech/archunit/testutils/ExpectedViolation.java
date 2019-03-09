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
package com.tngtech.archunit.testutils;

import java.util.LinkedList;

import com.google.common.base.Splitter;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.testutils.ExpectedAccess.ExpectedCall;
import com.tngtech.archunit.testutils.ExpectedAccess.ExpectedFieldAccess;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.quote;

public class ExpectedViolation {
    private final MessageAssertionChain assertionChain;

    private ExpectedViolation(String ruleText) {
        this(new MessageAssertionChain());

        LinkedList<String> ruleLines = new LinkedList<>(Splitter.on(lineSeparator()).splitToList(ruleText));
        checkArgument(!ruleLines.isEmpty(), "Rule text may not be empty");
        if (ruleLines.size() == 1) {
            addSingleLineRuleAssertion(getOnlyElement(ruleLines));
        } else {
            addMultiLineRuleAssertion(ruleLines);
        }
    }

    private ExpectedViolation(MessageAssertionChain assertionChain) {
        this.assertionChain = checkNotNull(assertionChain);
    }

    static ExpectedViolation ofRule(String ruleText) {
        return new ExpectedViolation(ruleText);
    }

    private void addSingleLineRuleAssertion(String ruleText) {
        assertionChain.add(MessageAssertionChain.matchesLine(String.format(
                "Architecture Violation .* Rule '%s' was violated.*", quote(ruleText))));
    }

    private void addMultiLineRuleAssertion(LinkedList<String> ruleLines) {
        assertionChain.add(MessageAssertionChain.matchesLine(String.format(
                "Architecture Violation .* Rule '%s", quote(requireNonNull(ruleLines.pollFirst())))));
        assertionChain.add(MessageAssertionChain.matchesLine(String.format("%s' was violated.*", quote(requireNonNull(ruleLines.pollLast())))));
        assertionChain.add(MessageAssertionChain.containsConsecutiveLines(ruleLines));
    }

    void by(ExpectedFieldAccess access) {
        access.associateLines(toAssertionChain());
    }

    void by(ExpectedCall call) {
        call.associateLines(toAssertionChain());
    }

    void by(ExpectedDependency dependency) {
        dependency.associateLines(toAssertionChain());
    }

    void by(MessageAssertionChain.Link assertion) {
        assertionChain.add(assertion);
    }

    ViolationComparisonResult evaluate(AssertionError error) {
        return assertionChain.evaluate(error);
    }

    ExpectedViolation copy() {
        return new ExpectedViolation(assertionChain.copy());
    }

    String describe() {
        return assertionChain.toString();
    }

    private ExpectedRelation.LineAssociation toAssertionChain() {
        return new ExpectedRelation.LineAssociation() {
            @Override
            public void associateIfPatternMatches(String pattern) {
                assertionChain.add(MessageAssertionChain.matchesLine(pattern));
            }

            @Override
            public void associateIfStringIsContained(String string) {
                assertionChain.add(MessageAssertionChain.containsLine(string));
            }
        };
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
            return MessageAssertionChain.containsLine("Class <%s> does not reside in a package '%s' in (%s.java:0)",
                    clazz.getName(), packageIdentifier, clazz.getSimpleName());
        }
    }

    public static ClassAssertionCreator clazz(Class<?> clazz) {
        return new ClassAssertionCreator(clazz);
    }

    @Internal
    public static class ClassAssertionCreator {
        private final Class<?> clazz;

        private ClassAssertionCreator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public MessageAssertionChain.Link havingNameMatching(String regex) {
            return MessageAssertionChain.containsLine("Class <%s> matches '%s' in (%s.java:0)",
                    clazz.getName(), regex, clazz.getSimpleName());
        }

        public MessageAssertionChain.Link havingSimpleNameContaining(String infix) {
            return MessageAssertionChain.containsLine("simple name of %s contains '%s' in (%s.java:0)",
                    clazz.getName(), infix, clazz.getSimpleName());
        }

        public MessageAssertionChain.Link beingAnInterface() {
            return MessageAssertionChain.containsLine("Class <%s> is an interface in (%s.java:0)", clazz.getName(), clazz.getSimpleName());
        }
    }
}
