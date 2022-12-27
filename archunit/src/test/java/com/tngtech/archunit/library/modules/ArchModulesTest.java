package com.tngtech.archunit.library.modules;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.modules.ArchModule.Identifier;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.FirstClassInModule1;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.SecondClassInModule1;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1.FirstClassInSubModule11;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1.SecondClassInSubModule11;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.FirstClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.SecondClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.FirstClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.sub1.FirstClassInSubModule21;
import com.tngtech.archunit.testutil.assertion.DependenciesAssertion.ExpectedDependencies;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.library.modules.ArchModulesTest.ModuleDependenciesAssertion.ExpectedModuleDependency.from;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.assertion.DependenciesAssertion.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchModulesTest {
    private final String validTestExamplePackage = getClass().getPackage().getName() + ".testexamples.valid";
    private final JavaClasses testExamples = new ClassFileImporter().importPackages(validTestExamplePackage);

    @Test
    public void partitions_modules_by_single_package_not_including_subpackages() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInModule1.class,
                SecondClassInModule1.class);

        assertThat(modules.tryGetByIdentifier("module1")).contains(module);
        assertThat(modules.tryGetByIdentifier("absent")).isEmpty();
    }

    @Test
    public void partitions_modules_by_single_package_each_including_subpackages() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*)..")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInModule1.class,
                SecondClassInModule1.class,
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class,
                FirstClassInSubModule12.class,
                SecondClassInSubModule12.class);
    }

    @Test
    public void partitions_modules_by_multiple_separate_packages() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1", "sub1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByIdentifier("module1")).isEmpty();
        assertThat(modules.tryGetByIdentifier("module1.sub")).isEmpty();
        assertThat(modules.tryGetByIdentifier("module1:sub")).isEmpty();
    }

    @Test
    public void partitions_modules_by_multiple_unified_packages() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(**)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1.sub1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByIdentifier("module1", "sub1")).isEmpty();
    }

    @Test
    public void names_modules_by_default() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        assertThat(modules.getNames()).containsOnly(
                "module1:sub1",
                "module1:sub2",
                "module2:sub1",
                "module2:sub2");
    }

    @Test
    public void allows_naming_modules() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .deriveNameFromPattern("MyModule [$1][${2}]")
                .modularize(testExamples);

        assertThat(modules.getNames()).containsOnly(
                "MyModule [module1][sub1]",
                "MyModule [module1][sub2]",
                "MyModule [module2][sub1]",
                "MyModule [module2][sub2]");
    }

    @Test
    public void rejects_multiple_modules_with_same_name() {
        String duplicateName = "alwaysSame";

        assertThatThrownBy(
                () -> ArchModules
                        .defineByPackages(validTestExamplePackage + ".(*).(*)")
                        .deriveNameFromPattern(duplicateName)
                        .modularize(testExamples)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Found multiple modules with the same name: [" + duplicateName + "]");
    }

    @Test
    public void supports_joined_identifier_when_naming_modules() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .deriveNameFromPattern("MyModule [$@]")
                .modularize(testExamples);

        assertThat(modules.getNames()).containsOnly(
                "MyModule [module1:sub1]",
                "MyModule [module1:sub2]",
                "MyModule [module2:sub1]",
                "MyModule [module2:sub2]");
    }

    @Test
    public void allows_retrieving_modules_by_name() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .deriveNameFromPattern("MyModule [$1][$2]")
                .modularize(testExamples);

        ArchModule module = modules.getByName("MyModule [module1][sub1]");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByName("MyModule [module1][sub1]")).contains(module);
        assertThat(modules.tryGetByName("absent")).isEmpty();
    }

    @Test
    public void allows_defining_modules_by_function() {
        ArchModules modules = ArchModules
                .defineBy(javaClass -> {
                    String suffix = javaClass.getPackageName().replace(validTestExamplePackage, "");
                    List<String> parts = Splitter.on(".").omitEmptyStrings().splitToList(suffix);
                    return parts.size() > 1 ? Identifier.from(parts.subList(0, 2)) : Identifier.ignore();
                })
                .deriveNameFromPattern("Any $1->$2")
                .modularize(testExamples);

        assertThat(modules.getNames()).containsOnly(
                "Any module1->sub1",
                "Any module1->sub2",
                "Any module2->sub1",
                "Any module2->sub2");
    }

    @Test
    public void provides_class_dependencies_from_self() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1", "sub1");

        assertThatDependencies(module.getClassDependenciesFromSelf())
                .containOnly(from(FirstClassInSubModule11.class).to(FirstClassInSubModule12.class)
                        .from(FirstClassInSubModule11.class).to(FirstClassInSubModule12[].class)
                        .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12.class)
                        .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12[].class)
                        .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12[][].class)
                        .from(SecondClassInSubModule11.class).to(FirstClassInModule2.class)
                        .from(SecondClassInSubModule11.class).to(FirstClassInSubModule21.class)
                        .from(FirstClassInSubModule11.class).to(String.class)
                        .from(FirstClassInSubModule11.class).to(List.class)
                        .from(FirstClassInSubModule11.class).to(Object.class)
                        .from(SecondClassInSubModule11.class).to(Object.class)
                        .from(SecondClassInSubModule11.class).to(Collection.class)
                );
    }

    @Test
    public void provides_class_dependencies_to_self() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module2", "sub1");

        assertThatDependencies(module.getClassDependenciesToSelf())
                .containOnly(from(SecondClassInSubModule11.class).to(FirstClassInSubModule21.class)
                        .from(FirstClassInSubModule12.class).to(FirstClassInSubModule21.class));
    }

    @Test
    public void provides_module_dependencies_from_self() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1", "sub1");

        assertThatModuleDependencies(module.getModuleDependenciesFromSelf())
                .containOnlyModuleDependencies(
                        from("module1", "sub1").to("module1", "sub2")
                                .withClassDependencies(
                                        from(FirstClassInSubModule11.class).to(FirstClassInSubModule12.class)
                                                .from(FirstClassInSubModule11.class).to(FirstClassInSubModule12[].class)
                                                .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12.class)
                                                .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12[].class)
                                                .from(FirstClassInSubModule11.class).to(SecondClassInSubModule12[][].class)),
                        from("module1", "sub1").to("module2", "sub1")
                                .withClassDependencies(from(SecondClassInSubModule11.class).to(FirstClassInSubModule21.class)));
    }

    @Test
    public void provides_module_dependencies_to_self() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module2", "sub1");

        assertThatModuleDependencies(module.getModuleDependenciesToSelf())
                .containOnlyModuleDependencies(
                        from("module1", "sub1").to("module2", "sub1")
                                .withClassDependencies(from(SecondClassInSubModule11.class).to(FirstClassInSubModule21.class)),
                        from("module1", "sub2").to("module2", "sub1")
                                .withClassDependencies(
                                        from(FirstClassInSubModule12.class).to(FirstClassInSubModule21.class)
                                                .from(FirstClassInSubModule12.class).to(FirstClassInSubModule21[].class)
                                                .from(FirstClassInSubModule12.class).to(FirstClassInSubModule21[][].class)
                                ));
    }

    @Test
    public void all_dependencies_not_covered_by_module_dependencies_are_considered_undefined() {
        ArchModules modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule module = modules.getByIdentifier("module1", "sub1");

        assertThatDependencies(module.getUndefinedDependencies())
                .containOnly(
                        from(FirstClassInSubModule11.class).to(String.class)
                                .from(FirstClassInSubModule11.class).to(Object.class)
                                .from(FirstClassInSubModule11.class).to(List.class)
                                .from(SecondClassInSubModule11.class).to(Collection.class)
                                .from(SecondClassInSubModule11.class).to(FirstClassInModule2.class)
                                .from(SecondClassInSubModule11.class).to(Object.class));
    }

    private static ModuleDependenciesAssertion assertThatModuleDependencies(Collection<? extends ModuleDependency> dependencies) {
        return new ModuleDependenciesAssertion(dependencies);
    }

    static class ModuleDependenciesAssertion extends
            AbstractObjectAssert<ModuleDependenciesAssertion, Collection<? extends ModuleDependency>> {

        ModuleDependenciesAssertion(Collection<? extends ModuleDependency> dependencies) {
            super(dependencies, ModuleDependenciesAssertion.class);
        }

        void containOnlyModuleDependencies(ExpectedModuleDependency... expectedModuleDependencies) {
            assertThat(actual).as("actual module dependencies").hasSameSizeAs(expectedModuleDependencies);

            List<ExpectedModuleDependency> unmatchedDependencies = newArrayList(expectedModuleDependencies);
            unmatchedDependencies.removeIf(expectedModuleDependency -> actual.stream().anyMatch(expectedModuleDependency::matches));
            assertThat(unmatchedDependencies).as("unmatched module dependencies").isEmpty();
        }

        static class ExpectedModuleDependency {
            private final Identifier origin;
            private final Identifier target;
            private final Optional<ExpectedDependencies> expectedDependencies;

            private ExpectedModuleDependency(Identifier origin, Identifier target) {
                this(origin, target, Optional.empty());
            }

            private ExpectedModuleDependency(Identifier origin, Identifier target,
                    Optional<ExpectedDependencies> expectedDependencies) {
                this.origin = origin;
                this.target = target;
                this.expectedDependencies = expectedDependencies;
            }

            static Creator from(String... identifier) {
                return new Creator(Identifier.from(identifier));
            }

            boolean matches(ModuleDependency moduleDependency) {
                if (!moduleDependency.getOrigin().getIdentifier().equals(origin) || !moduleDependency.getTarget().getIdentifier().equals(target)) {
                    return false;
                }
                if (!expectedDependencies.isPresent()) {
                    return true;
                }

                Set<Dependency> actualClassDependencies = moduleDependency.toClassDependencies();
                ExpectedDependencies.MatchResult result = expectedDependencies.get().match(actualClassDependencies);
                return result.matchesExactly();
            }

            @Override
            public String toString() {
                return String.format("Expected Module Dependency [%s -> %s] {%s}", origin, target, expectedDependencies);
            }

            public ExpectedModuleDependency withClassDependencies(ExpectedDependencies expectedDependencies) {
                return new ExpectedModuleDependency(origin, target, Optional.of(expectedDependencies));
            }

            static class Creator {
                private final Identifier origin;

                Creator(Identifier origin) {
                    this.origin = origin;
                }

                ExpectedModuleDependency to(String... identifier) {
                    return new ExpectedModuleDependency(origin, Identifier.from(identifier));
                }
            }
        }
    }
}
