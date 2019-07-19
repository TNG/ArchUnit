package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class SecurityTest {
    @Test
    public void only_security_infrastructure_should_use_java_security() {
        ArchRule rule = classes().that().resideInAPackage("java.security..")
                .should().onlyBeAccessed().byAnyPackage("..example.layers.security..", "java.security..")
                .because("we want to have one isolated cross-cutting concern 'security'");

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.layers", "java.security");

        rule.check(classes);
    }

    @Test
    public void only_security_infrastructure_should_use_java_security_on_whole_classpath() {
        ArchRule rule = classes().that().resideInAPackage("java.security.cert..")
                .should().onlyBeAccessed().byAnyPackage("..example.layers.security..", "java..", "..sun..", "javax..", "apple.security..");

        JavaClasses classes = new ClassFileImporter().importClasspath(onlyAppAndRuntime());

        rule.check(classes);
    }

    private ImportOptions onlyAppAndRuntime() {
        return new ImportOptions().with(new ImportOption() {
            @Override
            public boolean includes(Location location) {
                return location.contains("archunit")
                        || location.contains("/rt.jar")
                        || location.contains("java.base");
            }
        });
    }
}
