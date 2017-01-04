package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.core.ChainableFunction;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;

import static com.tngtech.archunit.core.properties.HasName.Predicates.withName;
import static com.tngtech.archunit.core.properties.HasReturnType.Functions.GET_RETURN_TYPE;

public interface HasReturnType {
    JavaClass getReturnType();

    class Predicates {
        public static DescribedPredicate<HasReturnType> withReturnType(DescribedPredicate<? super JavaClass> predicate) {
            return predicate.onResultOf(GET_RETURN_TYPE).as("with return type '%s'", predicate.getDescription());
        }

        public static DescribedPredicate<HasReturnType> withReturnType(Class<?> returnType) {
            return withReturnType(returnType.getName());
        }

        public static DescribedPredicate<HasReturnType> withReturnType(String returnTypeName) {
            return withReturnType(withName(returnTypeName)).as("with return type '%s'", returnTypeName);
        }
    }

    class Functions {
        public static final ChainableFunction<HasReturnType, JavaClass> GET_RETURN_TYPE = new ChainableFunction<HasReturnType, JavaClass>() {
            @Override
            public JavaClass apply(HasReturnType input) {
                return input.getReturnType();
            }
        };
    }
}
