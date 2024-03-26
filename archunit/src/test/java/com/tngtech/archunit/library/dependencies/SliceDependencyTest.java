package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.library.dependencies.GivenSlicesTest.TEST_CLASSES_PACKAGE;
import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;

public class SliceDependencyTest {
    @Test
    public void can_be_converted_to_dependencies() {
        Slices slices = Slices.matching(TEST_CLASSES_PACKAGE + ".(*)..")
                .of(new ClassFileImporter().importPackages(TEST_CLASSES_PACKAGE));

        Slice origin = getSlice("first", slices);
        Slice target = getSlice("second", slices);

        SliceDependency sliceDependency = SliceDependency.of(origin, origin.getDependenciesFromSelf(), target);

        assertThatConversionOf(sliceDependency)
                .satisfiesStandardConventions()
                .isPossibleTo(Dependency.class);
    }

    private Slice getSlice(String name, Slices slices) {
        return slices.stream().filter(it -> it.getNamePart(1).equals(name)).findFirst().get();
    }
}