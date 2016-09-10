package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;

public class LayerDependencyRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(ClassViolatingCodingRules.class);
    }

    @Ignore
    @Test
    public void services_should_not_access_controllers() {
        all(classes.that(resideIn("..service..")))
                .should(never(accessClassesThatResideIn("..controller..")));
    }

    @Ignore
    @Test
    public void persistence_should_not_access_services() {
        all(classes.that(resideIn("..persistence..")))
                .should(never(accessClassesThatResideIn("..service..")));
    }

    @Ignore
    @Test
    public void services_should_only_be_accessed_by_controllers_or_other_services() {
        all(classes.that(resideIn("..service..")))
                .should(onlyBeAccessedByAnyPackage("..controller..", "..service.."));
    }
}
