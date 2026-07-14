package com.tngtech.archunit.core.domain.properties;

import java.io.IOException;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.ThrowsClause;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.throwsClause;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasThrowsClauseTest {
    @Test
    public void predicate_throwsClauseWithTypes_by_type() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class, SecondException.class))
                .accepts(hasThrowsClause)
                .hasDescription(String.format("throws types [%s, %s]", FirstException.class.getName(), SecondException.class.getName()));
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class)).rejects(hasThrowsClause);
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(SecondException.class)).rejects(hasThrowsClause);
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(RuntimeException.class)).rejects(hasThrowsClause);
    }

    @Test
    public void predicate_throwsClauseWithTypes_by_type_name() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class.getName(), SecondException.class.getName()))
                .accepts(hasThrowsClause)
                .hasDescription(String.format("throws types [%s, %s]", FirstException.class.getName(), SecondException.class.getName()));
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(FirstException.class.getName())).rejects(hasThrowsClause);
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(SecondException.class.getName())).rejects(hasThrowsClause);
        assertThat(HasThrowsClause.Predicates.throwsClauseWithTypes(Object.class.getName())).rejects(hasThrowsClause);
    }

    static Stream<DescribedPredicate<?>> containing_type_cases() {
        return Stream.of(
                throwsClauseContainingType(FirstException.class),
                throwsClauseContainingType(FirstException.class.getName()),
                throwsClauseContainingType(equivalentTo(FirstException.class).as(FirstException.class.getName()))
        );
    }

    @ParameterizedTest
    @MethodSource("containing_type_cases")
    void predicate_containing_type(DescribedPredicate<HasThrowsClause<?>> predicate) {
        assertThat(predicate)
                .accepts(newHasThrowsClause(FirstException.class, SecondException.class))
                .rejects(newHasThrowsClause(IOException.class, SecondException.class))
                .hasDescription("throws clause containing type " + FirstException.class.getName());
    }

    @Test
    public void predicate_on_parameters_by_Predicate() {
        HasThrowsClause<?> hasThrowsClause = newHasThrowsClause(FirstException.class, SecondException.class);

        assertThat(HasThrowsClause.Predicates.throwsClause(alwaysTrue()))
                .accepts(hasThrowsClause);
        assertThat(HasThrowsClause.Predicates.throwsClause(DescribedPredicate.<ThrowsClause<?>>alwaysFalse().as("some text")))
                .rejects(hasThrowsClause)
                .hasDescription("throws types some text");
    }

    @SafeVarargs
    private final HasThrowsClause<?> newHasThrowsClause(Class<? extends Throwable>... throwsDeclarations) {
        return new HasThrowsClause() {
            @Override
            public ThrowsClause<?> getThrowsClause() {
                return throwsClause(throwsDeclarations);
            }

            @Override
            public String toString() {
                return HasThrowsClause.class.getSimpleName() + "{ throws " + formatNamesOf(throwsDeclarations) + "}";
            }
        };
    }

    private static class FirstException extends Exception {
    }

    private static class SecondException extends Exception {
    }
}
