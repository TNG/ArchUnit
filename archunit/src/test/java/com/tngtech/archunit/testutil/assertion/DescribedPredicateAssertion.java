package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.base.DescribedPredicate;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class DescribedPredicateAssertion<T> extends AbstractObjectAssert<DescribedPredicateAssertion<T>, DescribedPredicate<T>> {

    public DescribedPredicateAssertion(DescribedPredicate<T> predicate) {
        super(predicate, DescribedPredicateAssertion.class);
    }

    public DescribedPredicateAssertion<T> accepts(T value) {
        assertThatPredicateAppliesTo(value).isTrue();
        return this;
    }

    public DescribedPredicateAssertion<T> rejects(T value) {
        assertThatPredicateAppliesTo(value).isFalse();
        return this;
    }

    private AbstractBooleanAssert<?> assertThatPredicateAppliesTo(T value) {
        return assertThat(actual.apply(value)).as("predicate <%s> matches <%s>", actual, value);
    }

    public DescribedPredicateAssertion<T> hasSameDescriptionAs(DescribedPredicate<T> otherPredicate) {
        return hasDescription(otherPredicate.getDescription());
    }

    public DescribedPredicateAssertion<T> hasDescription(String description) {
        assertThat(actual.getDescription()).as("description of predicate <%s>", actual).isEqualTo(description);
        return this;
    }
}
