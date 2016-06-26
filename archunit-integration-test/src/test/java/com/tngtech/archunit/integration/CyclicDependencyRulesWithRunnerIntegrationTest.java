package com.tngtech.archunit.integration;

import com.tngtech.archunit.exampletest.CyclicDependencyRulesWithRunnerTest;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slice;
import org.junit.runner.RunWith;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example.cycle")
public class CyclicDependencyRulesWithRunnerIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromSimpleCycle")
    public static final ArchRule<Slice> NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromConstructorCycle")
    public static final ArchRule<Slice> NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromInheritanceCycle")
    public static final ArchRule<Slice> NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromFieldAccessCycle")
    public static final ArchRule<Slice> NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromSimpleCyclicScenario")
    public static final ArchRule<Slice> NO_CYCLES_IN_SIMPLE_SCENARIO =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_IN_SIMPLE_SCENARIO;

    @ArchTest
    @ExpectedViolationFrom(location = CyclicDependencyRulesIntegrationTest.class, method = "expectViolationFromComplexCyclicScenario")
    public static final ArchRule<Slice> NO_CYCLES_IN_COMPLEX_SCENARIO =
            CyclicDependencyRulesWithRunnerTest.NO_CYCLES_IN_COMPLEX_SCENARIO;
}
