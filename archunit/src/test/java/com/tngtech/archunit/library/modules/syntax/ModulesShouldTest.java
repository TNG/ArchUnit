package com.tngtech.archunit.library.modules.syntax;

import java.util.function.Function;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.TestModule;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module1.ClassInModule1;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module2.InternalClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module2.api.ApiClassInModule2;
import com.tngtech.archunit.library.modules.testexamples.default_annotation.module3.ClassInModule3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.library.modules.syntax.GivenModulesTest.modulesByClassName;
import static com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope.consideringAllDependencies;
import static com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope.consideringOnlyDependenciesBetweenModules;
import static com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ModulesShouldTest {

    @Test
    public void respectTheirAllowedDependencies_considering_all_dependencies() {
        assertThatRule(modulesByClassName().should().respectTheirAllowedDependencies(alwaysFalse(), consideringAllDependencies()))
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasViolationContaining(String.class.getName())
                .hasViolationContaining(ArchRule.class.getName());
    }

    @Test
    public void respectTheirAllowedDependencies_considering_only_dependencies_between_modules() {
        assertThatRule(modulesByClassName().should().respectTheirAllowedDependencies(alwaysFalse(), consideringOnlyDependenciesBetweenModules()))
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasNoViolationContaining(String.class.getName())
                .hasNoViolationContaining(ArchRule.class.getName());
    }

    @Test
    public void respectTheirAllowedDependencies_considering_only_dependencies_in_packages() {
        assertThatRule(modulesByClassName().should().respectTheirAllowedDependencies(
                alwaysFalse(),
                consideringOnlyDependenciesInAnyPackage(getClass().getPackage().getName() + "..", ArchRule.class.getPackage().getName() + "..")
        ))
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasNoViolationContaining(String.class.getName())
                .hasViolationContaining(ArchRule.class.getName());
    }

    @Test
    public void respectTheirAllowedDependenciesDeclaredIn_takes_allowed_dependencies_from_annotation_property() {
        assertThatRule(modules().definedByAnnotation(TestModule.class)
                .should().respectTheirAllowedDependenciesDeclaredIn("allowedDependencies", consideringOnlyDependenciesBetweenModules()))
                .checking(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class, ClassInModule3.class))
                .hasViolationContaining(ClassInModule3.class.getName())
                .hasNoViolationContaining(ApiClassInModule2.class.getName())
                .hasNoViolationContaining(InternalClassInModule2.class.getName());
    }

    @Test
    public void respectTheirAllowedDependenciesDeclaredIn_works_together_with_filtering_by_predicate() {
        assertThatRule(modules().definedByAnnotation(TestModule.class)
                .that(DescribedPredicate.describe("are not Module 1", it -> !it.getName().equals("Module 1")))
                .and(DescribedPredicate.describe("are not Module 2", it -> !it.getName().equals("Module 2")))
                .or(DescribedPredicate.describe("are Module 3", it -> it.getName().equals("Module 3")))
                .should().respectTheirAllowedDependenciesDeclaredIn("allowedDependencies", consideringOnlyDependenciesBetweenModules()))
                .checking(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class, ClassInModule3.class))
                .hasNoViolation();
    }

    @Test
    public void respectTheirAllowedDependenciesDeclaredIn_rejects_missing_property() {
        assertThatThrownBy(
                () -> modules().definedByAnnotation(TestModule.class)
                        .should().respectTheirAllowedDependenciesDeclaredIn("notThere", consideringOnlyDependenciesBetweenModules())
                        .evaluate(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("Could not invoke @%s.notThere()", TestModule.class.getSimpleName()));
    }

    @Test
    public void respectTheirAllowedDependenciesDeclaredIn_rejects_property_of_wrong_type() {
        assertThatThrownBy(
                () -> modules().definedByAnnotation(TestModule.class)
                        .should().respectTheirAllowedDependenciesDeclaredIn("name", consideringOnlyDependenciesBetweenModules())
                        .evaluate(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("Property @%s.name() must be of type %s", TestModule.class.getSimpleName(), String[].class.getSimpleName()));
    }

    @Test
    public void onlyDependOnEachOtherThroughPackagesDeclaredIn_takes_allowed_dependencies_from_annotation_property() {
        assertThatRule(modules().definedByAnnotation(TestModule.class)
                .should().onlyDependOnEachOtherThroughPackagesDeclaredIn("exposedPackages"))
                .checking(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class))
                .hasViolationContaining(InternalClassInModule2.class.getName())
                .hasNoViolationContaining(ApiClassInModule2.class.getName());
    }

    @Test
    public void onlyDependOnEachOtherThroughPackagesDeclaredIn_rejects_missing_property() {
        assertThatThrownBy(
                () -> modules().definedByAnnotation(TestModule.class)
                        .should().onlyDependOnEachOtherThroughPackagesDeclaredIn("notThere")
                        .evaluate(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("Could not invoke @%s.notThere()", TestModule.class.getSimpleName()));
    }

    @Test
    public void onlyDependOnEachOtherThroughPackagesDeclaredIn_rejects_property_of_wrong_type() {
        assertThatThrownBy(
                () -> modules().definedByAnnotation(TestModule.class)
                        .should().onlyDependOnEachOtherThroughPackagesDeclaredIn("name")
                        .evaluate(new ClassFileImporter().importPackagesOf(ClassInModule1.class, InternalClassInModule2.class))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("Property @%s.name() must be of type %s", TestModule.class.getSimpleName(), String[].class.getSimpleName()));
    }

    static Stream<Function<ModulesRule<?>, ModulesRule<?>>> ruleModificationsToIgnoreViolationsFromModuleTwoToArchRule() {
        return Stream.of(
                modulesRule -> modulesRule.ignoreDependency(ModuleTwo.class, ArchRule.class),
                modulesRule -> modulesRule.ignoreDependency(ModuleTwo.class.getName(), ArchRule.class.getName()),
                modulesRule -> modulesRule.ignoreDependency(equivalentTo(ModuleTwo.class), equivalentTo(ArchRule.class)),
                modulesRule -> modulesRule.ignoreDependency(dependency(ModuleTwo.class, ArchRule.class))
        );
    }

    @ParameterizedTest
    @MethodSource("ruleModificationsToIgnoreViolationsFromModuleTwoToArchRule")
    void respectTheirAllowedDependencies_ignores_dependencies(Function<ModulesRule<?>, ModulesRule<?>> modifyRuleToIgnoreViolationsFromModuleTwoToArchRule) {
        ModulesRule<?> rule = modulesByClassName().should().respectTheirAllowedDependencies(alwaysFalse(), consideringAllDependencies());

        rule = modifyRuleToIgnoreViolationsFromModuleTwoToArchRule.apply(rule);

        assertThatRule(rule)
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasViolationContaining(String.class.getName())
                .hasNoViolationContaining(ArchRule.class.getName());
    }

    @Test
    public void respectTheirAllowedDependencies_filtered_by_that_ignores_dependencies() {
        assertThatRule(
                modulesByClassName()
                        .that(describe("are not Module Two", m -> !m.getName().endsWith(ModuleTwo.class.getSimpleName())))
                        .should().respectTheirAllowedDependencies(alwaysFalse(), consideringAllDependencies())
        )
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasViolationContaining(String.class.getName())
                .hasNoViolationContaining(ArchRule.class.getName());
    }

    static Stream<ModulesRule<?>> onlyDependOnEachOtherThroughClassesThat_ignores_dependencies() {
        return Stream.of(
                modulesByClassName().should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName()))),
                modulesByClassName().should().onlyDependOnEachOtherThroughClassesThat().haveSimpleName(ModuleTwo.class.getSimpleName())
        );
    }

    @ParameterizedTest
    @MethodSource
    void onlyDependOnEachOtherThroughClassesThat_ignores_dependencies(ModulesRule<?> rule) {
        rule = rule
                .ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"));

        assertThatRule(rule)
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasOnlyOneViolationContaining("cyclicDependencyTwo")
                .hasNoViolationContaining("cyclicDependencyOne");

        rule = rule.ignoreDependency(d -> d.getDescription().contains("cyclicDependencyTwo"));

        assertThatRule(rule)
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasNoViolation();
    }

    @Test
    public void beFreeOfCycles_ignores_dependencies() {
        ModulesRule<?> rule = modulesByClassName().should().beFreeOfCycles().ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"));

        assertThatRule(rule)
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasOnlyOneViolationContaining("Cycle detected")
                .hasViolationContaining("cyclicDependencyTwo")
                .hasNoViolationContaining("cyclicDependencyOne");

        rule = rule.ignoreDependency(d -> d.getDescription().contains("cyclicDependencyTwo"));

        assertThatRule(rule)
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasNoViolation();
    }

    @Test
    public void andShould_joins_predefined_conditions() {
        assertThatRule(
                modulesByClassName()
                        .should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName())))
                        .andShould().beFreeOfCycles()
        )
                .hasDescriptionContaining("only depend on each other")
                .hasDescriptionContaining("be free of cycles")
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                // from checking for cycles we should get a violation starting with a '-'
                .hasViolationContaining("- Field <%s.cyclicDependencyOne>", ModuleTwo.class.getName())
                // from checking how modules depend on each other we should get the same violation, but not starting with a '-'
                .hasViolationMatching("^" + quote("Field <" + ModuleTwo.class.getName() + ".cyclicDependencyOne>") + ".*");
    }

    @Test
    public void andShould_joins_custom_condition() {
        assertThatRule(
                modulesByClassName()
                        .should().beFreeOfCycles()
                        .andShould(new ArchCondition<ArchModule<ArchModule.Descriptor>>("not contain 'cyclicDependencyOne'") {
                            @Override
                            public void check(ArchModule<ArchModule.Descriptor> module, ConditionEvents events) {
                                module.getClassDependenciesFromSelf()
                                        .stream().filter(it -> it.getDescription().contains("cyclicDependencyOne"))
                                        .forEach(it -> events.add(SimpleConditionEvent.violated(it, "custom: cyclicDependencyOne")));
                            }
                        })
        )
                .hasDescriptionContaining("be free of cycles")
                .hasDescriptionContaining("not contain 'cyclicDependencyOne'")
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                // from checking for cycles we should get a violation starting with a '-'
                .hasViolationContaining("- Field <%s.cyclicDependencyOne>", ModuleTwo.class.getName())
                .hasViolation("custom: cyclicDependencyOne");
    }

    @Test
    public void andShould_only_ignores_dependencies_of_last_condition() {
        assertThatRule(
                modulesByClassName()
                        .should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName())))
                        .ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"))
                        .andShould().beFreeOfCycles()
        )
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasViolationContaining("cyclicDependencyOne");

        assertThatRule(
                modulesByClassName()
                        .should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName())))
                        .andShould().beFreeOfCycles()
                        .ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"))
        )
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasViolationContaining("cyclicDependencyOne");

        assertThatRule(
                modulesByClassName()
                        .should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName())))
                        .ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"))
                        .andShould().beFreeOfCycles()
                        .ignoreDependency(d -> d.getDescription().contains("cyclicDependencyOne"))
        )
                .checking(new ClassFileImporter().importClasses(ModuleOne.class, ModuleTwo.class))
                .hasNoViolationContaining("cyclicDependencyOne");
    }

    @SuppressWarnings("unused")
    private static class ModuleOne {
        ModuleTwo dependencyToOtherModule;
        String dependencyToStandardJavaClass;
    }

    @SuppressWarnings("unused")
    private static class ModuleTwo {
        ModuleOne cyclicDependencyOne;
        ModuleOne cyclicDependencyTwo;
        ArchRule dependencyInOtherArchUnitPackage;
    }
}
