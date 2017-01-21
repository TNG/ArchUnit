package com.tngtech.archunit.exampletest.junit;

import javax.persistence.EntityManager;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class DaoRulesWithRunnerTest {
    @ArchTest
    public static final ArchRule only_DAOs_may_use_the_EntityManager =
            noClasses().that().resideOutsideOfPackage("..dao..")
                    .should().access().classesThat().areAssignableTo(EntityManager.class)
                    .as("Only DAOs may use the " + EntityManager.class.getSimpleName());
}
