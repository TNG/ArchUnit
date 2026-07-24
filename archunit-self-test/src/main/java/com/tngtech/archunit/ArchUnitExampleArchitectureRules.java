package com.tngtech.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchUnitExampleArchitectureRules {
    @ArchTest
    public static final ArchRule examples_should_be_independent_of_Guava =
            noClasses().should().accessClassesThat().resideInAnyPackage("..google..")
                    .because("we want to keep the dependencies of archunit-example minimal");
}
