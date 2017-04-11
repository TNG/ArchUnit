package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RETURN_TYPE;

public interface HasReturnType {
    @PublicAPI(usage = ACCESS)
    JavaClass getReturnType();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> returnType(DescribedPredicate<? super JavaClass> predicate) {
            return predicate.onResultOf(GET_RETURN_TYPE).as("return type '%s'", predicate.getDescription());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> returnType(Class<?> returnType) {
            return returnType(returnType.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> returnType(String returnTypeName) {
            return returnType(name(returnTypeName)).as("return type '%s'", returnTypeName);
        }
    }

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasReturnType, JavaClass> GET_RETURN_TYPE = new ChainableFunction<HasReturnType, JavaClass>() {
            @Override
            public JavaClass apply(HasReturnType input) {
                return input.getReturnType();
            }
        };
    }
}
