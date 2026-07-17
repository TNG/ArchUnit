package com.tngtech.archunit.library.modules;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.ClassOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.one.ClassOneOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.one.ClassTwoOne;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    public void description_contains_class_dependencies_in_stable_order() {
        ArchModules<?> modules = ArchModules.defineByPackages("..test_modules.(*)..")
                .modularize(new ClassFileImporter().importPackagesOf(
                        ClassOne.class, ClassOneOne.class, ClassTwo.class, ClassTwoOne.class));

        ModuleDependency<?> moduleDependency = createDependency(modules, "one", "two");

        assertThat(moduleDependency.getDescription())
                .isEqualTo(String.format("Module Dependency [one -> two]:%n%s%n%s",
                        String.format("Field <%s.classTwo> has type <%s> in (ClassOne.java:0)",
                                ClassOne.class.getName(), ClassTwo.class.getName()),
                        String.format("Field <%s.twoOne> has type <%s> in (ClassOneOne.java:0)",
                                ClassOneOne.class.getName(), ClassTwoOne.class.getName())));
    }

    private static <D extends ArchModule.Descriptor> ModuleDependency<D> createDependency(ArchModules<D> modules, String originIdentifier, String targetIdentifier) {
        return ModuleDependency.tryCreate(
                modules.getByIdentifier(originIdentifier),
                modules.getByIdentifier(targetIdentifier)
        ).get();
    }
}
