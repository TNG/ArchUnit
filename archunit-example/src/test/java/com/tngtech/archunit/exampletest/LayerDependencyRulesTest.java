package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.ClassViolatingCodingRules;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classAccessesPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.classIsOnlyAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
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
                .should("not access classes that reside in '..controller..'")
                .assertedBy(never(classAccessesPackage("..controller..")));
    }

    @Ignore
    @Test
    public void persistence_should_not_access_services() {
        all(classes.that(resideIn("..persistence..")))
                .should("not access classes that reside in '..service..'")
                .assertedBy(never(classAccessesPackage("..service..")));
    }

    @Ignore
    @Test
    public void services_should_only_be_accessed_by_controllers_or_other_services() {
        all(classes.that(resideIn("..service..")))
                .should("only be accessed by classes that either reside in '..controller..' or '..service'")
                .assertedBy(classIsOnlyAccessedByAnyPackage("..controller..", "..service.."));
    }
}
