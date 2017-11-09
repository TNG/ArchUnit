package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.service.ServiceInterface;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.exampletest.junit.LayeredArchitectureTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedAccess.accessFrom;
import static com.tngtech.archunit.junit.ExpectedAccess.callFrom;
import static com.tngtech.archunit.junit.ExpectedDependency.inheritanceFrom;
import static java.lang.System.lineSeparator;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class LayeredArchitectureIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = LayeredArchitectureIntegrationTest.class, method = "expectLayerViolations")
    public static final ArchRule layer_dependencies_are_respected =
            LayeredArchitectureTest.layer_dependencies_are_respected;

    @ArchTest
    @ExpectedViolationFrom(location = LayeredArchitectureIntegrationTest.class, method = "expectLayerViolationsWithException")
    public static final ArchRule layer_dependencies_are_respected_with_exception =
            LayeredArchitectureTest.layer_dependencies_are_respected_with_exception;

    @CalledByArchUnitIntegrationTestRunner
    static void expectLayerViolations(ExpectsViolations expectsViolations) {
        expectLayerViolationsWithException(expectsViolations);
        expectsViolations
                .by(callFrom(SomeMediator.class, "violateLayerRulesIndirectly")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(15)
                        .asDependency());
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectLayerViolationsWithException(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("Layered architecture consisting of" + lineSeparator() +
                "layer 'Controllers' ('com.tngtech.archunit.example.controller..')" + lineSeparator() +
                "layer 'Services' ('com.tngtech.archunit.example.service..')" + lineSeparator() +
                "layer 'Persistence' ('com.tngtech.archunit.example.persistence..')" + lineSeparator() +
                "where layer 'Controllers' may not be accessed by any layer" + lineSeparator() +
                "where layer 'Services' may only be accessed by layers ['Controllers']" + lineSeparator() +
                "where layer 'Persistence' may only be accessed by layers ['Services']")

                .by(inheritanceFrom(DaoCallingService.class)
                        .implementing(ServiceInterface.class))

                .by(callFrom(DaoCallingService.class, "violateLayerRules")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(14)
                        .asDependency())

                .by(callFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(12)
                        .asDependency())

                .by(callFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .toMethod(UseCaseTwoController.class, "doSomethingTwo")
                        .inLine(13)
                        .asDependency())

                .by(accessFrom(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .getting().field(UseCaseOneTwoController.class, "someString")
                        .inLine(11)
                        .asDependency());
    }
}
