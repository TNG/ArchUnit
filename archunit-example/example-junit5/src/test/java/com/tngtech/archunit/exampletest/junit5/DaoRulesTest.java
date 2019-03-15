package com.tngtech.archunit.exampletest.junit5;

import java.sql.SQLException;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class DaoRulesTest {
    @ArchTest
    static final ArchRule DAOs_must_reside_in_a_dao_package =
            classes().that().haveNameMatching(".*Dao").should().resideInAPackage("..dao..")
                    .as("DAOs should reside in a package '..dao..'");

    @ArchTest
    static final ArchRule entities_must_reside_in_a_domain_package =
            classes().that().areAnnotatedWith(Entity.class).should().resideInAPackage("..domain..")
                    .as("Entities should reside in a package '..domain..'");

    @ArchTest
    static final ArchRule only_DAOs_may_use_the_EntityManager =
            noClasses().that().resideOutsideOfPackage("..dao..")
                    .should().accessClassesThat().areAssignableTo(EntityManager.class)
                    .as("Only DAOs may use the " + EntityManager.class.getSimpleName());


    @ArchTest
    static final ArchRule DAOs_must_not_throw_SQLException =
            classes().that().haveNameMatching(".*Dao")
                    .should(notContainMethodsThrowing(SQLException.class));

    private static ArchCondition<JavaClass> notContainMethodsThrowing(final Class<? extends Exception> exception) {
        return new ArchCondition<JavaClass>("not contain methods throwing " + exception.getName()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    if (method.getThrowsClause().containsType(exception)) {
                        String message = String.format("%s throws %s in %s",
                                method.getFullName(), exception.getName(),
                                method.getSourceCodeLocation());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            }
        };
    }
}
