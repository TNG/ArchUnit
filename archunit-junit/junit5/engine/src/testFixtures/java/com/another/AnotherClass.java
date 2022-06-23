package com.another;

import java.io.Serializable;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class AnotherClass {

    @ArchTest
    public static final ArchRule someField = classes().that().implement(Serializable.class)
            .should().implement(Serializable.class);

    @ArchTest
    public static final ArchRule anotherField = classes().that().implement(Serializable.class)
            .should().implement(Serializable.class);
}
