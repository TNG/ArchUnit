package com.tngtech.archunit.integration;

import com.tngtech.archunit.exampletest.LayerDependencyRulesWithRunnerTest;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class LayerDependencyRulesWithRunnerIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = LayerDependencyRulesIntegrationTest.class, method = "expectViolationByAccessFromServiceToController")
    public static final ArchRule<?> services_should_not_access_controllers =
            LayerDependencyRulesWithRunnerTest.services_should_not_access_controllers;

    @ArchTest
    @ExpectedViolationFrom(location = LayerDependencyRulesIntegrationTest.class, method = "expectViolationByAccessFromPersistenceToService")
    public static final ArchRule<?> persistence_should_not_access_services =
            LayerDependencyRulesWithRunnerTest.persistence_should_not_access_services;

    @ArchTest
    @ExpectedViolationFrom(location = LayerDependencyRulesIntegrationTest.class, method = "expectViolationByIllegalAccessToService")
    public static final ArchRule<?> services_should_only_be_accessed_by_controllers_or_other_services =
            LayerDependencyRulesWithRunnerTest.services_should_only_be_accessed_by_controllers_or_other_services;
}
