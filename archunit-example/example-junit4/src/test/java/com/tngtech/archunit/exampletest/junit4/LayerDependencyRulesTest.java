package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class LayerDependencyRulesTest {

    // 'access' catches only violations by real accesses, i.e. accessing a field, calling a method; compare 'dependOn' further down

    @ArchTest
    public static final ArchRule services_should_not_access_controllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().accessClassesThat().resideInAPackage("..controller..");

    @ArchTest
    public static final ArchRule persistence_should_not_access_services =
            noClasses().that().resideInAPackage("..persistence..")
                    .should().accessClassesThat().resideInAPackage("..service..");

    @ArchTest
    public static final ArchRule services_should_only_be_accessed_by_controllers_or_other_services =
            classes().that().resideInAPackage("..service..")
                    .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");

    @ArchTest
    public static final ArchRule services_should_only_access_persistence_or_other_services =
            classes().that().resideInAPackage("..service..")
                    .should().onlyAccessClassesThat().resideInAnyPackage("..service..", "..persistence..", "java..");

    // 'dependOn' catches a wider variety of violations, e.g. having fields of type, having method parameters of type, extending type ...

    @ArchTest
    public static final ArchRule services_should_not_depend_on_controllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    public static final ArchRule persistence_should_not_depend_on_services =
            noClasses().that().resideInAPackage("..persistence..")
                    .should().dependOnClassesThat().resideInAPackage("..service..");

    @ArchTest
    public static final ArchRule services_should_only_be_depended_on_by_controllers_or_other_services =
            classes().that().resideInAPackage("..service..")
                    .should().onlyHaveDependentClassesThat().resideInAnyPackage("..controller..", "..service..");

    @ArchTest
    public static final ArchRule services_should_only_depend_on_persistence_or_other_services =
            classes().that().resideInAPackage("..service..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage("..service..", "..persistence..", "java..", "javax..");

}
