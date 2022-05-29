package com.tngtech.archunit.testutil.assertion;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.ThrowsClause;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ThrowsClauseAssertion extends AbstractObjectAssert<ThrowsClauseAssertion, ThrowsClause<?>> {
    public ThrowsClauseAssertion(ThrowsClause<?> throwsClause) {
        super(throwsClause, ThrowsClauseAssertion.class);
    }

    public void matches(Class<?>... classes) {
        assertThat(actual).as("ThrowsClause").hasSize(classes.length);
        for (int i = 0; i < actual.size(); i++) {
            assertThat(Iterables.get(actual, i)).as("Element %d", i).matches(classes[i]);
        }
    }

    public void isEmpty() {
        assertThat(actual).as("ThrowsClause").isEmpty();
    }
}
