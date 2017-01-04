package com.tngtech.archunit.exampletest;

import javax.persistence.Entity;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.lang.ClassesTransformer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.ArchRule.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.resideInAPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.withName;

public class DaoRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(InWrongPackageDao.class, OtherDao.class, ServiceViolatingDaoRules.class);
    }

    @Ignore
    @Test
    public void DAOs_must_reside_in_a_dao_package() {
        all(classes().that(are(withName(".*Dao"))).as("DAOs"))
                .should(resideInAPackage("..dao..")).check(classes);
    }

    @Ignore
    @Test
    public void entities_must_reside_in_a_domain_package() {
        ClassesTransformer<JavaClass> entities = classes().that(are(annotatedWith(Entity.class))).as("Entities");

        all(entities).should(resideInAPackage("..domain..")).check(classes);
    }
}
