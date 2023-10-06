package com.tngtech.archunit.library.modules;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.modules.ArchModule.Identifier;
import com.tngtech.archunit.library.modules.testexamples.MyModule;
import com.tngtech.archunit.library.modules.testexamples.annotation_with_custom_name.MyModuleWithCustomName;
import com.tngtech.archunit.library.modules.testexamples.annotation_with_custom_name.module1.ModuleOneDescriptorCustomName;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.FirstClassInModule1;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.ModuleOneDescriptor;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.SecondClassInModule1;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1.FirstClassInSubModule11;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub1.SecondClassInSubModule11;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.FirstClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.valid.module1.sub2.SecondClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.FirstClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.ModuleTwoDescriptor;
import com.tngtech.archunit.library.modules.testexamples.valid.module2.sub1.FirstClassInSubModule21;
import com.tngtech.archunit.testutil.assertion.DependenciesAssertion.ExpectedDependencies;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.tngtech.archunit.library.modules.ArchModulesTest.ModuleDependenciesAssertion.ExpectedModuleDependency.from;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.assertion.DependenciesAssertion.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchModulesTest {
    private final String validTestExamplePackage = getExamplePackage("valid");
    private final JavaClasses testExamples = new ClassFileImporter().importPackages(validTestExamplePackage);

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void partitions_modules_by_single_package_not_including_subpackages() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1");

        assertThatTypes(module).matchInAnyOrder(
                ModuleOneDescriptor.class,
                FirstClassInModule1.class,
                SecondClassInModule1.class);

        assertThat(modules.tryGetByIdentifier("module1")).contains((ArchModule) module);
        assertThat(modules.tryGetByIdentifier("absent")).isEmpty();
    }

    @Test
    public void partitions_modules_by_single_package_each_including_subpackages() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*)..")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1");

        assertThatTypes(module).matchInAnyOrder(
                ModuleOneDescriptor.class,
                FirstClassInModule1.class,
                SecondClassInModule1.class,
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class,
                FirstClassInSubModule12.class,
                SecondClassInSubModule12.class);
    }

    @Test
    public void partitions_modules_by_multiple_separate_packages() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1", "sub1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByIdentifier("module1")).isEmpty();
        assertThat(modules.tryGetByIdentifier("module1.sub")).isEmpty();
        assertThat(modules.tryGetByIdentifier("module1:sub")).isEmpty();
    }

    @Test
    public void partitions_modules_by_multiple_unified_packages() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(**)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1.sub1");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByIdentifier("module1", "sub1")).isEmpty();
    }

    @Test
    public void names_modules_by_default() {
        ArchModules<?> modules = ArchModules
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
        ArchModules<?> modules = ArchModules
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
        ArchModules<?> modules = ArchModules
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void allows_retrieving_modules_by_name() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .deriveNameFromPattern("MyModule [$1][$2]")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByName("MyModule [module1][sub1]");

        assertThatTypes(module).matchInAnyOrder(
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class);

        assertThat(modules.tryGetByName("MyModule [module1][sub1]")).contains((ArchModule) module);
        assertThat(modules.tryGetByName("absent")).isEmpty();
    }

    @Test
    public void allows_defining_modules_by_function() {
        ArchModules<?> modules = ArchModules
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
    public void allows_defining_modules_by_root_classes() {
        ArchModules<?> modules = ArchModules
                .defineByRootClasses(javaClass -> javaClass.getSimpleName().endsWith("Descriptor"))
                .modularize(testExamples);

        assertThat(modules.getNames()).containsOnly(
                ModuleOneDescriptor.class.getPackage().getName(),
                ModuleTwoDescriptor.class.getPackage().getName()
        );

        ArchModule<?> module = modules.getByIdentifier(ModuleOneDescriptor.class.getPackage().getName());

        assertThatTypes(module).matchInAnyOrder(
                ModuleOneDescriptor.class,
                FirstClassInModule1.class,
                SecondClassInModule1.class,
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class,
                FirstClassInSubModule12.class,
                SecondClassInSubModule12.class);
    }

    @Test
    public void rejects_overlapping_modules_by_root_classes() {
        JavaClasses invalidExamples = new ClassFileImporter().importPackages(getExamplePackage("invalid"));

        assertThatThrownBy(
                () -> ArchModules
                        .defineByRootClasses(javaClass -> javaClass.getSimpleName().endsWith("Descriptor"))
                        .modularize(invalidExamples)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modules would overlap")
                .hasMessageContaining(
                        com.tngtech.archunit.library.modules.testexamples.invalid.overlapping_root_classes.ModuleOneDescriptor.class.getPackage().getName())
                .hasMessageContaining(
                        com.tngtech.archunit.library.modules.testexamples.invalid.overlapping_root_classes.child.ModuleTwoDescriptor.class.getPackage().getName());
    }

    @Test
    public void allows_defining_modules_by_annotations() {
        ArchModules<AnnotationDescriptor<MyModule>> modules = ArchModules
                .defineByAnnotation(MyModule.class)
                .modularize(testExamples);

        ArchModule<AnnotationDescriptor<MyModule>> module = modules.getByIdentifier(ModuleOneDescriptor.class.getPackage().getName());

        String expectedModuleName = ModuleOneDescriptor.class.getAnnotation(MyModule.class).name();
        assertThat(module.getName()).isEqualTo(expectedModuleName);
        assertThat(module.getDescriptor().getAnnotation().name()).isEqualTo(expectedModuleName);

        assertThatTypes(module).matchInAnyOrder(
                ModuleOneDescriptor.class,
                FirstClassInModule1.class,
                SecondClassInModule1.class,
                FirstClassInSubModule11.class,
                SecondClassInSubModule11.class,
                FirstClassInSubModule12.class,
                SecondClassInSubModule12.class);
    }

    @Test
    public void rejects_defining_modules_by_annotations_when_name_property_can_not_be_derived() {
        JavaClasses classes = new ClassFileImporter().importPackages(getExamplePackage("annotation_with_custom_name"));

        assertInvalidAnnotationDefinitionByDefaultNameProperty(MyModuleWithCustomName.class, classes);
    }

    @Test
    public void rejects_defining_modules_by_annotations_when_name_property_has_incompatible_type() {
        @AnnotationWithIncompatibleNameProperty(name = 42)
        class RootClassWithIncompatibleAnnotation {
        }
        JavaClasses classes = new ClassFileImporter().importClasses(AnnotationWithIncompatibleNameProperty.class, RootClassWithIncompatibleAnnotation.class);

        assertInvalidAnnotationDefinitionByDefaultNameProperty(AnnotationWithIncompatibleNameProperty.class, classes);
    }

    private static void assertInvalidAnnotationDefinitionByDefaultNameProperty(Class<? extends Annotation> annotationType, JavaClasses classes) {
        assertThatThrownBy(
                () -> ArchModules
                        .defineByAnnotation(annotationType)
                        .modularize(classes)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@" + annotationType.getSimpleName() + ".name()")
                .hasMessageContaining("Supplied annotation must provide a method 'String name()'")
                .hasMessageContaining("defineByAnnotation(annotationType, nameFunction)");
    }

    @Test
    public void allows_defining_modules_by_annotations_with_customized_name_property() {
        JavaClasses invalidExamples = new ClassFileImporter().importPackages(getExamplePackage("annotation_with_custom_name"));

        ArchModules<AnnotationDescriptor<MyModuleWithCustomName>> modules = ArchModules
                .defineByAnnotation(MyModuleWithCustomName.class, MyModuleWithCustomName::customName)
                .modularize(invalidExamples);

        ArchModule<?> module = modules.getByIdentifier(ModuleOneDescriptorCustomName.class.getPackage().getName());

        String expectedModuleName = ModuleOneDescriptorCustomName.class.getAnnotation(MyModuleWithCustomName.class).customName();
        assertThat(module.getName()).isEqualTo(expectedModuleName);
    }

    @Test
    public void rejects_overlapping_modules_by_annotations() {
        JavaClasses invalidExamples = new ClassFileImporter().importPackages(getExamplePackage("invalid"));

        assertThatThrownBy(
                () -> ArchModules
                        .defineByAnnotation(MyModule.class)
                        .modularize(invalidExamples)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modules would overlap")
                .hasMessageContaining(
                        com.tngtech.archunit.library.modules.testexamples.invalid.overlapping_root_classes.ModuleOneDescriptor.class.getPackage().getName())
                .hasMessageContaining(
                        com.tngtech.archunit.library.modules.testexamples.invalid.overlapping_root_classes.child.ModuleTwoDescriptor.class.getPackage().getName());
    }

    @Test
    public void allows_customizing_modules_defined_by_root_classes_from_all_classes_of_the_module() {
        ArchModules<TestModuleDescriptor> modules = ArchModules
                .defineByRootClasses(javaClass -> javaClass.getSimpleName().endsWith("Descriptor"))
                .describeBy((__, containedClasses) -> {
                    JavaClass descriptorClass = containedClasses.stream()
                            .filter(it -> it.getSimpleName().endsWith("Descriptor"))
                            .collect(onlyElement());
                    String name = getValue(descriptorClass.getField("name").reflect());
                    return new TestModuleDescriptor(name, descriptorClass);
                })
                .modularize(testExamples);

        ArchModule<TestModuleDescriptor> module = modules.getByIdentifier(ModuleOneDescriptor.class.getPackage().getName());

        assertThat(module.getName()).isEqualTo(ModuleOneDescriptor.name);
        assertThat(module.getDescriptor().getDescriptorClass().getName()).isEqualTo(ModuleOneDescriptor.class.getName());
    }

    @Test
    public void allows_customizing_modules_defined_by_root_classes_directly_from_root_class() {
        ArchModules<TestModuleDescriptor> modules = ArchModules
                .defineByRootClasses(javaClass -> javaClass.getSimpleName().endsWith("Descriptor"))
                .describeModuleByRootClass((__, descriptorClass) -> {
                    String name = getValue(descriptorClass.getField("name").reflect());
                    return new TestModuleDescriptor(name, descriptorClass);
                })
                .modularize(testExamples);

        ArchModule<TestModuleDescriptor> module = modules.getByIdentifier(ModuleOneDescriptor.class.getPackage().getName());

        assertThat(module.getName()).isEqualTo(ModuleOneDescriptor.name);
        assertThat(module.getDescriptor().getDescriptorClass().getName()).isEqualTo(ModuleOneDescriptor.class.getName());
    }

    @Test
    public void provides_class_dependencies_from_self() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1", "sub1");

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
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module2", "sub1");

        assertThatDependencies(module.getClassDependenciesToSelf())
                .containOnly(from(SecondClassInSubModule11.class).to(FirstClassInSubModule21.class)
                        .from(FirstClassInSubModule12.class).to(FirstClassInSubModule21.class));
    }

    @Test
    public void provides_module_dependencies_from_self() {
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1", "sub1");

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
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module2", "sub1");

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
        ArchModules<?> modules = ArchModules
                .defineByPackages(validTestExamplePackage + ".(*).(*)")
                .modularize(testExamples);

        ArchModule<?> module = modules.getByIdentifier("module1", "sub1");

        assertThatDependencies(module.getUndefinedDependencies())
                .containOnly(
                        from(FirstClassInSubModule11.class).to(String.class)
                                .from(FirstClassInSubModule11.class).to(Object.class)
                                .from(FirstClassInSubModule11.class).to(List.class)
                                .from(SecondClassInSubModule11.class).to(Collection.class)
                                .from(SecondClassInSubModule11.class).to(FirstClassInModule2.class)
                                .from(SecondClassInSubModule11.class).to(Object.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(Field staticField) {
        try {
            return (T) staticField.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getExamplePackage(String subpackageName) {
        return getClass().getPackage().getName() + ".testexamples." + subpackageName;
    }

    private static ModuleDependenciesAssertion assertThatModuleDependencies(Collection<? extends ModuleDependency<?>> dependencies) {
        return new ModuleDependenciesAssertion(dependencies);
    }

    static class ModuleDependenciesAssertion extends
            AbstractObjectAssert<ModuleDependenciesAssertion, Collection<? extends ModuleDependency<?>>> {

        ModuleDependenciesAssertion(Collection<? extends ModuleDependency<?>> dependencies) {
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

            boolean matches(ModuleDependency<?> moduleDependency) {
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

    private static class TestModuleDescriptor implements ArchModule.Descriptor {
        private final String name;
        private final JavaClass descriptorClass;

        TestModuleDescriptor(String name, JavaClass descriptorClass) {
            this.name = name;
            this.descriptorClass = descriptorClass;
        }

        @Override
        public String getName() {
            return name;
        }

        JavaClass getDescriptorClass() {
            return descriptorClass;
        }
    }

    private @interface AnnotationWithIncompatibleNameProperty {
        int name();
    }
}
