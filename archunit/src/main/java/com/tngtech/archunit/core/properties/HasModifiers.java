package com.tngtech.archunit.core.properties;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaModifier;

public interface HasModifiers {
    Set<JavaModifier> getModifiers();

    class Predicates {
        public static DescribedPredicate<HasModifiers> withModifier(final JavaModifier modifier) {
            return new DescribedPredicate<HasModifiers>("with modifier " + modifier) {
                @Override
                public boolean apply(HasModifiers input) {
                    return input.getModifiers().contains(modifier);
                }
            };
        }
    }
}
