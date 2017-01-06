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
        public static DescribedPredicate<HasParameterTypes> withParameterTypes(final Class<?>... types) {
            return withParameterTypes(namesOf(types));
        }

        public static DescribedPredicate<HasParameterTypes> withParameterTypes(final String... types) {
            return withParameterTypes(ImmutableList.copyOf(types));
        }

        public static DescribedPredicate<HasParameterTypes> withParameterTypes(final List<String> typeNames) {
            return withParameterTypes(equalTo(typeNames).onResultOf(GET_NAMES)
                    .as("[%s]", formatMethodParameterTypeNames(typeNames)));
        }

        public static DescribedPredicate<HasParameterTypes> withParameterTypes(final DescribedPredicate<JavaClassList> predicate) {
            return new DescribedPredicate<HasParameterTypes>("with parameter types " + predicate.getDescription()) {
                @Override
                public boolean apply(HasParameterTypes input) {
                    return predicate.apply(input.getParameters());
                }
            };
        }
    }
}
