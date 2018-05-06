package com.tngtech.archunit.integration.junit4;

import com.tngtech.archunit.example.AbstractController;
import com.tngtech.archunit.example.MyController;
import com.tngtech.archunit.example.MyService;
import com.tngtech.archunit.example.controller.SomeGuiController;
import com.tngtech.archunit.example.controller.SomeUtility;
import com.tngtech.archunit.example.controller.WronglyAnnotated;
import com.tngtech.archunit.example.service.impl.WronglyNamedSvc;
import com.tngtech.archunit.example.web.AnnotatedController;
import com.tngtech.archunit.example.web.InheritedControllerImpl;
import com.tngtech.archunit.exampletest.junit4.NamingConventionTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedLocation.javaClass;
import static com.tngtech.archunit.junit.ExpectedNaming.simpleNameOf;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class NamingConventionIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = NamingConventionIntegrationTest.class, method = "expectPrefixedWithServiceViolation")
    public static ArchRule services_should_be_prefixed =
            NamingConventionTest.services_should_be_prefixed;

    @CalledByArchUnitIntegrationTestRunner
    static void expectPrefixedWithServiceViolation(ExpectsViolations expectViolations) {
        String expectedRuleText = String.format(
                "classes that reside in a package '..service..' "
                        + "and are annotated with @%s "
                        + "should have simple name starting with 'Service'",
                MyService.class.getSimpleName());

        expectViolations.ofRule(expectedRuleText)
                .by(simpleNameOf(WronglyNamedSvc.class).notStartingWith("Service"));
    }

    @ArchTest
    @ExpectedViolationFrom(location = NamingConventionIntegrationTest.class, method = "expectContainsGuiViolation")
    public static ArchRule controllers_should_not_have_Gui_in_name =
            NamingConventionTest.controllers_should_not_have_Gui_in_name;

    @CalledByArchUnitIntegrationTestRunner
    static void expectContainsGuiViolation(ExpectsViolations expectViolations) {
        expectViolations.ofRule("classes that reside in a package '..controller..' should have simple name not containing 'Gui'")
                .by(simpleNameOf(SomeGuiController.class).containing("Gui"));
    }

    @ArchTest
    @ExpectedViolationFrom(location = NamingConventionIntegrationTest.class, method = "expectSuffixedWithControllerViolation")
    public static ArchRule controllers_should_be_suffixed =
            NamingConventionTest.controllers_should_be_suffixed;

    @CalledByArchUnitIntegrationTestRunner
    static void expectSuffixedWithControllerViolation(ExpectsViolations expectViolations) {
        String expectedRuleText = String.format(
                "classes that reside in a package '..controller..' "
                        + "or are annotated with @%s "
                        + "or are assignable to %s "
                        + "should have simple name ending with 'Controller'",
                MyController.class.getSimpleName(), AbstractController.class.getName());

        expectViolations.ofRule(expectedRuleText)
                .by(simpleNameOf(InheritedControllerImpl.class).notEndingWith("Controller"))
                .by(simpleNameOf(SomeUtility.class).notEndingWith("Controller"))
                .by(simpleNameOf(WronglyAnnotated.class).notEndingWith("Controller"));
    }

    @ArchTest
    @ExpectedViolationFrom(location = NamingConventionIntegrationTest.class, method = "expectControllerPackageViolation")
    public static ArchRule classes_named_controller_should_be_in_a_controller_package =
            NamingConventionTest.classes_named_controller_should_be_in_a_controller_package;

    @CalledByArchUnitIntegrationTestRunner
    static void expectControllerPackageViolation(ExpectsViolations expectViolations) {
        expectViolations
                .ofRule("classes that have simple name containing 'Controller' should reside in a package '..controller..'")
                .by(javaClass(AbstractController.class).notResidingIn("..controller.."))
                .by(javaClass(AnnotatedController.class).notResidingIn("..controller.."))
                .by(javaClass(InheritedControllerImpl.class).notResidingIn("..controller.."))
                .by(javaClass(MyController.class).notResidingIn("..controller.."));
    }
}
