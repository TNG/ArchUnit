package com.tngtech.archunit.integration.junit;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules.MyEntityManager;
import com.tngtech.archunit.exampletest.junit.DaoRulesWithRunnerTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.junit.ExpectedViolation;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.junit.ExpectedViolation.from;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class DaoRulesWithRunnerIntegrationTest {
    private static final String ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT =
            "Only DAOs may use the " + EntityManager.class.getSimpleName();

    @ArchTest
    @ExpectedViolationFrom(location = DaoRulesWithRunnerIntegrationTest.class, method = "expectViolationByIllegalUseOfEntityManager")
    public static final ArchRule only_DAOs_may_use_the_EntityManager =
            DaoRulesWithRunnerTest.only_DAOs_may_use_the_EntityManager;

    private static void expectViolationByIllegalUseOfEntityManager(ExpectedViolation expectedViolation) {
        expectedViolation.ofRule(ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT)
                .byCall(from(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(24))
                .byCall(from(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(MyEntityManager.class, "persist", Object.class)
                        .inLine(25));
    }
}
