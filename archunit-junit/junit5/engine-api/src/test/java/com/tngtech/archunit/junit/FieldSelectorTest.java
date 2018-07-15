package com.tngtech.archunit.junit;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldSelectorTest {

    @Test
    void select_simple_field() {
        FieldSelector selector = FieldSelector.selectField(SomeParentTest.class.getName(), "someRule");

        assertThat(selector.getJavaClass()).isEqualTo(SomeParentTest.class);
        assertThat(selector.getJavaField().getName()).isEqualTo("someRule");
    }

    @Test
    void select_parent_field_in_child() {
        FieldSelector selector = FieldSelector.selectField(SomeChildTest.class.getName(), "someRule");

        assertThat(selector.getJavaClass()).isEqualTo(SomeChildTest.class);
        assertThat(selector.getJavaField().getName()).isEqualTo("someRule");
        assertThat(selector.getJavaField().getDeclaringClass()).isEqualTo(SomeParentTest.class);
    }

    @Test
    void select_shadowed_field() {
        FieldSelector selector = FieldSelector.selectField(SomeChildTest.class.getName(), "someShadowedRule");

        assertThat(selector.getJavaClass()).isEqualTo(SomeChildTest.class);
        assertThat(selector.getJavaField().getName()).isEqualTo("someShadowedRule");
        assertThat(selector.getJavaField().getDeclaringClass()).isEqualTo(SomeChildTest.class);
    }

    static class SomeParentTest {
        @ArchTest
        static final ArchRule someRule = null;
        @ArchTest
        static final ArchRule someShadowedRule = null;
    }

    static class SomeChildTest extends SomeParentTest {
        @ArchTest
        static final ArchRule someShadowedRule = null;
    }
}
