package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.A;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.ClassWithVariousMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.FirstException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.ALL_METHOD_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.CONSTRUCTOR_FOUR_ARGS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.CONSTRUCTOR_ONE_ARG;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.CONSTRUCTOR_TWO_ARGS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.METHOD_FOUR_ARGS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.METHOD_ONE_ARG;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.METHOD_THREE_ARGS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.METHOD_TWO_ARGS;
import static com.tngtech.archunit.lang.syntax.elements.GivenCodeUnitsTest.oneParameterOfType;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.ALL_CONSTRUCTOR_DESCRIPTIONS;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.MembersShouldTest.parseMembers;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CodeUnitsShouldTest {

    @Test
    public void complex_code_unit_syntax() {
        EvaluationResult result = codeUnits()
                .that(doNotHaveParametersOfType(List.class))
                .should().beAnnotatedWith(A.class)
                .andShould().beProtected()
                .orShould().haveRawReturnType(String.class)
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        String failure = getOnlyElement(result.getFailureReport().getDetails());
        assertThat(failure)
                .matches(String.format(".*Constructor .*%s.* does not have modifier PROTECTED.*", quote(CONSTRUCTOR_ONE_ARG)))
                .contains("and Constructor")
                .matches(String.format(".*Constructor .*%s.* is not annotated with @A.*", quote(CONSTRUCTOR_ONE_ARG)))
                .matches(
                        String.format(".*Constructor .*%s.* does not have raw return type %s.*", quote(CONSTRUCTOR_ONE_ARG), String.class.getName()));
    }

    @Test
    public void types_match_for_methods() {
        EvaluationResult result = methods()
                .that().arePrivate()
                // we can use the super type JavaMember
                .should(new ArchCondition<JavaMember>("exist") {
                    @Override
                    public void check(JavaMember item, ConditionEvents events) {
                    }
                })
                // and have preserved out type so we can assert JavaMethod later on
                .andShould(new ArchCondition<JavaMethod>("not exist") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        events.add(SimpleConditionEvent.violated(method, "expected violation"));
                    }
                })
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(Joiner.on(" ").join(result.getFailureReport().getDetails())).contains("expected violation");
    }

    @Test
    public void types_match_for_constructors() {
        EvaluationResult result = constructors()
                .that().arePrivate()
                // we can use the super type JavaMember
                .should(new ArchCondition<JavaMember>("exist") {
                    @Override
                    public void check(JavaMember item, ConditionEvents events) {
                    }
                })
                // and have preserved out type so we can assert JavaMethod later on
                .andShould(new ArchCondition<JavaConstructor>("not exist") {
                    @Override
                    public void check(JavaConstructor constructor, ConditionEvents events) {
                        events.add(SimpleConditionEvent.violated(constructor, "expected violation"));
                    }
                })
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(Joiner.on(" ").join(result.getFailureReport().getDetails())).contains("expected violation");
    }

    @DataProvider
    public static Object[][] restricted_parameter_types_rules() {
        return $$(
                $(codeUnits().should().haveRawParameterTypes(String.class),
                        union(allMethodsExcept(METHOD_ONE_ARG), allConstructorsExcept(CONSTRUCTOR_ONE_ARG))),
                $(codeUnits().should().haveRawParameterTypes(String.class.getName()),
                        union(allMethodsExcept(METHOD_ONE_ARG), allConstructorsExcept(CONSTRUCTOR_ONE_ARG))),
                $(codeUnits().should().haveRawParameterTypes(oneParameterOfType(String.class)),
                        union(allMethodsExcept(METHOD_ONE_ARG), allConstructorsExcept(CONSTRUCTOR_ONE_ARG))),
                $(codeUnits().should().notHaveRawParameterTypes(String.class),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(codeUnits().should().notHaveRawParameterTypes(String.class.getName()),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(codeUnits().should().notHaveRawParameterTypes(oneParameterOfType(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),

                $(methods().should().haveRawParameterTypes(String.class),
                        allMethodsExcept(METHOD_ONE_ARG)),
                $(methods().should().haveRawParameterTypes(String.class.getName()),
                        allMethodsExcept(METHOD_ONE_ARG)),
                $(methods().should().haveRawParameterTypes(oneParameterOfType(String.class)),
                        allMethodsExcept(METHOD_ONE_ARG)),
                $(methods().should().notHaveRawParameterTypes(String.class),
                        ImmutableSet.of(METHOD_ONE_ARG)),
                $(methods().should().notHaveRawParameterTypes(String.class.getName()),
                        ImmutableSet.of(METHOD_ONE_ARG)),
                $(methods().should().notHaveRawParameterTypes(oneParameterOfType(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG)),

                $(constructors().should().haveRawParameterTypes(String.class),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(constructors().should().haveRawParameterTypes(String.class.getName()),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(constructors().should().haveRawParameterTypes(oneParameterOfType(String.class)),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(constructors().should().notHaveRawParameterTypes(String.class),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(constructors().should().notHaveRawParameterTypes(String.class.getName()),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(constructors().should().notHaveRawParameterTypes(oneParameterOfType(String.class)),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG))
        );
    }

    @Test
    @UseDataProvider("restricted_parameter_types_rules")
    public void parameter_types_predicates(ArchRule rule, Collection<String> expectedMembers) {
        EvaluationResult result = rule.evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualMembers = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualMembers).containsOnlyElementsOf(expectedMembers);
    }

    @DataProvider
    public static Object[][] restricted_return_type_rules() {
        return $$(
                $(codeUnits().should().haveRawReturnType(String.class),
                        union(allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS), ALL_CONSTRUCTOR_DESCRIPTIONS)),
                $(codeUnits().should().haveRawReturnType(String.class.getName()),
                        union(allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS), ALL_CONSTRUCTOR_DESCRIPTIONS)),
                $(codeUnits().should().haveRawReturnType(equivalentTo(String.class)),
                        union(allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS), ALL_CONSTRUCTOR_DESCRIPTIONS)),
                $(codeUnits().should().notHaveRawReturnType(String.class),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(codeUnits().should().notHaveRawReturnType(String.class.getName()),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(codeUnits().should().notHaveRawReturnType(equivalentTo(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(methods().should().haveRawReturnType(String.class),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(methods().should().haveRawReturnType(String.class.getName()),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(methods().should().haveRawReturnType(equivalentTo(String.class)),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(methods().should().notHaveRawReturnType(String.class),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(methods().should().notHaveRawReturnType(String.class.getName()),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(methods().should().notHaveRawReturnType(equivalentTo(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(constructors().should().haveRawReturnType(String.class),
                        ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().haveRawReturnType(String.class.getName()),
                        ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().haveRawReturnType(equivalentTo(String.class)),
                        ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(constructors().should().notHaveRawReturnType(String.class),
                        Collections.emptySet()),
                $(constructors().should().notHaveRawReturnType(String.class.getName()),
                        Collections.emptySet()),
                $(constructors().should().notHaveRawReturnType(equivalentTo(String.class)),
                        Collections.emptySet())
        );
    }

    @Test
    @UseDataProvider("restricted_return_type_rules")
    public void return_type_predicates(ArchRule rule, Collection<String> expectedMembers) {
        EvaluationResult result = rule.evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualMembers = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualMembers).containsOnlyElementsOf(expectedMembers);
    }

    @DataProvider
    public static Object[][] restricted_throwable_type_rules() {
        return $$(
                $(codeUnits().should().declareThrowableOfType(FirstException.class),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(codeUnits().should().declareThrowableOfType(FirstException.class.getName()),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(codeUnits().should().declareThrowableOfType(equivalentTo(FirstException.class)),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(codeUnits().should().notDeclareThrowableOfType(FirstException.class),
                        allCodeUnitsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(codeUnits().should().notDeclareThrowableOfType(FirstException.class.getName()),
                        allCodeUnitsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(codeUnits().should().notDeclareThrowableOfType(equivalentTo(FirstException.class)),
                        allCodeUnitsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),

                $(methods().should().declareThrowableOfType(FirstException.class),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(methods().should().declareThrowableOfType(FirstException.class.getName()),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(methods().should().declareThrowableOfType(equivalentTo(FirstException.class)),
                        ImmutableSet.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(methods().should().notDeclareThrowableOfType(FirstException.class),
                        allMethodsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(methods().should().notDeclareThrowableOfType(FirstException.class.getName()),
                        allMethodsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(methods().should().notDeclareThrowableOfType(equivalentTo(FirstException.class)),
                        allMethodsExcept(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),

                $(constructors().should().declareThrowableOfType(FirstException.class),
                        ImmutableSet.of(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(constructors().should().declareThrowableOfType(FirstException.class.getName()),
                        ImmutableSet.of(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(constructors().should().declareThrowableOfType(equivalentTo(FirstException.class)),
                        ImmutableSet.of(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(constructors().should().notDeclareThrowableOfType(FirstException.class),
                        allConstructorsExcept(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(constructors().should().notDeclareThrowableOfType(FirstException.class.getName()),
                        allConstructorsExcept(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS)),
                $(constructors().should().notDeclareThrowableOfType(equivalentTo(FirstException.class)),
                        allConstructorsExcept(CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_FOUR_ARGS))
        );
    }

    @Test
    @UseDataProvider("restricted_throwable_type_rules")
    public void throwable_type_predicates(ArchRule rule, Collection<String> expectedMembers) {
        EvaluationResult result = rule.evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualMembers = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualMembers).containsOnlyElementsOf(expectedMembers);
    }

    private static DescribedPredicate<JavaCodeUnit> doNotHaveParametersOfType(final Class<?> type) {
        return new DescribedPredicate<JavaCodeUnit>("do not have parameters of type " + type.getSimpleName()) {
            @Override
            public boolean apply(JavaCodeUnit codeUnit) {
                return !codeUnit.getRawParameterTypes().getNames().contains(type.getName());
            }
        };
    }

    private static Set<String> allMethodsExcept(String... methods) {
        return Sets.difference(ALL_METHOD_DESCRIPTIONS, ImmutableSet.copyOf(methods));
    }

    private static Set<String> allConstructorsExcept(String... constructors) {
        return Sets.difference(ALL_CONSTRUCTOR_DESCRIPTIONS, ImmutableSet.copyOf(constructors));
    }

    private static Set<String> allCodeUnitsExcept(String... codeUnits) {
        return union(allMethodsExcept(codeUnits), allConstructorsExcept(codeUnits));
    }
}