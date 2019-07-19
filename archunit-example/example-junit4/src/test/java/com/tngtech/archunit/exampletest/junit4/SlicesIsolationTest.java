package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.layers.controller.two.UseCaseTwoController;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slice;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class SlicesIsolationTest {
    @ArchTest
    public static final ArchRule controllers_should_only_use_their_own_slice =
            slices().matching("..controller.(*)..").namingSlices("Controller $1")
                    .as("Controllers").should().notDependOnEachOther();

    @ArchTest
    public static final ArchRule specific_controllers_should_only_use_their_own_slice =
            slices().matching("..controller.(*)..").namingSlices("Controller $1")
                    .that(containDescription("Controller one"))
                    .or(containDescription("Controller two"))
                    .as("Controllers one and two").should().notDependOnEachOther();

    @ArchTest
    public static final ArchRule controllers_should_only_use_their_own_slice_with_custom_ignore =
            slices().matching("..controller.(*)..").namingSlices("Controller $1")
                    .as("Controllers").should().notDependOnEachOther()
                    .ignoreDependency(UseCaseOneTwoController.class, UseCaseTwoController.class)
                    .ignoreDependency(nameMatching(".*controller\\.three.*"), DescribedPredicate.<JavaClass>alwaysTrue());

    private static DescribedPredicate<Slice> containDescription(final String descriptionPart) {
        return new DescribedPredicate<Slice>("contain description '%s'", descriptionPart) {
            @Override
            public boolean apply(Slice input) {
                return input.getDescription().contains(descriptionPart);
            }
        };
    }
}
