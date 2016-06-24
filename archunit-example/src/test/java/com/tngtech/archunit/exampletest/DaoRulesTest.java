package com.tngtech.archunit.exampletest;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.DescribedPredicate.are;
import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classCallsMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classResidesIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.named;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetClassIs;

public class DaoRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(InWrongPackageDao.class, OtherDao.class, ServiceViolatingDaoRules.class);
    }

    @Ignore
    @Test
    public void DAOs_must_reside_in_a_dao_package() {
        all(classes.that(are(named(".*Dao")).as("DAOs")))
                .should("reside in a package 'dao'")
                .assertedBy(classResidesIn("..dao.."));
    }

    @Ignore
    @Test
    public void only_DAOs_may_use_the_EntityManager() {
        JavaClasses classesThatAreNoDaos = classes.that(not(resideIn("..dao..")).as("classes that are no DAOs"));

        all(classesThatAreNoDaos).should("not access the " + EntityManager.class.getSimpleName()).
                assertedBy(never(classCallsMethodWhere(targetClassIs(EntityManager.class))));
    }

    @Ignore
    @Test
    public void entities_must_reside_in_a_domain_package() {
        JavaClasses entities = classes.that(are(annotatedWith(Entity.class)).as("Entities"));

        all(entities).should("reside in a package 'domain'").assertedBy(classResidesIn("..domain.."));
    }
}
