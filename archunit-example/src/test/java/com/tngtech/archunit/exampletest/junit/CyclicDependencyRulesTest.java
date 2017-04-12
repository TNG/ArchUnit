package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example.cycle")
public class CyclicDependencyRulesTest {

    @ArchTest
    public static final ArchRule NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES =
            slices().matching("..(simplecycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES =
            slices().matching("..(constructorcycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES =
            slices().matching("..(inheritancecycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES =
            slices().matching("..(fieldaccesscycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule NO_CYCLES_IN_SIMPLE_SCENARIO =
            slices().matching("..simplescenario.(*)..").namingSlices("$1").should().beFreeOfCycles();

    @ArchTest
    public static final ArchRule NO_CYCLES_IN_COMPLEX_SCENARIO =
            slices().matching("..(complexcycles).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();
}

