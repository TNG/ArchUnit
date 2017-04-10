package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.JavaClass;

public interface HasType {
    JavaClass getType();

    class Functions {
        public static final ChainableFunction<HasType, JavaClass> GET_TYPE = new ChainableFunction<HasType, JavaClass>() {
            @Override
            public JavaClass apply(HasType input) {
                return input.getType();
            }
        };
    }
}
