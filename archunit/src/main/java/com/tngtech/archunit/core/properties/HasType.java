package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.JavaClass;

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
