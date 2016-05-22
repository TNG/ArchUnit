package com.tngtech.archunit.core;

import com.tngtech.archunit.core.TestUtils.AnotherClassWithMethodNamedMethod;
import com.tngtech.archunit.core.TestUtils.ClassWithMethodNamedMethod;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMethodTest {

    @Test
    public void equals_works() throws Exception {
        JavaMethod method = javaMethod("method", ClassWithMethodNamedMethod.class);
        JavaMethod equalMethod = javaMethod("method", ClassWithMethodNamedMethod.class);
        JavaMethod differentMethod = javaMethod("method", AnotherClassWithMethodNamedMethod.class);

        assertThat(method).isEqualTo(method);
        assertThat(method).isEqualTo(equalMethod);
        assertThat(method.getOwner()).isEqualTo(equalMethod.getOwner());

        assertThat(method.getName()).isEqualTo(differentMethod.getName());
        assertThat(method.getParameters()).isEqualTo(differentMethod.getParameters());
        assertThat(method.getReturnType()).isEqualTo(differentMethod.getReturnType());
        assertThat(method).isNotEqualTo(differentMethod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompatible_owner_is_rejected() throws Exception {
        new JavaMethod.Builder()
                .withMethod(ClassWithMethodNamedMethod.class.getDeclaredMethod("method"))
                .build(javaClass(AnotherClassWithMethodNamedMethod.class));
    }
}