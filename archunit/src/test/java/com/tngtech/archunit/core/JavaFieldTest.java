package com.tngtech.archunit.core;

import com.tngtech.archunit.core.TestUtils.AnotherClassWithFieldNamedValue;
import com.tngtech.archunit.core.TestUtils.ClassWithFieldNamedValue;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaField;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaFieldTest {

    @Test
    public void equals_works() throws Exception {
        JavaField field = javaField("value", ClassWithFieldNamedValue.class);
        JavaField equalField = javaField("value", ClassWithFieldNamedValue.class);
        JavaField differentField = javaField("value", AnotherClassWithFieldNamedValue.class);

        assertThat(field).isEqualTo(field);
        assertThat(field).isEqualTo(equalField);
        assertThat(field.getOwner()).isEqualTo(equalField.getOwner());

        assertThat(field.getName()).isEqualTo(differentField.getName());
        assertThat(field.getType()).isEqualTo(differentField.getType());
        assertThat(field).isNotEqualTo(differentField);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompatible_owner_is_rejected() throws Exception {
        new JavaField.Builder()
                .withField(ClassWithFieldNamedValue.class.getDeclaredField("value"))
                .build(javaClass(AnotherClassWithFieldNamedValue.class));
    }
}
