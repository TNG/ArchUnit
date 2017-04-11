package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasType {
    @PublicAPI(usage = ACCESS)
    JavaClass getType();

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasType, JavaClass> GET_TYPE = new ChainableFunction<HasType, JavaClass>() {
            @Override
            public JavaClass apply(HasType input) {
                return input.getType();
            }
        };
    }
}
