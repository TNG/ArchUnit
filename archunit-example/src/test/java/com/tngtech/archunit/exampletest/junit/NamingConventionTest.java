package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.example.AbstractController;
import com.tngtech.archunit.example.SomeControllerAnnotation;
import com.tngtech.archunit.exampletest.Example;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class NamingConventionTest {

    @ArchTest
    public static void services_should_be_prefixed(JavaClasses javaClasses) {
        classes().
                that().resideInAPackage("..service..")
                .should().haveSimpleNameStartingWith("Service")
                .check(javaClasses);
    }

    @ArchTest
    public static void controllers_should_not_have_Gui_in_name(JavaClasses javaClasses) {
        classes().
                that().resideInAPackage("..controller..")
                .should().haveSimpleNameNotContaining("Gui")
                .check(javaClasses);
    }

    @ArchTest
    public static void controllers_should_be_suffixed(JavaClasses javaClasses) {
        classes().
                that().resideInAPackage("..controller..")
                .or().areAnnotatedWith(SomeControllerAnnotation.class)
                .or().areAssignableTo(AbstractController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(javaClasses);
    }
}
