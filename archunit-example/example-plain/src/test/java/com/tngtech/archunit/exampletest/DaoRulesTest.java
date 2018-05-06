package com.tngtech.archunit.exampletest;

import javax.persistence.Entity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Category(Example.class)
public class DaoRulesTest {

    private final JavaClasses classes = new ClassFileImporter().importPackagesOf(InWrongPackageDao.class, OtherDao.class, ServiceViolatingDaoRules.class);

    @Test
    public void DAOs_must_reside_in_a_dao_package() {
        classes().that().haveNameMatching(".*Dao").should().resideInAPackage("..dao..")
                .as("DAOs should reside in a package '..dao..'").check(classes);
    }

    @Test
    public void entities_must_reside_in_a_domain_package() {
        classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..domain..")
                .as("Entities should reside in a package '..domain..'").check(classes);
    }
}
