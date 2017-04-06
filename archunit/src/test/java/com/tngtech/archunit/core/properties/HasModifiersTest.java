package com.tngtech.archunit.core.properties;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.JavaModifier;
import org.junit.Test;

import static com.tngtech.archunit.core.properties.HasModifiers.Predicates.modifier;
import static org.assertj.core.api.Assertions.assertThat;

public class HasModifiersTest {
    @Test
    public void modifier_predicate() {
        assertThat(modifier(JavaModifier.PRIVATE).apply(hasModifiers(JavaModifier.PRIVATE, JavaModifier.STATIC)))
                .as("Predicate matches").isTrue();
        assertThat(modifier(JavaModifier.PRIVATE).apply(hasModifiers(JavaModifier.PUBLIC, JavaModifier.STATIC)))
                .as("Predicate matches").isFalse();
        assertThat(modifier(JavaModifier.PRIVATE).getDescription()).isEqualTo("modifier PRIVATE");
    }

    private static HasModifiers hasModifiers(final JavaModifier... modifiers) {
        return new HasModifiers() {
            @Override
            public Set<JavaModifier> getModifiers() {
                return ImmutableSet.copyOf(modifiers);
            }
        };
    }
}