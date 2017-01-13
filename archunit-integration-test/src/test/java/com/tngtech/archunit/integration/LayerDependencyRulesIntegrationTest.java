package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.controller.one.UseCaseOneController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.exampletest.LayerDependencyRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.example.SomeMediator.violateLayerRulesIndirectly;
import static com.tngtech.archunit.example.controller.one.UseCaseOneController.someString;
import static com.tngtech.archunit.example.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService.violateLayerRules;
import static com.tngtech.archunit.example.service.ServiceViolatingLayerRules.illegalAccessToController;
import static com.tngtech.archunit.junit.ExpectedViolation.from;

public class LayerDependencyRulesIntegrationTest extends LayerDependencyRulesTest {

    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void services_should_not_access_controllers() {
        expectViolationByAccessFromServiceToController(expectViolation);

        super.services_should_not_access_controllers();
    }

    static void expectViolationByAccessFromServiceToController(ExpectedViolation expectViolation) {
        expectViolation.ofRule("no classes that reside in package '..service..' " +
                "should access classes that reside in package '..controller..'")
                .byAccess(from(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneController.class, someString)
                        .inLine(11))
                .byCall(from(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toConstructor(UseCaseTwoController.class)
                        .inLine(12))
                .byCall(from(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .toMethod(UseCaseTwoController.class, doSomethingTwo)
                        .inLine(13));
    }

    @Test
    @Override
    public void persistence_should_not_access_services() {
        expectViolationByAccessFromPersistenceToService(expectViolation);

        super.persistence_should_not_access_services();
    }

    static void expectViolationByAccessFromPersistenceToService(ExpectedViolation expectViolation) {
        expectViolation.ofRule("no classes that reside in package '..persistence..' should " +
                "access classes that reside in package '..service..'")
                .byCall(from(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(13));
    }

    @Test
    @Override
    public void services_should_only_be_accessed_by_controllers_or_other_services() {
        expectViolationByIllegalAccessToService(expectViolation);

        super.services_should_only_be_accessed_by_controllers_or_other_services();
    }

    static void expectViolationByIllegalAccessToService(ExpectedViolation expectViolation) {
        expectViolation.ofRule("classes that reside in package '..service..' should " +
                "only be accessed by any package ['..controller..', '..service..']")
                .byCall(from(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(13))
                .byCall(from(SomeMediator.class, violateLayerRulesIndirectly)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(15));
    }
}
