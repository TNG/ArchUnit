package com.tngtech.archunit.core.domain.properties;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaModifier;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class HasModifiersTest {
    @Test
    public void modifier_predicate() {
        assertThat(modifier(JavaModifier.PRIVATE))
                .accepts(hasModifiers(JavaModifier.PRIVATE, JavaModifier.STATIC))
                .rejects(hasModifiers(JavaModifier.PUBLIC, JavaModifier.STATIC))
                .hasDescription("modifier PRIVATE");
    }

    private static HasModifiers hasModifiers(JavaModifier... modifiers) {
        return () -> ImmutableSet.copyOf(modifiers);
    }
}
