package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.AbstractController;
import com.tngtech.archunit.example.MyController;
import com.tngtech.archunit.example.MyService;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class NamingConventionTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

    @Test
    public void services_should_be_prefixed() {
        classes()
                .that().resideInAPackage("..service..")
                .and().areAnnotatedWith(MyService.class)
                .should().haveSimpleNameStartingWith("Service")
                .check(classes);
    }

    @Test
    public void controllers_should_not_have_Gui_in_name() {
        classes()
                .that().resideInAPackage("..controller..")
                .should().haveSimpleNameNotContaining("Gui")
                .check(classes);
    }

    @Test
    public void controllers_should_be_suffixed() {
        classes()
                .that().resideInAPackage("..controller..")
                .or().areAnnotatedWith(MyController.class)
                .or().areAssignableTo(AbstractController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(classes);
    }

    @Test
    public void classes_named_controller_should_be_in_a_controller_package() {
        classes()
                .that().haveSimpleNameContaining("Controller")
                .should().resideInAPackage("..controller..")
                .check(classes);
    }

}
