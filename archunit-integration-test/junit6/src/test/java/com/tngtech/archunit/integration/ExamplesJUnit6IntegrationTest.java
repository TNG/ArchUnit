package com.tngtech.archunit.integration;

import java.util.stream.Stream;

import com.tngtech.archunit.testutils.ExpectedJUnit6TestFailures;
import com.tngtech.archunit.testutils.ExpectedTestFailures;
import com.tngtech.archunit.testutils.ResultStoringExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class ExamplesJUnit6IntegrationTest extends ExamplesIntegrationTestBase<DynamicTest> {

    @BeforeAll
    static void initExtension() {
        ResultStoringExtension.enable();
    }

    @AfterEach
    void tearDown() {
        ResultStoringExtension.reset();
    }

    @AfterAll
    static void disableExtension() {
        ResultStoringExtension.disable();
    }

    @Override
    protected ExpectedTestFailures<DynamicTest> createExpectedTestFailures(Class<?>... testClasses) {
        return ExpectedJUnit6TestFailures.forTests(testClasses);
    }

    @TestFactory
    Stream<DynamicTest> CodingRulesTest() {
        return CodingRulesTest(com.tngtech.archunit.exampletest.junit6.CodingRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ControllerRulesTest() {
        return ControllerRulesTest(com.tngtech.archunit.exampletest.junit6.ControllerRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> CyclicDependencyRulesTest() {
        return CyclicDependencyRulesTest(com.tngtech.archunit.exampletest.junit6.CyclicDependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> DaoRulesTest() {
        return DaoRulesTest(com.tngtech.archunit.exampletest.junit6.DaoRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> DependencyRulesTest() {
        return DependencyRulesTest(com.tngtech.archunit.exampletest.junit6.DependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> FrozenRulesTest() {
        return FrozenRulesTest(com.tngtech.archunit.exampletest.junit6.FrozenRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> InterfaceRulesTest() {
        return InterfaceRulesTest(com.tngtech.archunit.exampletest.junit6.InterfaceRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> LayerDependencyRulesTest() {
        return LayerDependencyRulesTest(com.tngtech.archunit.exampletest.junit6.LayerDependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> LayeredArchitectureTest() {
        return LayeredArchitectureTest(com.tngtech.archunit.exampletest.junit6.LayeredArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> OnionArchitectureTest() {
        return OnionArchitectureTest(com.tngtech.archunit.exampletest.junit6.OnionArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> MethodsTest() {
        return MethodsTest(com.tngtech.archunit.exampletest.junit6.MethodsTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ModulesTest() {
        return ModulesTest(com.tngtech.archunit.exampletest.junit6.ModulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> NamingConventionTest() {
        return NamingConventionTest(com.tngtech.archunit.exampletest.junit6.NamingConventionTest.class);
    }

    @TestFactory
    Stream<DynamicTest> PlantUmlArchitectureTest() {
        return PlantUmlArchitectureTest(com.tngtech.archunit.exampletest.junit6.PlantUmlArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ProxyRulesTest() {
        return ProxyRulesTest(com.tngtech.archunit.exampletest.junit6.ProxyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> RestrictNumberOfClassesWithACertainPropertyTest() {
        return RestrictNumberOfClassesWithACertainPropertyTest(com.tngtech.archunit.exampletest.junit6.RestrictNumberOfClassesWithACertainPropertyTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SecurityTest() {
        return SecurityTestImpl();
    }

    @TestFactory
    Stream<DynamicTest> SessionBeanRulesTest() {
        return SessionBeanRulesTest(com.tngtech.archunit.exampletest.junit6.SessionBeanRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SingleClassTest() {
        return SingleClassTest(com.tngtech.archunit.exampletest.junit6.SingleClassTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SlicesIsolationTest() {
        return SlicesIsolationTest(com.tngtech.archunit.exampletest.junit6.SlicesIsolationTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ThirdPartyRulesTest() {
        return ThirdPartyRulesTest(com.tngtech.archunit.exampletest.junit6.ThirdPartyRulesTest.class);
    }
}
