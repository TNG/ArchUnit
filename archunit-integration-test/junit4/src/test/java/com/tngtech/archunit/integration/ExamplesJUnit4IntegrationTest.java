package com.tngtech.archunit.integration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.testutil.TransientCopyRule;
import com.tngtech.archunit.testutils.ExpectedJUnit4TestFailures;
import com.tngtech.archunit.testutils.ExpectedTestFailures;
import com.tngtech.archunit.testutils.ResultStoringExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

class ExamplesJUnit4IntegrationTest extends ExamplesIntegrationTestBase<DynamicTest> {

    @TempDir
    static Path temporaryViolationStore;

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
        return ExpectedJUnit4TestFailures.forTests(testClasses);
    }

    @TestFactory
    Stream<DynamicTest> CodingRulesTest() {
        return CodingRulesTest(com.tngtech.archunit.exampletest.junit4.CodingRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ControllerRulesTest() {
        return ControllerRulesTest(com.tngtech.archunit.exampletest.junit4.ControllerRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> CyclicDependencyRulesTest() {
        return CyclicDependencyRulesTest(com.tngtech.archunit.exampletest.junit4.CyclicDependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> DaoRulesTest() {
        return DaoRulesTest(com.tngtech.archunit.exampletest.junit4.DaoRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> DependencyRulesTest() {
        return DependencyRulesTest(com.tngtech.archunit.exampletest.junit4.DependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> FrozenRulesTest() {
        return FrozenRulesTest(com.tngtech.archunit.exampletest.junit4.FrozenRulesTest.class, this::withTemporaryViolationStore);
    }

    /** Without this step, older Java versions (e.g. Java 11) modify the ArchUnit store in the source code. */
    private void withTemporaryViolationStore(Runnable test) {
        try {
            File sourceDir = Paths.get(ArchConfiguration.get().getProperty("freeze.store.default.path")).toFile();
            TransientCopyRule transientCopyRule = new TransientCopyRule();
            transientCopyRule.copy(sourceDir, temporaryViolationStore.toFile());
            transientCopyRule.apply(new Statement() {
                @Override
                public void evaluate() {
                    String oldStorePath = ArchConfiguration.get().getProperty("freeze.store.default.path");
                    ArchConfiguration.get().setProperty("freeze.store.default.path", temporaryViolationStore.toAbsolutePath().toString());
                    try {
                        test.run();
                    } finally {
                        ArchConfiguration.get().setProperty("freeze.store.default.path", oldStorePath);
                    }
                }
            }, Description.EMPTY).evaluate();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @TestFactory
    Stream<DynamicTest> InterfaceRulesTest() {
        return InterfaceRulesTest(com.tngtech.archunit.exampletest.junit4.InterfaceRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> LayerDependencyRulesTest() {
        return LayerDependencyRulesTest(com.tngtech.archunit.exampletest.junit4.LayerDependencyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> LayeredArchitectureTest() {
        return LayeredArchitectureTest(com.tngtech.archunit.exampletest.junit4.LayeredArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> OnionArchitectureTest() {
        return OnionArchitectureTest(com.tngtech.archunit.exampletest.junit4.OnionArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> MethodsTest() {
        return MethodsTest(com.tngtech.archunit.exampletest.junit4.MethodsTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ModulesTest() {
        return ModulesTest(com.tngtech.archunit.exampletest.junit4.ModulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> NamingConventionTest() {
        return NamingConventionTest(com.tngtech.archunit.exampletest.junit4.NamingConventionTest.class);
    }

    @TestFactory
    Stream<DynamicTest> PlantUmlArchitectureTest() {
        return PlantUmlArchitectureTest(com.tngtech.archunit.exampletest.junit4.PlantUmlArchitectureTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ProxyRulesTest() {
        return ProxyRulesTest(com.tngtech.archunit.exampletest.junit4.ProxyRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> RestrictNumberOfClassesWithACertainPropertyTest() {
        return RestrictNumberOfClassesWithACertainPropertyTest(com.tngtech.archunit.exampletest.junit4.RestrictNumberOfClassesWithACertainPropertyTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SecurityTest() {
        return SecurityTestImpl();
    }

    @TestFactory
    Stream<DynamicTest> SessionBeanRulesTest() {
        return SessionBeanRulesTest(com.tngtech.archunit.exampletest.junit4.SessionBeanRulesTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SingleClassTest() {
        return SingleClassTest(com.tngtech.archunit.exampletest.junit4.SingleClassTest.class);
    }

    @TestFactory
    Stream<DynamicTest> SlicesIsolationTest() {
        return SlicesIsolationTest(com.tngtech.archunit.exampletest.junit4.SlicesIsolationTest.class);
    }

    @TestFactory
    Stream<DynamicTest> ThirdPartyRulesTest() {
        return ThirdPartyRulesTest(com.tngtech.archunit.exampletest.junit4.ThirdPartyRulesTest.class);
    }
}
