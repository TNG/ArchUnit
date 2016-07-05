package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slices;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.dependencies.DependencyRules.slicesShouldNotDependOnEachOtherIn;

@RunWith(ArchUnitRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class SlicesIsolationTest {
    @ArchIgnore
    @ArchTest
    public static final ArchRule<?> controllers_should_only_use_their_own_slice =
            slicesShouldNotDependOnEachOtherIn(
                    Slices.matching("..controller.(*)..")
                            .namingSlices("Controller $1").as("Controllers"));
}
