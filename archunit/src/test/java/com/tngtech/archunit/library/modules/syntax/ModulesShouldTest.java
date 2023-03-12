package com.tngtech.archunit.library.modules.syntax;

import java.util.function.Function;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
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

    @DataProvider
    public static Object[][] ruleModificationsToIgnoreViolationsFromModuleTwoToArchRule() {
        return testForEach(
                (Function<ModulesRule<?>, ModulesRule<?>>) modulesRule -> modulesRule.ignoreDependency(ModuleTwo.class, ArchRule.class),
                (Function<ModulesRule<?>, ModulesRule<?>>) modulesRule -> modulesRule.ignoreDependency(ModuleTwo.class.getName(), ArchRule.class.getName()),
                (Function<ModulesRule<?>, ModulesRule<?>>) modulesRule -> modulesRule.ignoreDependency(equivalentTo(ModuleTwo.class), equivalentTo(ArchRule.class)),
                (Function<ModulesRule<?>, ModulesRule<?>>) modulesRule -> modulesRule.ignoreDependency(dependency(ModuleTwo.class, ArchRule.class))
        );
    }

    @Test
    @UseDataProvider("ruleModificationsToIgnoreViolationsFromModuleTwoToArchRule")
    public void respectTheirAllowedDependencies_ignores_dependencies(Function<ModulesRule<?>, ModulesRule<?>> modifyRuleToIgnoreViolationsFromModuleTwoToArchRule) {
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

    @DataProvider
    public static Object[][] data_onlyDependOnEachOtherThroughClassesThat_ignores_dependencies() {
        return testForEach(
                modulesByClassName().should().onlyDependOnEachOtherThroughClassesThat(have(simpleName(ModuleTwo.class.getSimpleName()))),
                modulesByClassName().should().onlyDependOnEachOtherThroughClassesThat().haveSimpleName(ModuleTwo.class.getSimpleName())
        );
    }

    @Test
    @UseDataProvider
    public void test_onlyDependOnEachOtherThroughClassesThat_ignores_dependencies(ModulesRule<?> rule) {
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
