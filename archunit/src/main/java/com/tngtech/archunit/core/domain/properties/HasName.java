package com.tngtech.archunit.core.domain.properties;

import java.util.regex.Pattern;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasName {
    @PublicAPI(usage = ACCESS)
    String getName();

    interface AndFullName extends HasName {
        @PublicAPI(usage = ACCESS)
        String getFullName();
    }

    final class Predicates {
        private Predicates() {
        }

        /**
         * Matches names against a regular expression.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameMatching(final String regex) {
            final Pattern pattern = Pattern.compile(regex);
            return new DescribedPredicate<HasName>(String.format("name matching '%s'", regex)) {
                @Override
                public boolean apply(HasName input) {
                    return pattern.matcher(input.getName()).matches();
                }
            };
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> name(final String name) {
            return new DescribedPredicate<HasName>(String.format("name '%s'", name)) {
                @Override
                public boolean apply(HasName input) {
                    return input.getName().equals(name);
                }
            };
        }
    }

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasName, String> GET_NAME = new ChainableFunction<HasName, String>() {
            @Override
            public String apply(HasName input) {
                return input.getName();
            }
        };
    }
}
