package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree;
import com.tngtech.archunit.example.cycle.complexcycles.slice3.ClassCallingConstructorInSliceFive;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@Tag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.cycle")
class CyclicDependencyRulesTest {

    @ArchTest
    static final ArchRule no_cycles_by_method_calls_between_slices =
            slices().matching("..(simplecycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_by_constructor_calls_between_slices =
            slices().matching("..(constructorcycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_by_inheritance_between_slices =
            slices().matching("..(inheritancecycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_by_field_access_between_slices =
            slices().matching("..(fieldaccesscycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_by_member_dependencies_between_slices =
            slices().matching("..(membercycle).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_in_simple_scenario =
            slices().matching("..simplescenario.(*)..").namingSlices("$1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_in_complex_scenario =
            slices().matching("..(complexcycles).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_cycles_in_complex_scenario_with_custom_ignore =
            slices().matching("..(complexcycles).(*)..").namingSlices("$2 of $1").should().beFreeOfCycles()
                    .ignoreDependency(SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree.class, ClassCallingConstructorInSliceFive.class)
                    .ignoreDependency(resideInAPackage("..slice4.."), alwaysTrue());
}
