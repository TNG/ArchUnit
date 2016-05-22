package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.example.usecase.one.UseCaseOneController;
import com.tngtech.archunit.example.usecase.two.UseCaseTwoController;
import com.tngtech.archunit.exampletest.LayerDependencyRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.example.service.ServiceViolatingLayerRules.illegalAccessToUseCase;
import static com.tngtech.archunit.example.usecase.one.UseCaseOneController.someString;
import static com.tngtech.archunit.example.usecase.two.UseCaseTwoController.doSomething;
import static com.tngtech.archunit.junit.ExpectedViolation.from;

public class LayerDependencyRulesIntegrationTest extends LayerDependencyRulesTest {

    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void services_should_not_access_usecases() {
        expectViolation.ofRule("classes that reside in '..service..' should not access classes that reside in '..usecase..'")
                .byAccess(from(ServiceViolatingLayerRules.class, illegalAccessToUseCase)
                        .getting().field(UseCaseOneController.class, someString)
                        .inLine(10))
                .byCall(from(ServiceViolatingLayerRules.class, illegalAccessToUseCase)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(11))
                .byCall(from(ServiceViolatingLayerRules.class, illegalAccessToUseCase)
                        .toMethod(UseCaseTwoController.class, doSomething)
                        .inLine(12));

        super.services_should_not_access_usecases();
    }
}
