package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAccessBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public abstract class JavaCall<T extends CodeUnitCallTarget> extends JavaAccess<T> {
    JavaCall(JavaAccessBuilder<T, ?> builder) {
        super(builder);
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaCall<?>> target(final DescribedPredicate<? super CodeUnitCallTarget> predicate) {
            return new DescribedPredicate<JavaCall<?>>("target " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaCall<?> input) {
                    return predicate.apply(input.getTarget());
                }
            };
        }
    }
}
