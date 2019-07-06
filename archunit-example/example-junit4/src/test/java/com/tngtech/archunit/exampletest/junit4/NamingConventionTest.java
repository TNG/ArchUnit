package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.example.layers.AbstractController;
import com.tngtech.archunit.example.layers.MyController;
import com.tngtech.archunit.example.layers.MyService;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class NamingConventionTest {

    @ArchTest
    public static ArchRule services_should_be_prefixed =
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(MyService.class)
                    .should().haveSimpleNameStartingWith("Service");

    @ArchTest
    public static ArchRule controllers_should_not_have_Gui_in_name =
            classes()
                    .that().resideInAPackage("..controller..")
                    .should().haveSimpleNameNotContaining("Gui");

    @ArchTest
    public static ArchRule controllers_should_be_suffixed =
            classes()
                    .that().resideInAPackage("..controller..")
                    .or().areAnnotatedWith(MyController.class)
                    .or().areAssignableTo(AbstractController.class)
                    .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    public static ArchRule classes_named_controller_should_be_in_a_controller_package =
            classes()
                    .that().haveSimpleNameContaining("Controller")
                    .should().resideInAPackage("..controller..");

}
