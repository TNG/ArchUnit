package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.controller.two.UseCaseTwoController;
import com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.exampletest.LayerDependencyRulesTest;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.junit.ExpectsViolations;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.example.SomeMediator.violateLayerRulesIndirectly;
import static com.tngtech.archunit.example.controller.one.UseCaseOneTwoController.someString;
import static com.tngtech.archunit.example.controller.two.UseCaseTwoController.doSomethingTwo;
import static com.tngtech.archunit.example.persistence.layerviolation.DaoCallingService.violateLayerRules;
import static com.tngtech.archunit.example.service.ServiceViolatingLayerRules.illegalAccessToController;
import static com.tngtech.archunit.junit.ExpectedAccess.from;

public class LayerDependencyRulesIntegrationTest extends LayerDependencyRulesTest {

    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @Test
    @Override
    public void services_should_not_access_controllers() {
        expectViolationByAccessFromServiceToController(expectViolation);

        super.services_should_not_access_controllers();
    }

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationByAccessFromServiceToController(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("no classes that reside in a package '..service..' " +
                "should access classes that reside in a package '..controller..'")
                .byAccess(from(ServiceViolatingLayerRules.class, illegalAccessToController)
                        .getting().field(UseCaseOneTwoController.class, someString)
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

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationByAccessFromPersistenceToService(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("no classes that reside in a package '..persistence..' should " +
                "access classes that reside in a package '..service..'")
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

    @CalledByArchUnitIntegrationTestRunner
    static void expectViolationByIllegalAccessToService(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule("classes that reside in a package '..service..' should " +
                "only be accessed by any package ['..controller..', '..service..']")
                .byCall(from(DaoCallingService.class, violateLayerRules)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(13))
                .byCall(from(SomeMediator.class, violateLayerRulesIndirectly)
                        .toMethod(ServiceViolatingLayerRules.class, ServiceViolatingLayerRules.doSomething)
                        .inLine(15));
    }
}
