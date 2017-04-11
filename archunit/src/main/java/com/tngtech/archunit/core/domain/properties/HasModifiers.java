package com.tngtech.archunit.core.domain.properties;

import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaModifier;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface HasModifiers {
    @PublicAPI(usage = ACCESS)
    Set<JavaModifier> getModifiers();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
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
