package com.tngtech.archunit.core.properties;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClassList;

import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.JavaClass.namesOf;
import static com.tngtech.archunit.core.JavaClassList.GET_NAMES;

public interface HasParameterTypes {
    JavaClassList getParameters();

    class Predicates {
        public static DescribedPredicate<HasParameterTypes> parameterTypes(final Class<?>... types) {
            return parameterTypes(namesOf(types));
        }

        public static DescribedPredicate<HasParameterTypes> parameterTypes(final String... types) {
            return parameterTypes(ImmutableList.copyOf(types));
        }

        public static DescribedPredicate<HasParameterTypes> parameterTypes(final List<String> typeNames) {
            return parameterTypes(equalTo(typeNames).onResultOf(GET_NAMES)
                    .as("[%s]", formatMethodParameterTypeNames(typeNames)));
        }

        public static DescribedPredicate<HasParameterTypes> parameterTypes(final DescribedPredicate<JavaClassList> predicate) {
            return new DescribedPredicate<HasParameterTypes>("parameter types " + predicate.getDescription()) {
                @Override
                public boolean apply(HasParameterTypes input) {
                    return predicate.apply(input.getParameters());
                }
            };
        }
    }
}
