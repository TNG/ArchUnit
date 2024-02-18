package com.tngtech.archunit.library.modules;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.ClassOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;

public class ModuleDependencyTest {
    @Test
    public void can_be_converted_to_dependencies() {
        ArchModules<?> modules = ArchModules.defineByPackages("..test_modules.(*)..")
                .modularize(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class));

        ModuleDependency<?> moduleDependency = createDependency(modules, "one", "two");

        assertThatConversionOf(moduleDependency)
                .satisfiesStandardConventions()
                .isPossibleTo(Dependency.class);
    }

    private static <D extends ArchModule.Descriptor> ModuleDependency<D> createDependency(ArchModules<D> modules, String originIdentifier, String targetIdentifier) {
        return ModuleDependency.tryCreate(
                modules.getByIdentifier(originIdentifier),
                modules.getByIdentifier(targetIdentifier)
        ).get();
    }
}