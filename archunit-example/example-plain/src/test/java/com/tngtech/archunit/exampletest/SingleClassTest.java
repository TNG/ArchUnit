package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.ClassViolatingCodingRules;
import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.layers.core.CoreSatellite;
import com.tngtech.archunit.example.layers.core.HighSecurity;
import com.tngtech.archunit.example.layers.core.VeryCentralCore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClass;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.theClass;

@Category(Example.class)
public class SingleClassTest {
    private static final JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassViolatingCodingRules.class);

    @Test
    public void core_should_only_be_accessed_by_satellites() {
        theClass(VeryCentralCore.class)
                .should().onlyBeAccessed().byClassesThat().implement(CoreSatellite.class)
                .check(classes);
    }

    @Test
    public void core_should_only_access_classes_in_core_itself() {
        noClass(VeryCentralCore.class)
                .should().accessClassesThat().resideOutsideOfPackages("..core..", "java..")
                .check(classes);
    }

    @Test
    public void the_only_class_with_high_security_is_central_core() {
        classes()
                .that().areAnnotatedWith(HighSecurity.class)
                .should().be(VeryCentralCore.class)
                .check(classes);
    }

    @Test
    public void central_core_should_not_implement_some_business_interface() {
        classes()
                .that().implement(SomeOtherBusinessInterface.class)
                .should().notBe(VeryCentralCore.class)
                .check(classes);
    }
}
