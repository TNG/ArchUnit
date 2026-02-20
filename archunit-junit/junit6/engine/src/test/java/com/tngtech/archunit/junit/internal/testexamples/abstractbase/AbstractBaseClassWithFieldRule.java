package com.tngtech.archunit.junit.internal.testexamples.abstractbase;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.internal.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.internal.testexamples.UnwantedClass;
import com.tngtech.archunit.lang.ArchRule;

public abstract class AbstractBaseClassWithFieldRule {
    public static final String INSTANCE_FIELD_NAME = "abstractBaseClassInstanceField";

    @ArchTest
    ArchRule abstractBaseClassInstanceField = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);
}
