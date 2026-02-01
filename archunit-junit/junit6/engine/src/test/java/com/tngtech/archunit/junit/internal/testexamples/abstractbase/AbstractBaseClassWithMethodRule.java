package com.tngtech.archunit.junit.internal.testexamples.abstractbase;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.internal.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.internal.testexamples.UnwantedClass;

public abstract class AbstractBaseClassWithMethodRule {
    public static final String INSTANCE_METHOD_NAME = "abstractBaseClassInstanceMethod";

    @ArchTest
    void abstractBaseClassInstanceMethod(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }
}
