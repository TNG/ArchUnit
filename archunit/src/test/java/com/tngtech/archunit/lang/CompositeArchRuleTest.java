package com.tngtech.archunit.lang;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.Priority.HIGH;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class CompositeArchRuleTest {
    private static final boolean SATISFIED = true;
    private static final boolean UNSATISFIED = false;

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();

    @DataProvider
    public static Object[][] rules_to_AND() {
        return $$(
                $(archRuleThatSucceeds(), archRuleThatSucceeds(), SATISFIED),
                $(archRuleThatSucceeds(), archRuleThatFails(), UNSATISFIED),
                $(archRuleThatFails(), archRuleThatSucceeds(), UNSATISFIED),
                $(archRuleThatFails(), archRuleThatFails(), UNSATISFIED)
        );
    }

    @Test
    @UseDataProvider("rules_to_AND")
    public void rules_are_ANDed(ArchRule first, ArchRule second, boolean expectedSatisfied) {
        EvaluationResult result = CompositeArchRule.of(first).and(second).evaluate(importClasses(getClass()));

        assertThat(result.hasViolation()).as("result has violation").isEqualTo(!expectedSatisfied);
        assertPriority(result.getFailureReport().toString(), MEDIUM);
    }

    @Test
    @UseDataProvider("rules_to_AND")
    public void archRuleCollection(ArchRule first, ArchRule second, boolean expectedSatisfied) {
        List<ArchRule> ruleCollection = ImmutableList.of(first, second);
        EvaluationResult result = CompositeArchRule.of(ruleCollection).evaluate(importClasses(getClass()));

        assertThat(result.hasViolation()).as("result has violation").isEqualTo(!expectedSatisfied);
        assertPriority(result.getFailureReport().toString(), MEDIUM);
    }

    @Test
    public void description_is_modified_correctly() {
        ArchRule input = classes().should().bePublic();
        CompositeArchRule start = CompositeArchRule.of(input);

        assertThat(start.getDescription()).isEqualTo(input.getDescription());

        CompositeArchRule modified = start.and(classes().should().bePrivate().as("inner changed"));

        assertThat(modified.getDescription()).isEqualTo(start.getDescription() + " and inner changed");

        modified = modified.as("overridden")
                .because("reason")
                .and(classes().should().bePublic().as("changed"));

        assertThat(modified.getDescription()).isEqualTo("overridden, because reason and changed");
    }

    @Test
    public void priority_is_passed() {
        Priority priority = HIGH;
        CompositeArchRule rule = CompositeArchRule
                .priority(priority)
                .of(classes().should().bePublic()).and(classes().should().bePrivate());

        String failureMessage = rule.evaluate(importClasses(getClass())).getFailureReport().toString();

        assertPriority(failureMessage, priority);
    }

    @Test
    public void fails_on_empty_should_by_default() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                compositeRuleWithPartialEmptyShould().check(new ClassFileImporter().importClasses(Object.class));
            }
        }).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failed to check any classes");
    }

    @Test
    public void should_allow_empty_should_if_configured() {
        archConfigurationRule.setFailOnEmptyShould(false);

        compositeRuleWithPartialEmptyShould().check(new ClassFileImporter().importClasses(Object.class));
    }

    @Test
    public void allows_empty_should_if_overridden_by_rule() {
        archConfigurationRule.setFailOnEmptyShould(true);

        compositeRuleWithPartialEmptyShould().allowEmptyShould(true).check(new ClassFileImporter().importClasses(Object.class));
    }

    private static CompositeArchRule compositeRuleWithPartialEmptyShould() {
        return CompositeArchRule
                .of(classes().should().bePublic())
                .and(classes().that(DescribedPredicate.<JavaClass>alwaysFalse()).should().bePublic());
    }

    private void assertPriority(String failureMessage, Priority priority) {
        assertThat(failureMessage).contains(String.format("[Priority: %s]", priority));
    }

    private static ArchRule archRuleThatSucceeds() {
        return createArchRuleWithSatisfied(true);
    }

    private static ArchRule archRuleThatFails() {
        return createArchRuleWithSatisfied(false);
    }

    private static ArchRule createArchRuleWithSatisfied(final boolean satisfied) {
        return ArchRule.Factory.create(new AbstractClassesTransformer<JavaClass>("irrelevant") {
            @Override
            public Iterable<JavaClass> doTransform(JavaClasses collection) {
                return collection;
            }
        }, new ArchCondition<JavaClass>("irrelevant") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                events.add(new SimpleConditionEvent(item, satisfied, "irrelevant"));
            }
        }, Priority.MEDIUM).as(String.format("%s rule", satisfied ? "satisfied" : "failing"));
    }
}
