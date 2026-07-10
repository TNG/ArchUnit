package com.tngtech.archunit.library.modules.syntax;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotation;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotationCustomName;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.ClassOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.one.ClassOneOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.one.ClassTwoOne;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.library.modules.syntax.ModuleDependencyScope.consideringOnlyDependenciesBetweenModules;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;

public class ModuleRuleTest {

    @Test
    public void definedByPackages_default_name() {
        assertThatRule(
                modules()
                        .definedByPackages("..test_modules.(*).(*)..")
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations("one:one", "one:two", "two:one");
    }

    @Test
    public void definedByPackages_custom_name() {
        assertThatRule(
                modules()
                        .definedByPackages("..test_modules.(*).(*)..")
                        .derivingNameFromPattern("Test-Module($1-$2)")
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations("Test-Module(one-one)", "Test-Module(one-two)", "Test-Module(two-one)");
    }

    @Test
    public void definedByRootClasses_default_name() {
        assertThatRule(
                modules()
                        .definedByRootClasses(equivalentTo(ClassOne.class).or(equivalentTo(ClassTwo.class)))
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations(ClassOne.class.getPackage().getName(), ClassTwo.class.getPackage().getName());
    }

    @Test
    public void definedByRootClasses_custom_name() {
        assertThatRule(
                modules()
                        .definedByRootClasses(equivalentTo(ClassOne.class).or(equivalentTo(ClassTwo.class)))
                        .derivingModuleFromRootClassBy(DescribedFunction.describe("simple class name", javaClass -> ArchModule.Descriptor.create(javaClass.getSimpleName())))
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations(ClassOne.class.getSimpleName(), ClassTwo.class.getSimpleName());
    }

    @Test
    public void definedByAnnotation_default_name() {
        assertThatRule(
                modules()
                        .definedByAnnotation(TestAnnotation.class)
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations("one", "two");
    }

    @Test
    public void definedByAnnotation_custom_name() {
        assertThatRule(
                modules()
                        .definedByAnnotation(TestAnnotationCustomName.class, TestAnnotationCustomName::customName)
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations("customOne", "customTwo");
    }

    @Test
    public void definedBy_allows_generic_customization() {
        assertThatRule(
                modules()
                        .definedBy(DescribedFunction.describe("simple class name",
                                javaClass -> ArchModule.Identifier.from(javaClass.getSimpleName())))
                        .derivingModule(DescriptorFunction.describe("from identifier",
                                (identifier, __) -> ArchModule.Descriptor.create(identifier.getPart(1))))
                        .should(reportAllAsViolations(ArchModule::getName))
        )
                .checking(new ClassFileImporter().importClasses(ClassOne.class, ClassTwo.class))
                .hasOnlyViolations(ClassOne.class.getSimpleName(), ClassTwo.class.getSimpleName());
    }

    @Test
    public void ignoring_dependencies_can_be_applied_before_other_methods() {
        assertThatRule(
                modules()
                        .definedByAnnotation(TestAnnotation.class)
                        .should().respectTheirAllowedDependencies(alwaysFalse(), consideringOnlyDependenciesBetweenModules())
                        .ignoreDependency(equivalentTo(ClassOne.class), alwaysTrue())
                        .ignoreDependency(equivalentTo(ClassOneOne.class), alwaysTrue())
                        .ignoreDependency(equivalentTo(ClassTwoOne.class), alwaysTrue())
                        .because("reason")
                        .as("description")
                        .allowEmptyShould(false)
        )
                .checking(new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class))
                .hasNoViolation();
    }

    static Stream<ModulesRule<?>> rules() {
        return Stream.of(
                modules().definedByPackages("..test_modules.(*).(*)..").should().respectTheirAllowedDependencies(alwaysFalse(), consideringOnlyDependenciesBetweenModules()),
                modules().definedByPackages("..test_modules.(*).(*)..").should().beFreeOfCycles());
    }

    @ParameterizedTest
    @MethodSource("rules")
    void handles_violations_as_dependencies(ModulesRule<?> rule) {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(ClassOne.class, ClassTwo.class);

        Set<Dependency> reportedDependencies = new HashSet<>();
        rule.evaluate(classes).handleViolations(
                (Collection<Dependency> dependencies, String __) -> reportedDependencies.addAll(dependencies)
        );

        assertThatDependencies(reportedDependencies)
                .contain(ClassOneOne.class, ClassTwoOne.class)
                .contain(ClassTwoOne.class, ClassOneOne.class);
    }

    private static <D extends ArchModule.Descriptor> ArchCondition<ArchModule<D>> reportAllAsViolations(Function<ArchModule<D>, String> reportModule) {
        return new ArchCondition<ArchModule<D>>("report all as violations") {
            @Override
            public void check(ArchModule<D> module, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(module, reportModule.apply(module)));
            }
        };
    }

}
