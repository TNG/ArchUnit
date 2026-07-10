package com.tngtech.archunit.lang.syntax.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testclasses.ClassWithPublicAndPrivateConstructor;
import com.tngtech.archunit.lang.syntax.elements.testclasses.SomeClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.conditions.ArchConditions.haveOnlyPrivateConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClass;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.theClass;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.FAILURE_REPORT_NEWLINE_MARKER;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.accessTargetIs;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.accessesFieldRegex;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.containsPartOfRegex;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.locationPattern;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldTest.singleLineFailureReportOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static java.util.regex.Pattern.quote;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class GivenClassShouldTest {

    private static final String ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX = "[^" + FAILURE_REPORT_NEWLINE_MARKER + "]*";

    static Stream<Arguments> theClass_should_haveFullyQualifiedName_rules() {
        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveFullyQualifiedName(SomeClass.class.getName()),
                        theClass(SomeClass.class).should().notHaveFullyQualifiedName(SomeClass.class.getName())),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())),
                        theClass(SomeClass.class).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName()))),
                arguments(theClass(SomeClass.class.getName()).should().haveFullyQualifiedName(SomeClass.class.getName()),
                        theClass(SomeClass.class.getName()).should().notHaveFullyQualifiedName(SomeClass.class.getName())),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveFullyQualifiedName_rules")
    void theClass_should_haveFullyQualifiedName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have fully qualified name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getName())
                .haveFailingRuleText("the class %s should not have fully qualified name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getName())
                .containFailureDetail(
                        String.format("Class <%s> has fully qualified name '%s' in %s",
                                quote(SomeClass.class.getName()),
                                quote(SomeClass.class.getName()),
                                locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveFullyQualifiedName_rules() {
        return Stream.of(
                arguments(noClass(SomeClass.class).should().notHaveFullyQualifiedName(SomeClass.class.getName()),
                        noClass(SomeClass.class).should().haveFullyQualifiedName(SomeClass.class.getName())),
                arguments(noClass(SomeClass.class).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())),
                        noClass(SomeClass.class).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName()))),
                arguments(noClass(SomeClass.class.getName()).should().notHaveFullyQualifiedName(SomeClass.class.getName()),
                        noClass(SomeClass.class.getName()).should().haveFullyQualifiedName(SomeClass.class.getName())),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveFullyQualifiedName_rules")
    void noClass_should_haveFullyQualifiedName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not have fully qualified name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getName())
                .haveFailingRuleText("no class %s should have fully qualified name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has fully qualified name '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(SomeClass.class.getName()),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_haveSimpleName_rules() {
        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveSimpleName(SomeClass.class.getSimpleName()),
                        theClass(SomeClass.class).should().notHaveSimpleName(SomeClass.class.getSimpleName())),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())),
                        theClass(SomeClass.class).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName()))),
                arguments(theClass(SomeClass.class.getName()).should().haveSimpleName(SomeClass.class.getSimpleName()),
                        theClass(SomeClass.class.getName()).should().notHaveSimpleName(SomeClass.class.getSimpleName())),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveSimpleName_rules")
    void haveSimpleName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getSimpleName())
                .haveFailingRuleText("the class %s should not have simple name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getSimpleName())
                .containFailureDetail(String.format("Class <%s> has simple name '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(SomeClass.class.getSimpleName()),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getSimpleName()));
    }

    static Stream<Arguments> noClass_should_haveSimpleName_rules() {
        return Stream.of(
                arguments(noClass(SomeClass.class).should().notHaveSimpleName(SomeClass.class.getSimpleName()),
                        noClass(SomeClass.class).should().haveSimpleName(SomeClass.class.getSimpleName())),
                arguments(noClass(SomeClass.class).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName()))),
                arguments(noClass(SomeClass.class.getName()).should().notHaveSimpleName(SomeClass.class.getSimpleName()),
                        noClass(SomeClass.class.getName()).should().haveSimpleName(SomeClass.class.getSimpleName())),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveSimpleName_rules")
    void noClass_should_haveSimpleName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not have simple name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getSimpleName())
                .haveFailingRuleText("no class %s should have simple name '%s'",
                        SomeClass.class.getName(), SomeClass.class.getSimpleName())
                .containFailureDetail(String.format("Class <%s> has simple name '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(SomeClass.class.getSimpleName()),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getSimpleName()));
    }

    static Stream<Arguments> theClass_should_haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveNameMatching(regex),
                        theClass(SomeClass.class).should().haveNameNotMatching(regex)),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveNameMatching(regex)),
                        theClass(SomeClass.class).should(ArchConditions.haveNameNotMatching(regex))),
                arguments(theClass(SomeClass.class.getName()).should().haveNameMatching(regex),
                        theClass(SomeClass.class.getName()).should().haveNameNotMatching(regex)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveNameMatching(regex)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveNameNotMatching(regex)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveNameMatching_rules")
    void theClass_should_haveNameMatching(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have name matching '%s'",
                        SomeClass.class.getName(), regex)
                .haveFailingRuleText("the class %s should have name not matching '%s'",
                        SomeClass.class.getName(), regex)
                .containFailureDetail(String.format("Class <%s> has name matching '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(regex),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(regex));
    }

    static Stream<Arguments> noClass_should_haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return Stream.of(
                arguments(noClass(SomeClass.class).should().haveNameNotMatching(regex),
                        noClass(SomeClass.class).should().haveNameMatching(regex)),
                arguments(noClass(SomeClass.class).should(ArchConditions.haveNameNotMatching(regex)),
                        noClass(SomeClass.class).should(ArchConditions.haveNameMatching(regex))),
                arguments(noClass(SomeClass.class.getName()).should().haveNameNotMatching(regex),
                        noClass(SomeClass.class.getName()).should().haveNameMatching(regex)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.haveNameNotMatching(regex)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveNameMatching(regex)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveNameMatching_rules")
    void noClass_should_haveNameMatching(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have name not matching '%s'",
                        SomeClass.class.getName(), regex)
                .haveFailingRuleText("no class %s should have name matching '%s'",
                        SomeClass.class.getName(), regex)
                .containFailureDetail(String.format("Class <%s> has name matching '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(regex),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(regex));
    }

    static Stream<Arguments> theClass_should_haveSimpleNameStartingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveSimpleNameStartingWith(prefix),
                        theClass(SomeClass.class).should().haveSimpleNameNotStartingWith(prefix)),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameStartingWith(prefix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotStartingWith(prefix))),
                arguments(theClass(SomeClass.class.getName()).should().haveSimpleNameStartingWith(prefix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotStartingWith(prefix)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameStartingWith(prefix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveSimpleNameStartingWith_rules")
    void theClass_should_haveSimpleNameStartingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .haveFailingRuleText("the class %s should have simple name not starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .containFailureDetail(String.format("Class <%s> has simple name starting with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(prefix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveSimpleNameStartingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return Stream.of(
                arguments(noClass(SomeClass.class).should().haveSimpleNameNotStartingWith(prefix),
                        noClass(SomeClass.class).should().haveSimpleNameStartingWith(prefix)),
                arguments(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameStartingWith(prefix))),
                arguments(noClass(SomeClass.class.getName()).should().haveSimpleNameNotStartingWith(prefix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameStartingWith(prefix)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameStartingWith(prefix)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveSimpleNameStartingWith_rules")
    void noClass_should_haveSimpleNameStartingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .haveFailingRuleText("no class %s should have simple name starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .containFailureDetail(String.format("Class <%s> has simple name starting with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(prefix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_haveSimpleNameContaining_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveSimpleNameContaining(infix),
                        theClass(SomeClass.class).should().haveSimpleNameNotContaining(infix)),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameContaining(infix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotContaining(infix))),
                arguments(theClass(SomeClass.class.getName()).should().haveSimpleNameContaining(infix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotContaining(infix)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameContaining(infix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotContaining(infix)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveSimpleNameContaining_rules")
    void theClass_should_haveSimpleNameContaining(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name containing '%s'",
                        SomeClass.class.getName(), infix)
                .haveFailingRuleText("the class %s should have simple name not containing '%s'",
                        SomeClass.class.getName(), infix)
                .containFailureDetail(String.format("Class <%s> has simple name containing '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(infix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveSimpleNameContaining_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return Stream.of(
                arguments(noClass(SomeClass.class).should().haveSimpleNameNotContaining(infix),
                        noClass(SomeClass.class).should().haveSimpleNameContaining(infix)),
                arguments(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotContaining(infix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameContaining(infix))),
                arguments(noClass(SomeClass.class.getName()).should().haveSimpleNameNotContaining(infix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameContaining(infix)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotContaining(infix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameContaining(infix)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveSimpleNameContaining_rules")
    void noClass_should_haveSimpleNameContaining(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not containing '%s'",
                        SomeClass.class.getName(), infix)
                .haveFailingRuleText("no class %s should have simple name containing '%s'",
                        SomeClass.class.getName(), infix)
                .containFailureDetail(String.format("Class <%s> has simple name containing '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(infix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_haveSimpleNameEndingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);

        return Stream.of(
                arguments(theClass(SomeClass.class).should().haveSimpleNameEndingWith(suffix),
                        theClass(SomeClass.class).should().haveSimpleNameNotEndingWith(suffix)),
                arguments(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameEndingWith(suffix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotEndingWith(suffix))),
                arguments(theClass(SomeClass.class.getName()).should().haveSimpleNameEndingWith(suffix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotEndingWith(suffix)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameEndingWith(suffix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveSimpleNameEndingWith_rules")
    void theClass_should_haveSimpleNameEndingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .haveFailingRuleText("the class %s should have simple name not ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .containFailureDetail(String.format("Class <%s> has simple name ending with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(suffix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveSimpleNameEndingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return Stream.of(
                arguments(noClass(SomeClass.class).should().haveSimpleNameNotEndingWith(suffix),
                        noClass(SomeClass.class).should().haveSimpleNameEndingWith(suffix)),
                arguments(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameEndingWith(suffix))),
                arguments(noClass(SomeClass.class.getName()).should().haveSimpleNameNotEndingWith(suffix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameEndingWith(suffix)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameEndingWith(suffix)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveSimpleNameEndingWith_rules")
    void noClass_should_haveSimpleNameEndingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .haveFailingRuleText("no class %s should have simple name ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .containFailureDetail(String.format("Class <%s> has simple name ending with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(suffix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_resideInAPackage_rules() {
        String thePackage = SomeClass.class.getPackage().getName();
        return Stream.of(
                arguments(theClass(SomeClass.class).should().resideInAPackage(thePackage),
                        theClass(SomeClass.class).should().resideOutsideOfPackage(thePackage)),
                arguments(theClass(SomeClass.class).should(ArchConditions.resideInAPackage(thePackage)),
                        theClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackage(thePackage))),
                arguments(theClass(SomeClass.class.getName()).should().resideInAPackage(thePackage),
                        theClass(SomeClass.class.getName()).should().resideOutsideOfPackage(thePackage)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.resideInAPackage(thePackage)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackage(thePackage)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_resideInAPackage_rules")
    void theClass_should_resideInAPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String thePackage = SomeClass.class.getPackage().getName();
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should reside in a package '%s'",
                        SomeClass.class.getName(), thePackage)
                .haveFailingRuleText("the class %s should reside outside of package '%s'",
                        SomeClass.class.getName(), thePackage)
                .containFailureDetail(String.format("Class <%s> does not reside outside of package '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(thePackage),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_resideInAPackage_rules() {
        String thePackage = SomeClass.class.getPackage().getName();
        return Stream.of(
                arguments(noClass(SomeClass.class).should().resideOutsideOfPackage(thePackage),
                        noClass(SomeClass.class).should().resideInAPackage(thePackage)),
                arguments(noClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackage(thePackage)),
                        noClass(SomeClass.class).should(ArchConditions.resideInAPackage(thePackage))),
                arguments(noClass(SomeClass.class.getName()).should().resideOutsideOfPackage(thePackage),
                        noClass(SomeClass.class.getName()).should().resideInAPackage(thePackage)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackage(thePackage)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.resideInAPackage(thePackage)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_resideInAPackage_rules")
    void noClass_should_resideInAPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String thePackage = SomeClass.class.getPackage().getName();

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should reside outside of package '%s'",
                        SomeClass.class.getName(), thePackage)
                .haveFailingRuleText("no class %s should reside in a package '%s'",
                        SomeClass.class.getName(), thePackage)
                .containFailureDetail(String.format("Class <%s> does reside in a package '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(thePackage),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_resideInAnyPackage_rules() {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        return Stream.of(
                arguments(theClass(SomeClass.class).should().resideInAnyPackage(firstPackage, secondPackage),
                        theClass(SomeClass.class).should().resideOutsideOfPackages(firstPackage, secondPackage)),
                arguments(theClass(SomeClass.class).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        theClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage))),
                arguments(theClass(SomeClass.class.getName()).should().resideInAnyPackage(firstPackage, secondPackage),
                        theClass(SomeClass.class.getName()).should().resideOutsideOfPackages(firstPackage, secondPackage)),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_resideInAnyPackage_rules")
    void theClass_should_resideInAnyPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        String[] packageIdentifiers = {firstPackage, secondPackage};

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should reside in any package [%s]",
                        SomeClass.class.getName(),
                        joinSingleQuoted(packageIdentifiers))
                .haveFailingRuleText("the class %s should reside outside of packages [%s]",
                        SomeClass.class.getName(),
                        joinSingleQuoted(packageIdentifiers))
                .containFailureDetail(String.format("Class <%s> does not reside outside of packages \\[%s\\] in %s",
                        quote(SomeClass.class.getName()),
                        quote(joinSingleQuoted(packageIdentifiers)),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_resideInAnyPackage_rules() {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();

        return Stream.of(
                arguments(noClass(SomeClass.class).should().resideOutsideOfPackages(firstPackage, secondPackage),
                        noClass(SomeClass.class).should().resideInAnyPackage(firstPackage, secondPackage)),
                arguments(noClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        noClass(SomeClass.class).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage))),
                arguments(noClass(SomeClass.class.getName()).should().resideOutsideOfPackages(firstPackage, secondPackage),
                        noClass(SomeClass.class.getName()).should().resideInAnyPackage(firstPackage, secondPackage)),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_resideInAnyPackage_rules")
    void noClass_should_resideInAnyPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        String[] packageIdentifiers = {firstPackage, secondPackage};

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should reside outside of packages [%s]",
                        SomeClass.class.getName(),
                        joinSingleQuoted(packageIdentifiers))
                .haveFailingRuleText("no class %s should reside in any package [%s]",
                        SomeClass.class.getName(),
                        joinSingleQuoted(packageIdentifiers))
                .containFailureDetail(String.format("Class <%s> does reside in any package \\[%s\\] in %s",
                        quote(SomeClass.class.getName()),
                        quote(joinSingleQuoted(packageIdentifiers)),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_bePublic_rules() {
        return Stream.of(
                arguments(theClass(SomeClass.class).should().bePublic(),
                        theClass(SomeClass.class).should().notBePublic()),
                arguments(theClass(SomeClass.class).should(ArchConditions.bePublic()),
                        theClass(SomeClass.class).should(ArchConditions.notBePublic())),
                arguments(theClass(SomeClass.class.getName()).should().bePublic(),
                        theClass(SomeClass.class.getName()).should().notBePublic()),
                arguments(theClass(SomeClass.class.getName()).should(ArchConditions.bePublic()),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notBePublic()))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_bePublic_rules")
    void theClass_should_bePublic(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should be public",
                        SomeClass.class.getName())
                .haveFailingRuleText("the class %s should not be public",
                        SomeClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(SomeClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_bePublic_rules() {
        return Stream.of(
                arguments(noClass(SomeClass.class).should().notBePublic(),
                        noClass(SomeClass.class).should().bePublic()),
                arguments(noClass(SomeClass.class).should(ArchConditions.notBePublic()),
                        noClass(SomeClass.class).should(ArchConditions.bePublic())),
                arguments(noClass(SomeClass.class.getName()).should().notBePublic(),
                        noClass(SomeClass.class.getName()).should().bePublic()),
                arguments(noClass(SomeClass.class.getName()).should(ArchConditions.notBePublic()),
                        noClass(SomeClass.class.getName()).should(ArchConditions.bePublic()))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_bePublic_rules")
    void noClass_should_bePublic(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not be public",
                        SomeClass.class.getName())
                .haveFailingRuleText("no class %s should be public",
                        SomeClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(SomeClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_bePrivate_rules() {
        return Stream.of(
                arguments(theClass(PrivateClass.class).should().bePrivate(),
                        theClass(PrivateClass.class).should().notBePrivate()),
                arguments(theClass(PrivateClass.class).should(ArchConditions.bePrivate()),
                        theClass(PrivateClass.class).should(ArchConditions.notBePrivate())),
                arguments(theClass(PrivateClass.class.getName()).should().bePrivate(),
                        theClass(PrivateClass.class.getName()).should().notBePrivate()),
                arguments(theClass(PrivateClass.class.getName()).should(ArchConditions.bePrivate()),
                        theClass(PrivateClass.class.getName()).should(ArchConditions.notBePrivate()))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_bePrivate_rules")
    void theClass_should_bePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PrivateClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should be private",
                        PrivateClass.class.getName())
                .haveFailingRuleText("the class %s should not be private",
                        PrivateClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(PrivateClass.class.getName()),
                        quote(JavaModifier.PRIVATE.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_bePrivate_rules() {
        return Stream.of(
                arguments(noClass(PrivateClass.class).should().notBePrivate(),
                        noClass(PrivateClass.class).should().bePrivate()),
                arguments(noClass(PrivateClass.class).should(ArchConditions.notBePrivate()),
                        noClass(PrivateClass.class).should(ArchConditions.bePrivate())),
                arguments(noClass(PrivateClass.class.getName()).should().notBePrivate(),
                        noClass(PrivateClass.class.getName()).should().bePrivate()),
                arguments(noClass(PrivateClass.class.getName()).should(ArchConditions.notBePrivate()),
                        noClass(PrivateClass.class.getName()).should(ArchConditions.bePrivate()))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_bePrivate_rules")
    void noClass_should_bePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PrivateClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not be private",
                        PrivateClass.class.getName())
                .haveFailingRuleText("no class %s should be private",
                        PrivateClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(PrivateClass.class.getName()),
                        quote(JavaModifier.PRIVATE.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_haveOnlyFinalFields_rules() {
        return Stream.of(
                arguments(theClass(ClassWithFinalFields.class).should().haveOnlyFinalFields(),
                        theClass(ClassWithNonFinalFields.class).should().haveOnlyFinalFields()),
                arguments(theClass(ClassWithFinalFields.class).should(ArchConditions.haveOnlyFinalFields()),
                        theClass(ClassWithNonFinalFields.class).should(ArchConditions.haveOnlyFinalFields())),
                arguments(theClass(ClassWithFinalFields.class.getName()).should().haveOnlyFinalFields(),
                        theClass(ClassWithNonFinalFields.class.getName()).should().haveOnlyFinalFields()),
                arguments(theClass(ClassWithFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()),
                        theClass(ClassWithNonFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveOnlyFinalFields_rules")
    void theClass_should_haveOnlyFinalFields(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithFinalFields.class, ClassWithNonFinalFields.class)
                .haveSuccessfulRuleText("the class %s should have only final fields",
                        ClassWithFinalFields.class.getName())
                .haveFailingRuleText("the class %s should have only final fields",
                        ClassWithNonFinalFields.class.getName())
                .containFailureDetail(String.format("Field <%s.integerField> is not final in %s",
                        quote(ClassWithNonFinalFields.class.getName()),
                        locationPattern(GivenClassShouldTest.class)))
                .containFailureDetail(String.format("Field <%s.stringField> is not final in %s",
                        quote(ClassWithNonFinalFields.class.getName()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(ClassWithFinalFields.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveOnlyFinalFields_rules() {
        return Stream.of(
                arguments(noClass(ClassWithNonFinalFields.class).should().haveOnlyFinalFields(),
                        noClass(ClassWithFinalFields.class).should().haveOnlyFinalFields()),
                arguments(noClass(ClassWithNonFinalFields.class).should(ArchConditions.haveOnlyFinalFields()),
                        noClass(ClassWithFinalFields.class).should(ArchConditions.haveOnlyFinalFields())),
                arguments(noClass(ClassWithNonFinalFields.class.getName()).should().haveOnlyFinalFields(),
                        noClass(ClassWithFinalFields.class.getName()).should().haveOnlyFinalFields()),
                arguments(noClass(ClassWithNonFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()),
                        noClass(ClassWithFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveOnlyFinalFields_rules")
    void noClass_should_haveOnlyFinalFields(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithFinalFields.class, ClassWithNonFinalFields.class)
                .haveSuccessfulRuleText("no class %s should have only final fields",
                        ClassWithNonFinalFields.class.getName())
                .haveFailingRuleText("no class %s should have only final fields",
                        ClassWithFinalFields.class.getName())
                .containFailureDetail(String.format("Field <%s.stringField> is final in %s",
                        quote(ClassWithFinalFields.class.getName()),
                        locationPattern(getClass())))
                .doNotContainFailureDetail(quote(ClassWithNonFinalFields.class.getName()));
    }

    static Stream<ArchRule> classes_should_have_only_private_constructor_rules() {
        return Stream.of(
                classes().should().haveOnlyPrivateConstructors(),
                classes().should(haveOnlyPrivateConstructors()));
    }

    @ParameterizedTest
    @MethodSource("classes_should_have_only_private_constructor_rules")
    void classes_should_have_only_private_constructor(ArchRule rule) {
        assertThatRule(rule).hasDescriptionContaining("classes should have only private constructors");
        assertThatRule(rule).checking(importClasses(ClassWithPrivateConstructors.class))
                .hasNoViolation();
        assertThatRule(rule).checking(importClasses(ClassWithPublicAndPrivateConstructor.class))
                .hasOnlyViolations(String.format("Constructor <%s.<init>(%s)> is not private in (%s.java:%d)",
                        ClassWithPublicAndPrivateConstructor.class.getName(), String.class.getName(),
                        ClassWithPublicAndPrivateConstructor.class.getSimpleName(), 5));
    }

    static Stream<Arguments> theClass_should_beProtected_rules() {
        return Stream.of(
                arguments(theClass(ProtectedClass.class).should().beProtected(),
                        theClass(ProtectedClass.class).should().notBeProtected()),
                arguments(theClass(ProtectedClass.class).should(ArchConditions.beProtected()),
                        theClass(ProtectedClass.class).should(ArchConditions.notBeProtected())),
                arguments(theClass(ProtectedClass.class.getName()).should().beProtected(),
                        theClass(ProtectedClass.class.getName()).should().notBeProtected()),
                arguments(theClass(ProtectedClass.class.getName()).should(ArchConditions.beProtected()),
                        theClass(ProtectedClass.class.getName()).should(ArchConditions.notBeProtected()))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_beProtected_rules")
    void theClass_should_beProtected(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ProtectedClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should be protected",
                        ProtectedClass.class.getName())
                .haveFailingRuleText("the class %s should not be protected",
                        ProtectedClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(ProtectedClass.class.getName()),
                        quote(JavaModifier.PROTECTED.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_beProtected_rules() {
        return Stream.of(
                arguments(noClass(ProtectedClass.class).should().notBeProtected(),
                        noClass(ProtectedClass.class).should().beProtected()),
                arguments(noClass(ProtectedClass.class).should(ArchConditions.notBeProtected()),
                        noClass(ProtectedClass.class).should(ArchConditions.beProtected())),
                arguments(noClass(ProtectedClass.class.getName()).should().notBeProtected(),
                        noClass(ProtectedClass.class.getName()).should().beProtected()),
                arguments(noClass(ProtectedClass.class.getName()).should(ArchConditions.notBeProtected()),
                        noClass(ProtectedClass.class.getName()).should(ArchConditions.beProtected()))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_beProtected_rules")
    void noClass_should_beProtected(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ProtectedClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not be protected",
                        ProtectedClass.class.getName())
                .haveFailingRuleText("no class %s should be protected",
                        ProtectedClass.class.getName())
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(ProtectedClass.class.getName()),
                        quote(JavaModifier.PROTECTED.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_bePackagePrivate_rules() {
        return Stream.of(
                arguments(theClass(PackagePrivateClass.class).should().bePackagePrivate(),
                        theClass(PackagePrivateClass.class).should().notBePackagePrivate()),
                arguments(theClass(PackagePrivateClass.class).should(ArchConditions.bePackagePrivate()),
                        theClass(PackagePrivateClass.class).should(ArchConditions.notBePackagePrivate())),
                arguments(theClass(PackagePrivateClass.class.getName()).should().bePackagePrivate(),
                        theClass(PackagePrivateClass.class.getName()).should().notBePackagePrivate()),
                arguments(theClass(PackagePrivateClass.class.getName()).should(ArchConditions.bePackagePrivate()),
                        theClass(PackagePrivateClass.class.getName()).should(ArchConditions.notBePackagePrivate()))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_bePackagePrivate_rules")
    void theClass_should_bePackagePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PackagePrivateClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should be package private",
                        PackagePrivateClass.class.getName())
                .haveFailingRuleText("the class %s should not be package private",
                        PackagePrivateClass.class.getName())
                .containFailureDetail(String.format("Class <%s> does not have modifier %s in %s "
                                + "and Class <%s> does not have modifier %s in %s "
                                + "and Class <%s> does not have modifier %s in %s",
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PRIVATE.name()),
                        locationPattern(GivenClassShouldTest.class),
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PROTECTED.name()),
                        locationPattern(GivenClassShouldTest.class),
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_bePackagePrivate_rules() {
        return Stream.of(
                arguments(noClass(PackagePrivateClass.class).should().notBePackagePrivate(),
                        noClass(PackagePrivateClass.class).should().bePackagePrivate()),
                arguments(noClass(PackagePrivateClass.class).should(ArchConditions.notBePackagePrivate()),
                        noClass(PackagePrivateClass.class).should(ArchConditions.bePackagePrivate())),
                arguments(noClass(PackagePrivateClass.class.getName()).should().notBePackagePrivate(),
                        noClass(PackagePrivateClass.class.getName()).should().bePackagePrivate()),
                arguments(noClass(PackagePrivateClass.class.getName()).should(ArchConditions.notBePackagePrivate()),
                        noClass(PackagePrivateClass.class.getName()).should(ArchConditions.bePackagePrivate()))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_bePackagePrivate_rules")
    void noClass_should_bePackagePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PackagePrivateClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not be package private",
                        PackagePrivateClass.class.getName())
                .haveFailingRuleText("no class %s should be package private",
                        PackagePrivateClass.class.getName())
                .containFailureDetail(String.format("Class <%s> does not have modifier %s in %s "
                                + "and Class <%s> does not have modifier %s in %s "
                                + "and Class <%s> does not have modifier %s in %s",
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PRIVATE.name()),
                        locationPattern(GivenClassShouldTest.class),
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PROTECTED.name()),
                        locationPattern(GivenClassShouldTest.class),
                        quote(PackagePrivateClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_haveModifier_public_rules() {
        return Stream.of(
                arguments(theClass(PublicClass.class).should().haveModifier(JavaModifier.PUBLIC),
                        theClass(PublicClass.class).should().notHaveModifier(JavaModifier.PUBLIC)),
                arguments(theClass(PublicClass.class).should(ArchConditions.haveModifier(JavaModifier.PUBLIC)),
                        theClass(PublicClass.class).should(ArchConditions.notHaveModifier(JavaModifier.PUBLIC))),
                arguments(theClass(PublicClass.class.getName()).should().haveModifier(JavaModifier.PUBLIC),
                        theClass(PublicClass.class.getName()).should().notHaveModifier(JavaModifier.PUBLIC)),
                arguments(theClass(PublicClass.class.getName()).should(ArchConditions.haveModifier(JavaModifier.PUBLIC)),
                        theClass(PublicClass.class.getName()).should(ArchConditions.notHaveModifier(JavaModifier.PUBLIC)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_haveModifier_public_rules")
    void theClass_should_haveModifier_public(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PublicClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have modifier %s",
                        PublicClass.class.getName(), PUBLIC)
                .haveFailingRuleText("the class %s should not have modifier %s",
                        PublicClass.class.getName(), PUBLIC)
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(PublicClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_haveModifier_public_rules() {
        return Stream.of(
                arguments(noClass(PublicClass.class).should().notHaveModifier(JavaModifier.PUBLIC),
                        noClass(PublicClass.class).should().haveModifier(JavaModifier.PUBLIC)),
                arguments(noClass(PublicClass.class).should(ArchConditions.notHaveModifier(JavaModifier.PUBLIC)),
                        noClass(PublicClass.class).should(ArchConditions.haveModifier(JavaModifier.PUBLIC))),
                arguments(noClass(PublicClass.class.getName()).should().notHaveModifier(JavaModifier.PUBLIC),
                        noClass(PublicClass.class.getName()).should().haveModifier(JavaModifier.PUBLIC)),
                arguments(noClass(PublicClass.class.getName()).should(ArchConditions.notHaveModifier(JavaModifier.PUBLIC)),
                        noClass(PublicClass.class.getName()).should(ArchConditions.haveModifier(JavaModifier.PUBLIC)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_haveModifier_public_rules")
    void noClass_should_haveModifier_public(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, PublicClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not have modifier %s",
                        PublicClass.class.getName(), PUBLIC)
                .haveFailingRuleText("no class %s should have modifier %s",
                        PublicClass.class.getName(), PUBLIC)
                .containFailureDetail(String.format("Class <%s> has modifier %s in %s",
                        quote(PublicClass.class.getName()),
                        quote(JavaModifier.PUBLIC.name()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_beAnnotatedWith_rules() {
        return Stream.of(
                arguments(theClass(SomeAnnotatedClass.class).should().beAnnotatedWith(RuntimeRetentionAnnotation.class),
                        theClass(SomeAnnotatedClass.class).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                arguments(theClass(SomeAnnotatedClass.class).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        theClass(SomeAnnotatedClass.class).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class))),
                arguments(theClass(SomeAnnotatedClass.class.getName()).should().beAnnotatedWith(RuntimeRetentionAnnotation.class),
                        theClass(SomeAnnotatedClass.class.getName()).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                arguments(theClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        theClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_beAnnotatedWith_rules")
    void theClass_should_beAnnotatedWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeAnnotatedClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should be annotated with @%s",
                        SomeAnnotatedClass.class.getName(), RuntimeRetentionAnnotation.class.getSimpleName())
                .haveFailingRuleText("the class %s should not be annotated with @%s",
                        SomeAnnotatedClass.class.getName(), RuntimeRetentionAnnotation.class.getSimpleName())
                .containFailureDetail(String.format("Class <%s> is annotated with @%s in %s",
                        quote(SomeAnnotatedClass.class.getName()),
                        quote(RuntimeRetentionAnnotation.class.getSimpleName()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_beAnnotatedWith_rules() {
        return Stream.of(
                arguments(noClass(SomeAnnotatedClass.class).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class),
                        noClass(SomeAnnotatedClass.class).should().beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                arguments(noClass(SomeAnnotatedClass.class).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        noClass(SomeAnnotatedClass.class).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class))),
                arguments(noClass(SomeAnnotatedClass.class.getName()).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class),
                        noClass(SomeAnnotatedClass.class.getName()).should().beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                arguments(noClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        noClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_beAnnotatedWith_rules")
    void noClass_should_beAnnotatedWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeAnnotatedClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not be annotated with @%s",
                        SomeAnnotatedClass.class.getName(), RuntimeRetentionAnnotation.class.getSimpleName())
                .haveFailingRuleText("no class %s should be annotated with @%s",
                        SomeAnnotatedClass.class.getName(), RuntimeRetentionAnnotation.class.getSimpleName())
                .containFailureDetail(String.format("Class <%s> is annotated with @%s in %s",
                        quote(SomeAnnotatedClass.class.getName()),
                        quote(RuntimeRetentionAnnotation.class.getSimpleName()),
                        locationPattern(GivenClassShouldTest.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_implement_rules() {
        return Stream.of(
                arguments(theClass(ArrayList.class).should().implement(Collection.class),
                        theClass(ArrayList.class).should().notImplement(Collection.class)),
                arguments(theClass(ArrayList.class).should(ArchConditions.implement(Collection.class)),
                        theClass(ArrayList.class).should(ArchConditions.notImplement(Collection.class))),
                arguments(theClass(ArrayList.class.getName()).should().implement(Collection.class),
                        theClass(ArrayList.class.getName()).should().notImplement(Collection.class)),
                arguments(theClass(ArrayList.class.getName()).should(ArchConditions.implement(Collection.class)),
                        theClass(ArrayList.class.getName()).should(ArchConditions.notImplement(Collection.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_implement_rules")
    void theClass_should_implement(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ArrayList.class, Object.class)
                .haveSuccessfulRuleText("the class %s should implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .haveFailingRuleText("the class %s should not implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> does implement %s in %s",
                        quote(ArrayList.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(ArrayList.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_implement_rules() {
        return Stream.of(
                arguments(noClass(ArrayList.class).should().notImplement(Collection.class),
                        noClass(ArrayList.class).should().implement(Collection.class)),
                arguments(noClass(ArrayList.class).should(ArchConditions.notImplement(Collection.class)),
                        noClass(ArrayList.class).should(ArchConditions.implement(Collection.class))),
                arguments(noClass(ArrayList.class.getName()).should().notImplement(Collection.class),
                        noClass(ArrayList.class.getName()).should().implement(Collection.class)),
                arguments(noClass(ArrayList.class.getName()).should(ArchConditions.notImplement(Collection.class)),
                        noClass(ArrayList.class.getName()).should(ArchConditions.implement(Collection.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_implement_rules")
    void noClass_should_implement(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ArrayList.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .haveFailingRuleText("no class %s should implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> does implement %s in %s",
                        quote(ArrayList.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(ArrayList.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_beAssignableTo_rules() {
        return Stream.of(
                arguments(theClass(List.class).should().beAssignableTo(Collection.class),
                        theClass(List.class).should().notBeAssignableTo(Collection.class)),
                arguments(theClass(List.class).should(ArchConditions.beAssignableTo(Collection.class)),
                        theClass(List.class).should(ArchConditions.notBeAssignableTo(Collection.class))),
                arguments(theClass(List.class.getName()).should().beAssignableTo(Collection.class),
                        theClass(List.class.getName()).should().notBeAssignableTo(Collection.class)),
                arguments(theClass(List.class.getName()).should(ArchConditions.beAssignableTo(Collection.class)),
                        theClass(List.class.getName()).should(ArchConditions.notBeAssignableTo(Collection.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_beAssignableTo_rules")
    void theClass_should_beAssignableTo(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, List.class, Collection.class)
                .haveSuccessfulRuleText("the class %s should be assignable to %s",
                        List.class.getName(), Collection.class.getName())
                .haveFailingRuleText("the class %s should not be assignable to %s",
                        List.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> is assignable to %s in %s",
                        quote(List.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(List.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_beAssignableTo_rules() {
        return Stream.of(
                arguments(noClass(List.class).should().notBeAssignableTo(Collection.class),
                        noClass(List.class).should().beAssignableTo(Collection.class)),
                arguments(noClass(List.class).should(ArchConditions.notBeAssignableTo(Collection.class)),
                        noClass(List.class).should(ArchConditions.beAssignableTo(Collection.class))),
                arguments(noClass(List.class.getName()).should().notBeAssignableTo(Collection.class),
                        noClass(List.class.getName()).should().beAssignableTo(Collection.class)),
                arguments(noClass(List.class.getName()).should(ArchConditions.notBeAssignableTo(Collection.class)),
                        noClass(List.class.getName()).should(ArchConditions.beAssignableTo(Collection.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_beAssignableTo_rules")
    void noClass_should_beAssignableTo(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, List.class, Collection.class)
                .haveSuccessfulRuleText("no class %s should not be assignable to %s",
                        List.class.getName(), Collection.class.getName())
                .haveFailingRuleText("no class %s should be assignable to %s",
                        List.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> is assignable to %s in %s",
                        quote(List.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(List.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_beAssignableFrom_rules() {
        return Stream.of(
                arguments(theClass(Collection.class).should().beAssignableFrom(List.class),
                        theClass(Collection.class).should().notBeAssignableFrom(List.class)),
                arguments(theClass(Collection.class).should(ArchConditions.beAssignableFrom(List.class)),
                        theClass(Collection.class).should(ArchConditions.notBeAssignableFrom(List.class))),
                arguments(theClass(Collection.class.getName()).should().beAssignableFrom(List.class),
                        theClass(Collection.class.getName()).should().notBeAssignableFrom(List.class)),
                arguments(theClass(Collection.class.getName()).should(ArchConditions.beAssignableFrom(List.class)),
                        theClass(Collection.class.getName()).should(ArchConditions.notBeAssignableFrom(List.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_beAssignableFrom_rules")
    void theClass_should_beAssignableFrom(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, Collection.class, List.class)
                .haveSuccessfulRuleText("the class %s should be assignable from %s",
                        Collection.class.getName(), List.class.getName())
                .haveFailingRuleText("the class %s should not be assignable from %s",
                        Collection.class.getName(), List.class.getName())
                .containFailureDetail(String.format("Class <%s> is assignable from %s in %s",
                        quote(Collection.class.getName()),
                        quote(List.class.getName()),
                        locationPattern(Collection.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> noClass_should_beAssignableFrom_rules() {
        return Stream.of(
                arguments(noClass(Collection.class).should().notBeAssignableFrom(List.class),
                        noClass(Collection.class).should().beAssignableFrom(List.class)),
                arguments(noClass(Collection.class).should(ArchConditions.notBeAssignableFrom(List.class)),
                        noClass(Collection.class).should(ArchConditions.beAssignableFrom(List.class))),
                arguments(noClass(Collection.class.getName()).should().notBeAssignableFrom(List.class),
                        noClass(Collection.class.getName()).should().beAssignableFrom(List.class)),
                arguments(noClass(Collection.class.getName()).should(ArchConditions.notBeAssignableFrom(List.class)),
                        noClass(Collection.class.getName()).should(ArchConditions.beAssignableFrom(List.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_beAssignableFrom_rules")
    void noClass_should_beAssignableFrom(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, Collection.class, List.class)
                .haveSuccessfulRuleText("no class %s should not be assignable from %s",
                        Collection.class.getName(), List.class.getName())
                .haveFailingRuleText("no class %s should be assignable from %s",
                        Collection.class.getName(), List.class.getName())
                .containFailureDetail(String.format("Class <%s> is assignable from %s in %s",
                        quote(Collection.class.getName()),
                        quote(List.class.getName()),
                        locationPattern(Collection.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    static Stream<Arguments> theClass_should_getField_rules() {
        return Stream.of(
                arguments(theClass(ClassAccessingField.class).should().getField(ClassWithField.class, "field"), theClass(ClassAccessingWrongField.class).should().getField(ClassWithField.class,
                        "field")),
                arguments(theClass(ClassAccessingField.class).should(ArchConditions.getField(ClassWithField.class, "field")), theClass(ClassAccessingWrongField.class).should(ArchConditions.getField(ClassWithField.class,
                        "field"))),
                arguments(theClass(ClassAccessingField.class.getName()).should().getField(ClassWithField.class, "field"), theClass(ClassAccessingWrongField.class.getName()).should().getField(ClassWithField.class,
                        "field")),
                arguments(theClass(ClassAccessingField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")), theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getField(ClassWithField.class,
                        "field")))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_getField_rules")
    void theClass_should_getField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("the class %s should get field %s.%s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .haveFailingRuleText("the class %s should get field %s.%s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingWrongField.class, "gets",
                        ClassAccessingWrongField.class, "classAccessingField"))
                .doNotContainFailureDetail(quote(ClassAccessingField.class.getName()) + ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX + "get");
    }

    static Stream<Arguments> noClass_should_getField_rules() {
        return Stream.of(
                arguments(noClass(ClassAccessingWrongField.class).should().getField(ClassWithField.class, "field"), noClass(ClassAccessingField.class).should().getField(ClassWithField.class, "field")),
                arguments(noClass(ClassAccessingWrongField.class).should(ArchConditions.getField(ClassWithField.class, "field")), noClass(ClassAccessingField.class).should(ArchConditions.getField(ClassWithField.class, "field"))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should().getField(ClassWithField.class, "field"), noClass(ClassAccessingField.class.getName()).should().getField(ClassWithField.class, "field")),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")), noClass(ClassAccessingField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_getField_rules")
    void noClass_should_getField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("no class %s should get field %s.%s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .haveFailingRuleText("no class %s should get field %s.%s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingField.class, "gets",
                        ClassWithField.class, "field"))
                .doNotContainFailureDetail(quote(ClassAccessingWrongField.class.getName()));
    }

    static Stream<Arguments> theClass_should_accessField_rules() {
        return Stream.of(
                arguments(theClass(ClassAccessingField.class).should().accessField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class).should().accessField(ClassWithField.class, "field")),
                arguments(theClass(ClassAccessingField.class).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.accessField(ClassWithField.class, "field"))),
                arguments(theClass(ClassAccessingField.class.getName()).should().accessField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class.getName()).should().accessField(ClassWithField.class, "field")),
                arguments(theClass(ClassAccessingField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_accessField_rules")
    void theClass_should_accessField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("the class %s should access field %s.%s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .haveFailingRuleText("the class %s should access field %s.%s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingWrongField.class, "gets",
                        ClassAccessingWrongField.class, "classAccessingField"))
                .doNotContainFailureDetail(quote(ClassAccessingField.class.getName()) + ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX + "accesses");
    }

    static Stream<Arguments> noClass_should_accessField_rules() {
        return Stream.of(
                arguments(noClass(ClassAccessingWrongField.class).should().accessField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class).should().accessField(ClassWithField.class, "field")),
                arguments(noClass(ClassAccessingWrongField.class).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class).should(ArchConditions.accessField(ClassWithField.class, "field"))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should().accessField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class.getName()).should().accessField(ClassWithField.class, "field")),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_accessField_rules")
    void noClass_should_accessField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("no class %s should access field %s.%s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .haveFailingRuleText("no class %s should access field %s.%s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName(), "field")
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingField.class, "gets",
                        ClassWithField.class, "field"))
                .doNotContainFailureDetail(quote(ClassAccessingWrongField.class.getName()));
    }

    static Stream<Arguments> theClass_should_getFieldWhere_rules() {
        return Stream.of(
                arguments(theClass(ClassAccessingField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(theClass(ClassAccessingField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class)))),
                arguments(theClass(ClassAccessingField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(theClass(ClassAccessingField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_getFieldWhere_rules")
    void theClass_should_getFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("the class %s should get field where target is %s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName())
                .haveFailingRuleText("the class %s should get field where target is %s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName())
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingWrongField.class, "gets",
                        ClassAccessingWrongField.class, "classAccessingField"))
                .doNotContainFailureDetail(quote(ClassAccessingField.class.getSimpleName()) + ".*accesses");
    }

    static Stream<Arguments> noClass_should_getFieldWhere_rules() {
        return Stream.of(
                arguments(noClass(ClassAccessingWrongField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(noClass(ClassAccessingWrongField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class)))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_getFieldWhere_rules")
    void noClass_should_getFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("no class %s should get field where target is %s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName())
                .haveFailingRuleText("no class %s should get field where target is %s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName())
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingField.class, "gets",
                        ClassWithField.class, "field"))
                .doNotContainFailureDetail(quote(ClassAccessingWrongField.class.getSimpleName()));
    }

    static Stream<Arguments> theClass_should_accessFieldWhere_rules() {
        return Stream.of(
                arguments(theClass(ClassAccessingField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(theClass(ClassAccessingField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class)))),
                arguments(theClass(ClassAccessingField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(theClass(ClassAccessingField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class.getName())
                                .should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("theClass_should_accessFieldWhere_rules")
    void theClass_should_accessFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("the class %s should access field where target is %s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName())
                .haveFailingRuleText("the class %s should access field where target is %s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName())
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingWrongField.class, "gets",
                        ClassAccessingWrongField.class, "classAccessingField"))
                .doNotContainFailureDetail(quote(ClassAccessingField.class.getSimpleName()) + ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX + "accesses");
    }

    static Stream<Arguments> noClass_should_accessFieldWhere_rules() {
        return Stream.of(
                arguments(noClass(ClassAccessingWrongField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(noClass(ClassAccessingWrongField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class)))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                arguments(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("noClass_should_accessFieldWhere_rules")
    void noClass_should_accessFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ClassWithField.class, ClassAccessingWrongField.class, ClassAccessingField.class)
                .haveSuccessfulRuleText("no class %s should access field where target is %s",
                        ClassAccessingWrongField.class.getName(), ClassWithField.class.getSimpleName())
                .haveFailingRuleText("no class %s should access field where target is %s",
                        ClassAccessingField.class.getName(), ClassWithField.class.getSimpleName())
                .containFailureDetail(accessesFieldRegex(
                        ClassAccessingField.class, "gets",
                        ClassWithField.class, "field"))
                .doNotContainFailureDetail(quote(ClassAccessingWrongField.class.getName()));
    }

    private RuleEvaluationAsserter assertThatRules(ArchRule satisfiedRule, ArchRule unsatisfiedRule, Class<?>... classesToImport) {
        return new RuleEvaluationAsserter(satisfiedRule, unsatisfiedRule, classesToImport);
    }

    private static class RuleEvaluationAsserter {
        private final ArchRule satisfiedRule;
        private final ArchRule unsatisfiedRule;
        private final EvaluationResult satisfiedResult;
        private final EvaluationResult unsatisfiedResult;

        RuleEvaluationAsserter(ArchRule satisfiedRule, ArchRule unsatisfiedRule, Class<?>... classesToImport) {
            this.satisfiedRule = satisfiedRule;
            this.unsatisfiedRule = unsatisfiedRule;
            JavaClasses classes = importClasses(classesToImport);
            satisfiedResult = satisfiedRule.evaluate(classes);
            unsatisfiedResult = unsatisfiedRule.evaluate(classes);
        }

        RuleEvaluationAsserter haveSuccessfulRuleText(String descriptionTemplate, Object... args) {
            assertThat(satisfiedRule.getDescription()).isEqualTo(String.format(descriptionTemplate, args));
            assertNoViolation(satisfiedResult);
            return this;
        }

        RuleEvaluationAsserter haveFailingRuleText(String descriptionTemplate, Object... args) {
            String description = String.format(descriptionTemplate, args);
            assertThat(unsatisfiedRule.getDescription()).isEqualTo(description);
            assertThat(singleLineFailureReportOf(unsatisfiedResult)).contains(description);
            return this;
        }

        RuleEvaluationAsserter containFailureDetail(String detailPattern) {
            return containFailureDetail(Pattern.compile(detailPattern));
        }

        RuleEvaluationAsserter containFailureDetail(Pattern detailPattern) {
            assertThat(singleLineFailureReportOf(unsatisfiedResult)).containsPattern(detailPattern);
            return this;
        }

        void doNotContainFailureDetail(String detailPattern) {
            String anyLineThatContainsThePattern = ".*" + FAILURE_REPORT_NEWLINE_MARKER
                    + ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX + detailPattern + ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX
                    + FAILURE_REPORT_NEWLINE_MARKER + ".*";
            assertThat(singleLineFailureReportOf(unsatisfiedResult)).doesNotMatch(anyLineThatContainsThePattern);
        }
    }

    private static void assertNoViolation(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result has violation").isFalse();
    }

    private static class ClassWithField {
        String field;
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingField {
        ClassWithField classWithField;

        String access() {
            classWithField.field = "new";
            return classWithField.field;
        }
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingWrongField {
        ClassAccessingField classAccessingField;

        @SuppressWarnings("ConstantConditions")
        ClassWithField wrongAccess() {
            classAccessingField.classWithField = null;
            return classAccessingField.classWithField;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class PublicClass {
    }

    @SuppressWarnings("WeakerAccess")
    protected static class ProtectedClass {
    }

    @SuppressWarnings("WeakerAccess")
    static class PackagePrivateClass {
    }

    private static class PrivateClass {
    }

    @SuppressWarnings("unused")
    private static class ClassWithNonFinalFields {
        String stringField;
        int integerField;
    }

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static class ClassWithFinalFields {
        private final String stringField;

        ClassWithFinalFields(String stringField) {
            this.stringField = stringField;
        }
    }

    @RuntimeRetentionAnnotation
    private static class SomeAnnotatedClass {
    }

    @SuppressWarnings("unused")
    private static class ClassWithPrivateConstructors {
        private ClassWithPrivateConstructors() {
        }

        private ClassWithPrivateConstructors(String foo) {
        }
    }
}
