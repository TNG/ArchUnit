package com.tngtech.archunit.library.modules.syntax;

import java.util.function.Function;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotation;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.TestAnnotationCustomName;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.one.ClassOne;
import com.tngtech.archunit.library.modules.syntax.testexamples.test_modules.two.ClassTwo;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
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

    private static <D extends ArchModule.Descriptor> ArchCondition<ArchModule<D>> reportAllAsViolations(Function<ArchModule<D>, String> reportModule) {
        return new ArchCondition<ArchModule<D>>("report all as violations") {
            @Override
            public void check(ArchModule<D> module, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(module, reportModule.apply(module)));
            }
        };
    }

}
