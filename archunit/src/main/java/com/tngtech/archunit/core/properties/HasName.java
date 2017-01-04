package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.core.Function;

public interface HasName {
    String getName();

    interface AndFullName extends HasName {
        String getFullName();
    }

    class Functions {
        public static final Function<HasName, String> GET_NAME = new Function<HasName, String>() {
            @Override
            public String apply(HasName input) {
                return input.getName();
            }
        };
    }
}
