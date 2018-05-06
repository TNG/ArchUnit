package com.tngtech.archunit.integration.junit4;

import com.tngtech.archunit.exampletest.junit4.LayerDependencyRulesTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class LayerDependencyRulesIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.LayerDependencyRulesIntegrationTest.class,
            method = "expectViolationByAccessFromServiceToController")
    public static final ArchRule services_should_not_access_controllers =
            LayerDependencyRulesTest.services_should_not_access_controllers;

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.LayerDependencyRulesIntegrationTest.class,
            method = "expectViolationByAccessFromPersistenceToService")
    public static final ArchRule persistence_should_not_access_services =
            LayerDependencyRulesTest.persistence_should_not_access_services;

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.LayerDependencyRulesIntegrationTest.class,
            method = "expectViolationByIllegalAccessToService")
    public static final ArchRule services_should_only_be_accessed_by_controllers_or_other_services =
            LayerDependencyRulesTest.services_should_only_be_accessed_by_controllers_or_other_services;
}
