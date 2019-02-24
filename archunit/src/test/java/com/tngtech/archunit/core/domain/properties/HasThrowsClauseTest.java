package com.tngtech.archunit.core.domain.properties;

import java.io.IOException;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.ThrowsClause;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.throwsClause;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class HasThrowsClauseTest {
    @Test
    public void predicate_throwsClauseWithTypes_by_type() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class, SecondException.class).apply(hasThrowsClause))
                .as("predicate matches")
                .isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(SecondException.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Object.class).apply(hasThrowsClause)).as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class, SecondException.class).getDescription())
                .isEqualTo(String.format("throws types [%s, %s]", FirstException.class.getName(), SecondException.class.getName()));
    }

    @Test
    public void predicate_throwsClauseWithTypes_by_type_name() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class.getName(), SecondException.class.getName())
                .apply(hasThrowsClause))
                .as("predicate matches").isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(SecondException.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Object.class.getName()).apply(hasThrowsClause))
                .as("predicate matches").isFalse();
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class.getName(), SecondException.class.getName()).getDescription())
                .isEqualTo(String.format("throws types [%s, %s]", FirstException.class.getName(), SecondException.class.getName()));
    }

    @DataProvider
    public static Object[][] containing_type_cases() {
        return testForEach(
                throwsClauseContainingType(FirstException.class),
                throwsClauseContainingType(FirstException.class.getName()),
                throwsClauseContainingType(equivalentTo(FirstException.class).as(FirstException.class.getName()))
        );
    }

    @Test
    @UseDataProvider("containing_type_cases")
    public void predicate_containing_type(DescribedPredicate<HasThrowsClause<?>> predicate) {
        assertThat(predicate.apply(newHasThrowsClause(FirstException.class, SecondException.class)))
                .as("predicate matches").isTrue();

        assertThat(predicate.apply(newHasThrowsClause(IOException.class, SecondException.class)))
                .as("predicate matches").isFalse();

        assertThat(predicate.getDescription()).as("predicate description")
                .isEqualTo("throws clause containing type " + FirstException.class.getName());
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysTrue()).apply(hasThrowsClause)).isTrue();
        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysFalse()).apply(hasThrowsClause)).isFalse();

        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysFalse().as("some text")).getDescription())
                .isEqualTo("throws types some text");
    }

    @SafeVarargs
    private final HasThrowsClause<?> newHasThrowsClause(final Class<? extends Throwable>... throwsDeclarations) {
        return new HasThrowsClause() {
            @Override
            public ThrowsClause<?> getThrowsClause() {
                return throwsClause(throwsDeclarations);
            }
        };
    }

    private static class FirstException extends Exception {
    }

    private static class SecondException extends Exception {
    }
}
