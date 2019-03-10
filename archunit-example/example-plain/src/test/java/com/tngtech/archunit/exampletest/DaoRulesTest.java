package com.tngtech.archunit.exampletest;

import java.sql.SQLException;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.persistence.first.InWrongPackageDao;
import com.tngtech.archunit.example.persistence.second.dao.OtherDao;
import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Category(Example.class)
public class DaoRulesTest {

    private final JavaClasses classes =
            new ClassFileImporter().importPackagesOf(InWrongPackageDao.class, OtherDao.class, ServiceViolatingDaoRules.class);

    @Test
    public void DAOs_must_reside_in_a_dao_package() {
        classes().that().haveNameMatching(".*Dao").should().resideInAPackage("..dao..")
                .as("DAOs should reside in a package '..dao..'")
                .check(classes);
    }

    @Test
    public void entities_must_reside_in_a_domain_package() {
        classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..domain..")
                .as("Entities should reside in a package '..domain..'")
                .check(classes);
    }

    @Test
    public void only_DAOs_may_use_the_EntityManager() {
        noClasses().that().resideOutsideOfPackage("..dao..")
                .should().accessClassesThat().areAssignableTo(EntityManager.class)
                .as("Only DAOs may use the " + EntityManager.class.getSimpleName())
                .check(classes);
    }

    @Test
    public void DAOs_must_not_throw_SQLException() {
        classes().that().haveNameMatching(".*Dao").should(notContainMethodsThrowing(SQLException.class))
                .check(classes);
    }

    private static ArchCondition<JavaClass> notContainMethodsThrowing(final Class<? extends Exception> exception) {
        return new ArchCondition<JavaClass>("not contain methods throwing " + exception.getName()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    if (method.getThrowsClause().containsType(exception)) {
                        String message = String.format("%s throws %s in %s",
                                method.getFullName(), exception.getName(),
                                method.getOccurrence());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            }
        };
    }

}
