package com.tngtech.archunit.testutil.assertion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import org.assertj.core.api.Condition;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessesAssertion {
    private final Set<JavaAccess<?>> actualRemaining;

    public AccessesAssertion(Collection<JavaAccess<?>> accesses) {
        this.actualRemaining = new HashSet<>(accesses);
    }

    public AccessesAssertion contain(Condition<? super JavaAccess<?>> condition) {
        for (Iterator<JavaAccess<?>> iterator = actualRemaining.iterator(); iterator.hasNext(); ) {
            if (condition.matches(iterator.next())) {
                iterator.remove();
                return this;
            }
        }
        throw new AssertionError("No access matches " + condition + " within " + actualRemaining);
    }

    @SafeVarargs
    public final AccessesAssertion containOnly(Condition<? super JavaAccess<?>>... conditions) {
        for (Condition<? super JavaAccess<?>> condition : conditions) {
            contain(condition);
        }
        assertThat(actualRemaining).as("Unexpected " + JavaAccess.class.getSimpleName()).isEmpty();
        return this;
    }public static AccessCondition access() {
        return new AccessCondition();
    }

    public static class AccessCondition extends Condition<JavaAccess<?>> {
        private final DescribedPredicate<JavaAccess<?>> predicate;

        public AccessCondition() {
            this(DescribedPredicate.<JavaAccess<?>>alwaysTrue().as("access"));
        }

        public AccessCondition(DescribedPredicate<JavaAccess<?>> predicate) {
            super(predicate, predicate.getDescription());
            this.predicate = predicate;
        }

        public AccessCondition fromOrigin(Class<?> owner, String name) {
            DescribedPredicate<JavaAccess<?>> fromCodeUnit = DescribedPredicate.describe("", access ->
                    access.getOrigin().getOwner().isEquivalentTo(owner)
                            && access.getOrigin().getName().equals(name));
            return new AccessCondition(
                    predicate.and(fromCodeUnit)
                            .as("%s from origin %s.%s()", predicate.getDescription(), owner.getName(), name));
        }

        public AccessCondition toTarget(Class<?> owner, String name) {
            DescribedPredicate<JavaAccess<?>> toCodeUnit = DescribedPredicate.describe("", access ->
                    access.getTarget().getOwner().isEquivalentTo(owner)
                            && access.getTarget().getName().equals(name));
            return new AccessCondition(
                    predicate.and(toCodeUnit)
                            .as("%s to target %s.%s", predicate.getDescription(), owner.getName(), name));
        }

        public AccessCondition declaredInLambda() {
            return new AccessCondition(
                    predicate.and(DescribedPredicate.describe("", JavaAccess::isDeclaredInLambda))
                            .as(predicate.getDescription() + " declared in lambda"));
        }
    }
}
