package com.tngtech.archunit.integration;

import javax.persistence.EntityManager;

import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.first.dao.EntityInWrongPackage;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.exampletest.DaoRulesTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedViolation.from;
import static com.tngtech.archunit.junit.ExpectedViolation.javaPackageOf;

public class DaoRulesIntegrationTest extends DaoRulesTest {
    public static final String ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT =
            "classes that are no DAOs should not access the " + EntityManager.class.getSimpleName();

    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void DAOs_must_reside_in_a_dao_package() {
        expectedViolation.ofRule("DAOs should reside in a package '..dao..'")
                .by(javaPackageOf(InWrongPackageDao.class).notMatching("..dao.."));

        super.DAOs_must_reside_in_a_dao_package();
    }

    @Test
    @Override
    public void only_DAOs_may_use_the_EntityManager() {
        expectedViolation.ofRule(ONLY_DAOS_MAY_ACCESS_THE_ENTITYMANAGER_RULE_TEXT)
                .byCall(from(ServiceViolatingDaoRules.class, "illegallyUseEntityManager")
                        .toMethod(EntityManager.class, "persist", Object.class)
                        .inLine(20));

        super.only_DAOs_may_use_the_EntityManager();
    }

    @Test
    @Override
    public void entities_must_reside_in_a_domain_package() {
        expectedViolation.ofRule("Entities should reside in a package '..domain..'")
                .by(javaPackageOf(EntityInWrongPackage.class).notMatching("..domain.."));

        super.entities_must_reside_in_a_domain_package();
    }
}
