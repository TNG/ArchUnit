package com.tngtech.archunit.core;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.AccessTarget.CodeUnitCallTarget;

public abstract class JavaCall<T extends CodeUnitCallTarget> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }

    public static class Predicates {
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
