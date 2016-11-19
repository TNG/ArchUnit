package com.tngtech.archunit.core;

import com.tngtech.archunit.core.TestUtils.AnotherClassWithMethodNamedMethod;
import com.tngtech.archunit.core.TestUtils.ClassWithMethodNamedMethod;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClass;

public class JavaMethodTest {
    @Test(expected = IllegalArgumentException.class)
    public void incompatible_owner_is_rejected() throws Exception {
        new JavaMethod.Builder()
                .withMethod(ClassWithMethodNamedMethod.class.getDeclaredMethod("method"))
                .build(javaClass(AnotherClassWithMethodNamedMethod.class));
    }
}