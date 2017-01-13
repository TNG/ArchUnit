package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.allClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class LayerDependencyRulesWithRunnerTest {

    @ArchTest
    public static final ArchRule services_should_not_access_controllers =
            noClasses().that().resideInPackage("..service..")
                    .should().access().classesThat().resideInPackage("..controller..");

    @ArchTest
    public static final ArchRule persistence_should_not_access_services =
            noClasses().that().resideInPackage("..persistence..")
                    .should().access().classesThat().resideInPackage("..service..");

    @ArchTest
    public static final ArchRule services_should_only_be_accessed_by_controllers_or_other_services =
            allClasses().that().resideInPackage("..service..")
                    .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");
}
