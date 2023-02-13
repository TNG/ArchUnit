package com.tngtech.archunit.library.modules.syntax;

import java.util.function.Predicate;

import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.library.modules.syntax.ModuleRuleDefinition.modules;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;

public class GivenModulesTest {

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();

    // note that description logic is already covered by RandomModulesSyntaxTest

    @Test
    public void allows_restricting_modules() {
        archConfigurationRule.setFailOnEmptyShould(false);

        assertThatRule(modulesByClassName().should(alwaysBeViolations()))
                .checking(importClasses(Object.class, String.class))
                .hasNumberOfViolations(2);

        assertThatRule(
                modulesByClassName()
                        .that(getPredicate(m -> m.getName().contains("Object")))
                        .should(alwaysBeViolations()))
                .checking(importClasses(Object.class, String.class))
                .hasNumberOfViolations(1);

        assertThatRule(
                modulesByClassName()
                        .that(getPredicate(m -> m.getName().contains("Object")))
                        .or(getPredicate(m -> m.getName().contains("String")))
                        .should(alwaysBeViolations()))
                .checking(importClasses(Object.class, String.class))
                .hasNumberOfViolations(2);

        assertThatRule(
                modulesByClassName()
                        .that(getPredicate(m -> m.getName().contains("Object")))
                        .and(getPredicate(m -> m.getName().contains("String")))
                        .should(alwaysBeViolations()))
                .checking(importClasses(Object.class, String.class))
                .hasNoViolation();
    }

    private static DescribedPredicate<ArchModule<ArchModule.Descriptor>> getPredicate(Predicate<ArchModule<ArchModule.Descriptor>> predicate) {
        return DescribedPredicate.describe("", predicate);
    }

    static GivenModules<ArchModule.Descriptor> modulesByClassName() {
        return modules().definedBy(getClassName()).derivingModule(fromClassName());
    }

    private static DescriptorFunction<ArchModule.Descriptor> fromClassName() {
        return DescriptorFunction.describe("from class name", (identifier, __) -> ArchModule.Descriptor.create(identifier.getPart(1)));
    }

    private static DescribedFunction<JavaClass, ArchModule.Identifier> getClassName() {
        return DescribedFunction.describe("class name", clazz -> ArchModule.Identifier.from(clazz.getName()));
    }

    private static ArchCondition<ArchModule<?>> alwaysBeViolations() {
        return new ArchCondition<ArchModule<?>>("always be violations") {
            @Override
            public void check(ArchModule<?> module, ConditionEvents events) {
                events.add(violated(module, "violation of " + module.getName()));
            }
        };
    }
}
