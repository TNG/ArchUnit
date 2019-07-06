package com.tngtech.archunit.exampletest.junit4;

import java.sql.SQLException;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class DaoRulesTest {
    @ArchTest
    public static final ArchRule DAOs_must_reside_in_a_dao_package =
            classes().that().haveNameMatching(".*Dao").should().resideInAPackage("..dao..")
                    .as("DAOs should reside in a package '..dao..'");

    @ArchTest
    public static final ArchRule entities_must_reside_in_a_domain_package =
            classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..domain..")
                    .as("Entities should reside in a package '..domain..'");

    @ArchTest
    public static final ArchRule only_DAOs_may_use_the_EntityManager =
            noClasses().that().resideOutsideOfPackage("..dao..")
                    .should().accessClassesThat().areAssignableTo(EntityManager.class)
                    .as("Only DAOs may use the " + EntityManager.class.getSimpleName());

    @ArchTest
    public static final ArchRule DAOs_must_not_throw_SQLException =
            noMethods().that().areDeclaredInClassesThat().haveNameMatching(".*Dao")
                    .should().declareThrowableOfType(SQLException.class);
}
