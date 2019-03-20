package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.testclasses.first.any.pkg.ClassOnlyDependentOnOwnPackageAndObject;
import com.tngtech.archunit.library.testclasses.first.any.pkg.FirstAnyPkgClass;
import com.tngtech.archunit.library.testclasses.first.three.any.FirstThreeAnyClass;
import com.tngtech.archunit.library.testclasses.second.any.pkg.SecondAnyClass;
import com.tngtech.archunit.library.testclasses.second.three.any.SecondThreeAnyClass;
import com.tngtech.archunit.library.testclasses.some.pkg.sub.SomePkgSubClass;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.assertion.DependenciesAssertion.from;

public class SliceTest {

    @Test
    public void dependencies_from_self() {
        Slice slice = getSlice(slicesOfTestClasses(), "first");

        assertThatDependencies(slice.getDependenciesFromSelf()).containOnly(
                from(FirstAnyPkgClass.class).to(Object.class)
                        .from(FirstAnyPkgClass.class).to(SomePkgSubClass.class)
                        .from(FirstAnyPkgClass.class).to(SecondThreeAnyClass.class)
                        .from(ClassOnlyDependentOnOwnPackageAndObject.class).to(Object.class)
                        .from(FirstThreeAnyClass.class).to(Object.class)
                        .from(FirstThreeAnyClass.class).to(SecondThreeAnyClass.class)
        );
    }

    @Test
    @Deprecated
    // FIXME: Remove this once the deprecation is thrown out
    public void dependencies() {
        Slice slice = getSlice(slicesOfTestClasses(), "first");

        assertThatDependencies(slice.getDependencies()).containOnly(
                from(FirstAnyPkgClass.class).to(Object.class)
                        .from(FirstAnyPkgClass.class).to(SomePkgSubClass.class)
                        .from(FirstAnyPkgClass.class).to(SecondThreeAnyClass.class)
                        .from(ClassOnlyDependentOnOwnPackageAndObject.class).to(Object.class)
                        .from(FirstThreeAnyClass.class).to(Object.class)
                        .from(FirstThreeAnyClass.class).to(SecondThreeAnyClass.class)
        );
    }

    @Test
    public void dependencies_to_self() {
        Slice slice = getSlice(slicesOfTestClasses(), "first");

        assertThatDependencies(slice.getDependenciesToSelf()).containOnly(
                from(SecondAnyClass.class).to(FirstAnyPkgClass.class)
                        .from(SomePkgSubClass.class).to(FirstAnyPkgClass.class)
        );
    }

    private Slice getSlice(Slices slices, String name) {
        for (Slice slice : slices) {
            if (slice.getNamePart(1).equals(name)) {
                return slice;
            }
        }
        throw new AssertionError(String.format("Could not find slice '%s'", name));
    }

    private Slices slicesOfTestClasses() {
        JavaClasses testClasses = new ClassFileImporter().importPackages("com.tngtech.archunit.library.testclasses");
        return Slices.matching("..testclasses.(*)..").transform(testClasses);
    }
}