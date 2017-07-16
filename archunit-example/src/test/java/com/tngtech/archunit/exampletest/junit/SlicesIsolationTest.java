package com.tngtech.archunit.exampletest.junit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slice;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class SlicesIsolationTest {
    @ArchIgnore
    @ArchTest
    public static final ArchRule controllers_should_only_use_their_own_slice =
            slices().matching("..controller.(*)..").namingSlices("Controller $1")
                    .as("Controllers").should().notDependOnEachOther();

    @ArchIgnore
    @ArchTest
    public static final ArchRule specific_controllers_should_only_use_their_own_slice =
            slices().matching("..controller.(*)..").namingSlices("Controller $1")
                    .that(containDescription("Controller one"))
                    .or(containDescription("Controller two"))
                    .as("Controllers one and two").should().notDependOnEachOther();

    private static DescribedPredicate<Slice> containDescription(final String descriptionPart) {
        return new DescribedPredicate<Slice>("contain description '%s'", descriptionPart) {
            @Override
            public boolean apply(Slice input) {
                return input.getDescription().contains(descriptionPart);
            }
        };
    }
}
