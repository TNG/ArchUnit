package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slice;
import com.tngtech.archunit.library.dependencies.Slices;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.library.dependencies.DependencyRules.beFreeOfCycles;

@ArchIgnore
@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example.cycle")
public class CyclicDependencyRulesWithRunnerTest {

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_BY_METHOD_CALLS_BETWEEN_SLICES =
            all(Slices.matching("..(simplecycle).(*)..").namingSlices("$2 of $1")).should(beFreeOfCycles());

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_BY_CONSTRUCTOR_CALLS_BETWEEN_SLICES =
            all(Slices.matching("..(constructorcycle).(*)..").namingSlices("$2 of $1")).should(beFreeOfCycles());

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_BY_INHERITANCE_BETWEEN_SLICES =
            all(Slices.matching("..(inheritancecycle).(*)..").namingSlices("$2 of $1")).should(beFreeOfCycles());

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_BY_FIELD_ACCESS_BETWEEN_SLICES =
            all(Slices.matching("..(fieldaccesscycle).(*)..").namingSlices("$2 of $1")).should(beFreeOfCycles());

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_IN_SIMPLE_SCENARIO =
            all(Slices.matching("..simplescenario.(*)..").namingSlices("$1")).should(beFreeOfCycles());

    @ArchTest
    public static final ArchRule<Slice> NO_CYCLES_IN_COMPLEX_SCENARIO =
            all(Slices.matching("..(complexcycles).(*)..").namingSlices("$2 of $1")).should(beFreeOfCycles());
}

