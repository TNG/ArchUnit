package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaClass.Predicates.resideInPackage;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;

public class LayerDependencyRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingCodingRules.class);
    }

    @Ignore
    @Test
    public void services_should_not_access_controllers() {
        all(classes().that(resideInPackage("..service..")))
                .should(never(accessClassesThatResideIn("..controller.."))).check(classes);
    }

    @Ignore
    @Test
    public void persistence_should_not_access_services() {
        all(classes().that(resideInPackage("..persistence..")))
                .should(never(accessClassesThatResideIn("..service.."))).check(classes);
    }

    @Ignore
    @Test
    public void services_should_only_be_accessed_by_controllers_or_other_services() {
        all(classes().that(resideInPackage("..service..")))
                .should(onlyBeAccessedByAnyPackage("..controller..", "..service..")).check(classes);
    }
}
