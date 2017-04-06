package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;

import static com.tngtech.archunit.core.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.properties.HasReturnType.Functions.GET_RETURN_TYPE;

public interface HasReturnType {
    JavaClass getReturnType();

    class Predicates {
        public static DescribedPredicate<HasReturnType> returnType(DescribedPredicate<? super JavaClass> predicate) {
            return predicate.onResultOf(GET_RETURN_TYPE).as("return type '%s'", predicate.getDescription());
        }

        public static DescribedPredicate<HasReturnType> returnType(Class<?> returnType) {
            return returnType(returnType.getName());
        }

        public static DescribedPredicate<HasReturnType> returnType(String returnTypeName) {
            return returnType(name(returnTypeName)).as("return type '%s'", returnTypeName);
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
