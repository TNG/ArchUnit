package com.tngtech.archunit.exampletest.junit4.projectB;

import com.tngtech.archunit.exampletest.junit4.Example;
import com.tngtech.archunit.exampletest.junit4.platform.ServiceRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Runs the shared rules for "projectB" only.
 */
@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.projectB")
public class ServiceRulesTest {
    @ArchTest
    private final ArchRules service_rules = ArchRules.in(ServiceRules.class);
}
