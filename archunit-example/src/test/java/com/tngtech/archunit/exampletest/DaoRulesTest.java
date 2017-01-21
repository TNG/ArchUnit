package com.tngtech.archunit.exampletest;

import javax.persistence.Entity;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withNameMatching;
import static com.tngtech.archunit.lang.conditions.ArchConditions.resideInAPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.allClasses;

public class DaoRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(InWrongPackageDao.class, OtherDao.class, ServiceViolatingDaoRules.class);
    }

    @Ignore
    @Test
    public void DAOs_must_reside_in_a_dao_package() {
        allClasses().that(withNameMatching(".*Dao")).should(resideInAPackage("..dao.."))
                .as("DAOs should reside in a package '..dao..'").check(classes);
    }

    @Ignore
    @Test
    public void entities_must_reside_in_a_domain_package() {
        allClasses().that(are(annotatedWith(Entity.class))).should(resideInAPackage("..domain.."))
                .as("Entities should reside in a package '..domain..'").check(classes);
    }
}
