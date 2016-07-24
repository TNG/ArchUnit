package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.example.cycle.Cycles;
import com.tngtech.archunit.library.dependencies.Slices;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.library.dependencies.DependencyRules.beFreeOfCycles;

public class CyclicDependencyRulesTest {
    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImportHelper().importTreesOf(Cycles.class);
    }

    @Ignore
    @Test
    public void slices_should_not_contain_cyclic_dependencies_by_simple_method_calls() {
        all(Slices.of(classes).matching("..(simplecycle).(*)..").namingSlices("$2 of $1"))
                .should(beFreeOfCycles());
    }

    @Ignore
    @Test
    public void slices_should_not_contain_cyclic_dependencies_by_simple_constructor_calls() {
        all(Slices.of(classes).matching("..(constructorcycle).(*)..").namingSlices("$2 of $1"))
                .should(beFreeOfCycles());
    }

    @Ignore
    @Test
    public void slices_should_not_contain_cyclic_dependencies_by_inheritance() {
        all(Slices.of(classes).matching("..(inheritancecycle).(*)..").namingSlices("$2 of $1"))
                .should(beFreeOfCycles());
    }

    @Ignore
    @Test
    public void slices_should_not_contain_cyclic_dependencies_by_field_access() {
        all(Slices.of(classes).matching("..(fieldaccesscycle).(*)..").namingSlices("$2 of $1"))
                .should(beFreeOfCycles());
    }

    @Ignore
    @Test
    public void simple_cyclic_scenario() {
        all(Slices.of(classes).matching("..simplescenario.(*)..").namingSlices("$1"))
                .should(beFreeOfCycles());
    }

    @Ignore
    @Test
    public void slices_should_not_contain_cyclic_dependencies() {
        all(Slices.of(classes).matching("..(complexcycles).(*)..").namingSlices("$2 of $1"))
                .should(beFreeOfCycles());
    }
}
