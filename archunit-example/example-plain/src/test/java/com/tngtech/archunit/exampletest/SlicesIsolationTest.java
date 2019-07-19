package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.layers.controller.one.UseCaseOneTwoController;
import com.tngtech.archunit.example.layers.controller.two.UseCaseTwoController;
import com.tngtech.archunit.library.dependencies.Slice;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@Category(Example.class)
public class SlicesIsolationTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.layers");

    @Test
    public void controllers_should_only_use_their_own_slice() {
        slices().matching("..controller.(*)..").namingSlices("Controller $1")
                .as("Controllers").should().notDependOnEachOther()
                .check(classes);
    }

    @Test
    public void specific_controllers_should_only_use_their_own_slice() {
        slices().matching("..controller.(*)..").namingSlices("Controller $1")
                .that(containDescription("Controller one"))
                .or(containDescription("Controller two"))
                .as("Controllers one and two").should().notDependOnEachOther()
                .check(classes);
    }

    @Test
    public void controllers_should_only_use_their_own_slice_with_custom_ignore() {
        slices().matching("..controller.(*)..").namingSlices("Controller $1")
                .as("Controllers").should().notDependOnEachOther()
                .ignoreDependency(UseCaseOneTwoController.class, UseCaseTwoController.class)
                .ignoreDependency(nameMatching(".*controller\\.three.*"), DescribedPredicate.<JavaClass>alwaysTrue())
                .check(classes);
    }

    private static DescribedPredicate<Slice> containDescription(final String descriptionPart) {
        return new DescribedPredicate<Slice>("contain description '%s'", descriptionPart) {
            @Override
            public boolean apply(Slice input) {
                return input.getDescription().contains(descriptionPart);
            }
        };
    }
}
