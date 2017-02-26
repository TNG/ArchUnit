package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class LayerDependencyRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingCodingRules.class);
    }

    @Ignore
    @Test
    public void services_should_not_access_controllers() {
        noClasses().that().resideInPackage("..service..")
                .should().access().classesThat().resideInPackage("..controller..").check(classes);
    }

    @Ignore
    @Test
    public void persistence_should_not_access_services() {
        noClasses().that().resideInPackage("..persistence..")
                .should().access().classesThat().resideInPackage("..service..").check(classes);
    }

    @Ignore
    @Test
    public void services_should_only_be_accessed_by_controllers_or_other_services() {
        classes().that().resideInPackage("..service..")
                .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..").check(classes);
    }
}
