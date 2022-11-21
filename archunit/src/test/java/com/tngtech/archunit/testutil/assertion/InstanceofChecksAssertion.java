package com.tngtech.archunit.testutil.assertion;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class InstanceofChecksAssertion extends AbstractIterableAssert<InstanceofChecksAssertion, Set<InstanceofCheck>, InstanceofCheck, InstanceofChecksAssertion.InstanceofCheckAssertion> {
    public InstanceofChecksAssertion(Set<InstanceofCheck> instanceofChecks) {
        super(instanceofChecks, InstanceofChecksAssertion.class);
    }

    @Override
    protected InstanceofCheckAssertion toAssert(InstanceofCheck value, String description) {
        return new InstanceofCheckAssertion(value).as(description);
    }

    @Override
    protected InstanceofChecksAssertion newAbstractIterableAssert(Iterable<? extends InstanceofCheck> iterable) {
        return new InstanceofChecksAssertion(ImmutableSet.copyOf(iterable));
    }

    public void containInstanceofChecks(ExpectedInstanceofCheck... expectedInstanceofChecks) {
        containInstanceofChecks(ImmutableList.copyOf(expectedInstanceofChecks));
    }

    public void containInstanceofChecks(Iterable<ExpectedInstanceofCheck> expectedInstanceofChecks) {
        Set<ExpectedInstanceofCheck> unmatchedClassObjects = stream(expectedInstanceofChecks.spliterator(), false)
                .filter(expected -> actual.stream().noneMatch(expected))
                .collect(toSet());
        assertThat(unmatchedClassObjects).as("Instanceof checks not contained in " + actual).isEmpty();
    }

    static class InstanceofCheckAssertion extends AbstractObjectAssert<InstanceofCheckAssertion, InstanceofCheck> {
        InstanceofCheckAssertion(InstanceofCheck instanceofCheck) {
            super(instanceofCheck, InstanceofCheckAssertion.class);
        }
    }

    public static ExpectedInstanceofCheck instanceofCheck(Class<?> type, int lineNumber) {
        return new ExpectedInstanceofCheck(type, lineNumber);
    }

    public static class ExpectedInstanceofCheck implements Predicate<InstanceofCheck> {
        private final Class<?> type;
        private final int lineNumber;
        private final boolean declaredInLambda;

        private ExpectedInstanceofCheck(Class<?> type, int lineNumber) {
            this(type, lineNumber, false);
        }

        private ExpectedInstanceofCheck(Class<?> type, int lineNumber, boolean declaredInLambda) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.declaredInLambda = declaredInLambda;
        }

        public ExpectedInstanceofCheck declaredInLambda() {
            return new ExpectedInstanceofCheck(type, lineNumber, true);
        }

        @Override
        public boolean test(InstanceofCheck input) {
            return input.getRawType().isEquivalentTo(type) && input.getLineNumber() == lineNumber && input.isDeclaredInLambda() == declaredInLambda;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("type", type)
                    .add("lineNumber", lineNumber)
                    .add("declaredInLambda", declaredInLambda)
                    .toString();
        }
    }
}
