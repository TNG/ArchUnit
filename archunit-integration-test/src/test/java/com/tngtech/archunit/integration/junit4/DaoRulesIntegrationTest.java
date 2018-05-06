package com.tngtech.archunit.integration.junit4;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules.MyEntityManager;
import com.tngtech.archunit.exampletest.junit4.DaoRulesTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.CalledByArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectsViolations;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedAccess.callFromMethod;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class DaoRulesIntegrationTest {
    private static final String ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT =
            "Only DAOs may use the " + EntityManager.class.getSimpleName();

    @ArchTest
    @ExpectedViolationFrom(location = DaoRulesIntegrationTest.class, method = "expectViolationByIllegalUseOfEntityManager")
    public static final ArchRule only_DAOs_may_use_the_EntityManager =
            DaoRulesTest.only_DAOs_may_use_the_EntityManager;

    @CalledByArchUnitIntegrationTestRunner
    private static void expectViolationByIllegalUseOfEntityManager(ExpectsViolations expectsViolations) {
        expectsViolations.ofRule(ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT)
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(26))
                .by(callFromMethod(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(MyEntityManager.class, "persist", Object.class)
                        .inLine(27));
    }
}
