package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.controller.one.UseCaseOneController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.exampletest.junit.LayeredArchitectureTest;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedViolation.from;
import static java.lang.System.lineSeparator;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class LayeredArchitectureIntegrationTest {
    @ArchTest
    @ExpectedViolationFrom(location = LayeredArchitectureIntegrationTest.class, method = "expectLayerViolations")
    public static final ArchRule layer_dependencies_are_respected = LayeredArchitectureTest.layer_dependencies_are_respected;

    static void expectLayerViolations(ExpectedViolation expectViolation) {
        expectViolation.ofRule("Layered architecture consisting of" + lineSeparator() +
                "layer 'Controllers' ('com.tngtech.archunit.example.controller..')" + lineSeparator() +
                "layer 'Services' ('com.tngtech.archunit.example.service..')" + lineSeparator() +
                "layer 'Persistence' ('com.tngtech.archunit.example.persistence..')" + lineSeparator() +
                "where layer 'Controllers' may not be accessed by any layer" + lineSeparator() +
                "where layer 'Services' may only be accessed by layers ['Controllers']" + lineSeparator() +
                "where layer 'Persistence' may only be accessed by layers ['Services']")

                .byCall(from(DaoCallingService.class, "violateLayerRules")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(13))

                .byCall(from(SomeMediator.class, "violateLayerRulesIndirectly")
                        .toMethod(ServiceViolatingLayerRules.class, "doSomething")
                        .inLine(15))

                .byCall(from(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(12))

                .byCall(from(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .toMethod(UseCaseTwoController.class, "doSomethingTwo")
                        .inLine(13))

                .byAccess(from(ServiceViolatingLayerRules.class, "illegalAccessToController")
                        .getting().field(UseCaseOneController.class, "someString")
                        .inLine(11));
    }
}
