package com.tngtech.archunit.core;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

public class JavaMethodCall extends JavaCall<MethodCallTarget> {
    JavaMethodCall(AccessRecord<MethodCallTarget> methodAccessRecord) {
        super(methodAccessRecord);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls method <%s>";
    }

    public static class Predicates {
        public static DescribedPredicate<JavaMethodCall> target(final DescribedPredicate<? super MethodCallTarget> predicate) {
            return new DescribedPredicate<JavaMethodCall>("target " + predicate.getDescription()) {
                @Override
                public boolean apply(JavaMethodCall input) {
                    return predicate.apply(input.getTarget());
                }
            };

        }
    }
}
