package com.tngtech.archunit.testutil.assertion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
        throw new AssertionError("No access matches " + condition);
    }

    @SafeVarargs
    public final AccessesAssertion containOnly(Condition<? super JavaAccess<?>>... conditions) {
        for (Condition<? super JavaAccess<?>> condition : conditions) {
            contain(condition);
        }
        assertThat(actualRemaining).as("Unexpected " + JavaAccess.class.getSimpleName()).isEmpty();
        return this;
    }
}
