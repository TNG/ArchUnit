package com.tngtech.archunit.core.properties;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaModifier;

public interface HasModifiers {
    Set<JavaModifier> getModifiers();

    class Predicates {
        public static DescribedPredicate<HasModifiers> modifier(final JavaModifier modifier) {
            return new DescribedPredicate<HasModifiers>("modifier " + modifier) {
                @Override
                public boolean apply(HasModifiers input) {
                    return input.getModifiers().contains(modifier);
                }
            };
        }
    }
}
