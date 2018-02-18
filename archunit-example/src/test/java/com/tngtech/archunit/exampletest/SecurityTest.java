package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class SecurityTest {
    @Test
    public void only_security_infrastructure_should_use_java_security() {
        ArchRule rule = classes().that().resideInAPackage("java.security..")
                .should().onlyBeAccessed().byAnyPackage("..example.security..", "java.security..")
                .because("we want to have one isolated cross-cutting concern 'security'");

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example", "java.security");

        rule.check(classes);
    }
}
