package com.tngtech.archunit.core.properties;

import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;

public interface HasName {
    String getName();

    interface AndFullName extends HasName {
        String getFullName();
    }

    class Predicates {
        /**
         * Matches names against a regular expression.
         */
        public static DescribedPredicate<HasName> nameMatching(final String regex) {
            final Pattern pattern = Pattern.compile(regex);
            return new DescribedPredicate<HasName>(String.format("name matching '%s'", regex)) {
                @Override
                public boolean apply(HasName input) {
                    return pattern.matcher(input.getName()).matches();
                }
            };
        }

        public static DescribedPredicate<HasName> name(final String name) {
            return new DescribedPredicate<HasName>(String.format("name '%s'", name)) {
                @Override
                public boolean apply(HasName input) {
                    return input.getName().equals(name);
                }
            };
        }
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
