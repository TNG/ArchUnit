package com.tngtech.archunit.exampletest.junit;

import javax.persistence.EntityManager;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClass;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class DaoRulesWithRunnerTest {
    @ArchTest
    public static final ArchRule only_DAOs_may_use_the_EntityManager =
            all(classes().that(not(resideIn("..dao.."))).as("classes that are no DAOs"))
                    .should(never(accessClass(assignableTo(EntityManager.class))
                            .as("access the " + EntityManager.class.getSimpleName())));
}
