package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.TypeCast;

import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class TypeCastsAssertion extends AbstractIterableAssert<TypeCastsAssertion, Set<TypeCast>, TypeCast, TypeCastsAssertion.TypeCastAssertion> {
    public TypeCastsAssertion(Set<TypeCast> typeCasts) {
        super(typeCasts, TypeCastsAssertion.class);
    }

    @Override
    protected TypeCastAssertion toAssert(TypeCast value, String description) {
        return new TypeCastAssertion(value).as(description);
    }

    @Override
    protected TypeCastsAssertion newAbstractIterableAssert(Iterable<? extends TypeCast> iterable) {
        return new TypeCastsAssertion(ImmutableSet.copyOf(iterable));
    }

    public void containTypeCasts(ExpectedTypeCast... expectedTypeCasts) {
        containTypeCasts(List.of(expectedTypeCasts));
    }

    public void containTypeCasts(Iterable<ExpectedTypeCast> expectedTypeCasts) {
        Set<ExpectedTypeCast> unmatchedClassObjects = stream(expectedTypeCasts.spliterator(), false)
                .filter(expected -> actual.stream().noneMatch(expected))
                .collect(toSet());
        assertThat(unmatchedClassObjects).as("Type cast not contained in %s", actual).isEmpty();
    }

    static class TypeCastAssertion extends AbstractObjectAssert<TypeCastAssertion, TypeCast> {
        TypeCastAssertion(TypeCast typeCast) {
            super(typeCast, TypeCastAssertion.class);
        }
    }

    public static ExpectedTypeCast typeCast(Class<?> type, int lineNumber) {
        return new ExpectedTypeCast(type, lineNumber);
    }

    public static class ExpectedTypeCast implements Predicate<TypeCast> {
        private final Class<?> type;
        private final int lineNumber;
        private final boolean declaredInLambda;

        private ExpectedTypeCast(Class<?> type, int lineNumber) {
            this(type, lineNumber, false);
        }

        private ExpectedTypeCast(Class<?> type, int lineNumber, boolean declaredInLambda) {
            this.type = type;
            this.lineNumber = lineNumber;
            this.declaredInLambda = declaredInLambda;
        }

        public ExpectedTypeCast declaredInLambda() {
            return new ExpectedTypeCast(type, lineNumber, true);
        }

        @Override
        public boolean test(TypeCast input) {
            return input.getRawType().isEquivalentTo(type) 
                    && input.getLineNumber() == lineNumber
                    && input.isDeclaredInLambda() == declaredInLambda;
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
