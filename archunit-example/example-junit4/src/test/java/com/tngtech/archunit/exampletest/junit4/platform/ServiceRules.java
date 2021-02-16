package com.tngtech.archunit.exampletest.junit4.platform;

import com.tngtech.archunit.example.layers.MyService;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Shared rule, to be referenced in tests of "projectA" and "projectB".
 */
public class ServiceRules {
    @ArchTest
    public static ArchRule services_should_be_prefixed =
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(MyService.class)
                    .should().haveSimpleNameStartingWith("Service");
}
