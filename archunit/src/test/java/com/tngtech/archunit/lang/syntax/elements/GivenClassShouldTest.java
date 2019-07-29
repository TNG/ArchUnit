package com.tngtech.archunit.lang.syntax.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testclasses.SomeClass;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.regex.Pattern.quote;

@RunWith(DataProviderRunner.class)
public class GivenClassShouldTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private static final String ANY_NUMBER_OF_NON_NEWLINE_CHARS_REGEX = "[^" + FAILURE_REPORT_NEWLINE_MARKER + "]*";

    @DataProvider
    public static Object[][] theClass_should_haveFullyQualifiedName_rules() {
        return $$(
                $(theClass(SomeClass.class).should().haveFullyQualifiedName(SomeClass.class.getName()),
                        theClass(SomeClass.class).should().notHaveFullyQualifiedName(SomeClass.class.getName())),
                $(theClass(SomeClass.class).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())),
                        theClass(SomeClass.class).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName()))),
                $(theClass(SomeClass.class.getName()).should().haveFullyQualifiedName(SomeClass.class.getName()),
                        theClass(SomeClass.class.getName()).should().notHaveFullyQualifiedName(SomeClass.class.getName())),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveFullyQualifiedName_rules")
    public void theClass_should_haveFullyQualifiedName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_haveFullyQualifiedName_rules() {
        return $$(
                $(noClass(SomeClass.class).should().notHaveFullyQualifiedName(SomeClass.class.getName()),
                        noClass(SomeClass.class).should().haveFullyQualifiedName(SomeClass.class.getName())),
                $(noClass(SomeClass.class).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())),
                        noClass(SomeClass.class).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName()))),
                $(noClass(SomeClass.class.getName()).should().notHaveFullyQualifiedName(SomeClass.class.getName()),
                        noClass(SomeClass.class.getName()).should().haveFullyQualifiedName(SomeClass.class.getName())),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.notHaveFullyQualifiedName(SomeClass.class.getName())),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveFullyQualifiedName_rules")
    public void noClass_should_haveFullyQualifiedName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_haveSimpleName_rules() {
        return $$(
                $(theClass(SomeClass.class).should().haveSimpleName(SomeClass.class.getSimpleName()),
                        theClass(SomeClass.class).should().notHaveSimpleName(SomeClass.class.getSimpleName())),
                $(theClass(SomeClass.class).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())),
                        theClass(SomeClass.class).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName()))),
                $(theClass(SomeClass.class.getName()).should().haveSimpleName(SomeClass.class.getSimpleName()),
                        theClass(SomeClass.class.getName()).should().notHaveSimpleName(SomeClass.class.getSimpleName())),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveSimpleName_rules")
    public void haveSimpleName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_haveSimpleName_rules() {
        return $$(
                $(noClass(SomeClass.class).should().notHaveSimpleName(SomeClass.class.getSimpleName()),
                        noClass(SomeClass.class).should().haveSimpleName(SomeClass.class.getSimpleName())),
                $(noClass(SomeClass.class).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName()))),
                $(noClass(SomeClass.class.getName()).should().notHaveSimpleName(SomeClass.class.getSimpleName()),
                        noClass(SomeClass.class.getName()).should().haveSimpleName(SomeClass.class.getSimpleName())),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.notHaveSimpleName(SomeClass.class.getSimpleName())),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveSimpleName_rules")
    public void noClass_should_haveSimpleName(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return $$(
                $(theClass(SomeClass.class).should().haveNameMatching(regex),
                        theClass(SomeClass.class).should().haveNameNotMatching(regex)),
                $(theClass(SomeClass.class).should(ArchConditions.haveNameMatching(regex)),
                        theClass(SomeClass.class).should(ArchConditions.haveNameNotMatching(regex))),
                $(theClass(SomeClass.class.getName()).should().haveNameMatching(regex),
                        theClass(SomeClass.class.getName()).should().haveNameNotMatching(regex)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveNameMatching(regex)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveNameNotMatching(regex)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveNameMatching_rules")
    public void theClass_should_haveNameMatching(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have name matching '%s'",
                        SomeClass.class.getName(), regex)
                .haveFailingRuleText("the class %s should have name not matching '%s'",
                        SomeClass.class.getName(), regex)
                .containFailureDetail(String.format("Class <%s> matches '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(regex),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(regex));
    }

    @DataProvider
    public static Object[][] noClass_should_haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return $$(
                $(noClass(SomeClass.class).should().haveNameNotMatching(regex),
                        noClass(SomeClass.class).should().haveNameMatching(regex)),
                $(noClass(SomeClass.class).should(ArchConditions.haveNameNotMatching(regex)),
                        noClass(SomeClass.class).should(ArchConditions.haveNameMatching(regex))),
                $(noClass(SomeClass.class.getName()).should().haveNameNotMatching(regex),
                        noClass(SomeClass.class.getName()).should().haveNameMatching(regex)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.haveNameNotMatching(regex)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveNameMatching(regex)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveNameMatching_rules")
    public void noClass_should_haveNameMatching(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have name not matching '%s'",
                        SomeClass.class.getName(), regex)
                .haveFailingRuleText("no class %s should have name matching '%s'",
                        SomeClass.class.getName(), regex)
                .containFailureDetail(String.format("Class <%s> matches '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(regex),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(regex));
    }

    @DataProvider
    public static Object[][] theClass_should_haveSimpleNameStartingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return $$(
                $(theClass(SomeClass.class).should().haveSimpleNameStartingWith(prefix),
                        theClass(SomeClass.class).should().haveSimpleNameNotStartingWith(prefix)),
                $(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameStartingWith(prefix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotStartingWith(prefix))),
                $(theClass(SomeClass.class.getName()).should().haveSimpleNameStartingWith(prefix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotStartingWith(prefix)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameStartingWith(prefix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveSimpleNameStartingWith_rules")
    public void theClass_should_haveSimpleNameStartingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .haveFailingRuleText("the class %s should have simple name not starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .containFailureDetail(String.format("simple name of %s starts with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(prefix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] noClass_should_haveSimpleNameStartingWith_rules() {

        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return $$(
                $(noClass(SomeClass.class).should().haveSimpleNameNotStartingWith(prefix),
                        noClass(SomeClass.class).should().haveSimpleNameStartingWith(prefix)),
                $(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameStartingWith(prefix))),
                $(noClass(SomeClass.class.getName()).should().haveSimpleNameNotStartingWith(prefix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameStartingWith(prefix)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotStartingWith(prefix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameStartingWith(prefix)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveSimpleNameStartingWith_rules")
    public void noClass_should_haveSimpleNameStartingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .haveFailingRuleText("no class %s should have simple name starting with '%s'",
                        SomeClass.class.getName(), prefix)
                .containFailureDetail(String.format("simple name of %s starts with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(prefix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] theClass_should_haveSimpleNameContaining_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);

        return $$(
                $(theClass(SomeClass.class).should().haveSimpleNameContaining(infix),
                        theClass(SomeClass.class).should().haveSimpleNameNotContaining(infix)),
                $(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameContaining(infix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotContaining(infix))),
                $(theClass(SomeClass.class.getName()).should().haveSimpleNameContaining(infix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotContaining(infix)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameContaining(infix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotContaining(infix)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveSimpleNameContaining_rules")
    public void theClass_should_haveSimpleNameContaining(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name containing '%s'",
                        SomeClass.class.getName(), infix)
                .haveFailingRuleText("the class %s should have simple name not containing '%s'",
                        SomeClass.class.getName(), infix)
                .containFailureDetail(String.format("simple name of %s contains '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(infix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] noClass_should_haveSimpleNameContaining_rules() {

        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return $$(
                $(noClass(SomeClass.class).should().haveSimpleNameNotContaining(infix),
                        noClass(SomeClass.class).should().haveSimpleNameContaining(infix)),
                $(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotContaining(infix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameContaining(infix))),
                $(noClass(SomeClass.class.getName()).should().haveSimpleNameNotContaining(infix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameContaining(infix)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotContaining(infix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameContaining(infix)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveSimpleNameContaining_rules")
    public void noClass_should_haveSimpleNameContaining(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not containing '%s'",
                        SomeClass.class.getName(), infix)
                .haveFailingRuleText("no class %s should have simple name containing '%s'",
                        SomeClass.class.getName(), infix)
                .containFailureDetail(String.format("simple name of %s contains '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(infix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] theClass_should_haveSimpleNameEndingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);

        return $$(
                $(theClass(SomeClass.class).should().haveSimpleNameEndingWith(suffix),
                        theClass(SomeClass.class).should().haveSimpleNameNotEndingWith(suffix)),
                $(theClass(SomeClass.class).should(ArchConditions.haveSimpleNameEndingWith(suffix)),
                        theClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotEndingWith(suffix))),
                $(theClass(SomeClass.class.getName()).should().haveSimpleNameEndingWith(suffix),
                        theClass(SomeClass.class.getName()).should().haveSimpleNameNotEndingWith(suffix)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameEndingWith(suffix)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveSimpleNameEndingWith_rules")
    public void theClass_should_haveSimpleNameEndingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should have simple name ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .haveFailingRuleText("the class %s should have simple name not ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .containFailureDetail(String.format("simple name of %s ends with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(suffix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] noClass_should_haveSimpleNameEndingWith_rules() {

        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return $$(
                $(noClass(SomeClass.class).should().haveSimpleNameNotEndingWith(suffix),
                        noClass(SomeClass.class).should().haveSimpleNameEndingWith(suffix)),
                $(noClass(SomeClass.class).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)),
                        noClass(SomeClass.class).should(ArchConditions.haveSimpleNameEndingWith(suffix))),
                $(noClass(SomeClass.class.getName()).should().haveSimpleNameNotEndingWith(suffix),
                        noClass(SomeClass.class.getName()).should().haveSimpleNameEndingWith(suffix)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameNotEndingWith(suffix)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.haveSimpleNameEndingWith(suffix)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveSimpleNameEndingWith_rules")
    public void noClass_should_haveSimpleNameEndingWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should have simple name not ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .haveFailingRuleText("no class %s should have simple name ending with '%s'",
                        SomeClass.class.getName(), suffix)
                .containFailureDetail(String.format("simple name of %s ends with '%s' in %s",
                        quote(SomeClass.class.getName()),
                        quote(suffix),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] theClass_should_resideInAPackage_rules() {
        String thePackage = SomeClass.class.getPackage().getName();
        return $$(
                $(theClass(SomeClass.class).should().resideInAPackage(thePackage),
                        theClass(SomeClass.class).should().resideOutsideOfPackage(thePackage)),
                $(theClass(SomeClass.class).should(ArchConditions.resideInAPackage(thePackage)),
                        theClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackage(thePackage))),
                $(theClass(SomeClass.class.getName()).should().resideInAPackage(thePackage),
                        theClass(SomeClass.class.getName()).should().resideOutsideOfPackage(thePackage)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.resideInAPackage(thePackage)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackage(thePackage)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_resideInAPackage_rules")
    public void theClass_should_resideInAPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_resideInAPackage_rules() {
        String thePackage = SomeClass.class.getPackage().getName();
        return $$(
                $(noClass(SomeClass.class).should().resideOutsideOfPackage(thePackage),
                        noClass(SomeClass.class).should().resideInAPackage(thePackage)),
                $(noClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackage(thePackage)),
                        noClass(SomeClass.class).should(ArchConditions.resideInAPackage(thePackage))),
                $(noClass(SomeClass.class.getName()).should().resideOutsideOfPackage(thePackage),
                        noClass(SomeClass.class.getName()).should().resideInAPackage(thePackage)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackage(thePackage)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.resideInAPackage(thePackage)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_resideInAPackage_rules")
    public void noClass_should_resideInAPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_resideInAnyPackage_rules() {

        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        return $$(
                $(theClass(SomeClass.class).should().resideInAnyPackage(firstPackage, secondPackage),
                        theClass(SomeClass.class).should().resideOutsideOfPackages(firstPackage, secondPackage)),
                $(theClass(SomeClass.class).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        theClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage))),
                $(theClass(SomeClass.class.getName()).should().resideInAnyPackage(firstPackage, secondPackage),
                        theClass(SomeClass.class.getName()).should().resideOutsideOfPackages(firstPackage, secondPackage)),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        theClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_resideInAnyPackage_rules")
    public void theClass_should_resideInAnyPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        String[] packageIdentifiers = {firstPackage, secondPackage};

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("the class %s should reside in any package ['%s']",
                        SomeClass.class.getName(),
                        Joiner.on("', '").join(packageIdentifiers))
                .haveFailingRuleText("the class %s should reside outside of packages ['%s']",
                        SomeClass.class.getName(),
                        Joiner.on("', '").join(packageIdentifiers))
                .containFailureDetail(String.format("Class <%s> does not reside outside of packages \\['%s'\\] in %s",
                        quote(SomeClass.class.getName()),
                        quote(Joiner.on("', '").join(packageIdentifiers)),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] noClass_should_resideInAnyPackage_rules() {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();

        return $$(
                $(noClass(SomeClass.class).should().resideOutsideOfPackages(firstPackage, secondPackage),
                        noClass(SomeClass.class).should().resideInAnyPackage(firstPackage, secondPackage)),
                $(noClass(SomeClass.class).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        noClass(SomeClass.class).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage))),
                $(noClass(SomeClass.class.getName()).should().resideOutsideOfPackages(firstPackage, secondPackage),
                        noClass(SomeClass.class.getName()).should().resideInAnyPackage(firstPackage, secondPackage)),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        noClass(SomeClass.class.getName()).should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_resideInAnyPackage_rules")
    public void noClass_should_resideInAnyPackage(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        String firstPackage = SomeClass.class.getPackage().getName();
        String secondPackage = Object.class.getPackage().getName();
        String[] packageIdentifiers = {firstPackage, secondPackage};

        assertThatRules(satisfiedRule, unsatisfiedRule, SomeClass.class, Object.class)
                .haveSuccessfulRuleText("no class %s should reside outside of packages ['%s']",
                        SomeClass.class.getName(),
                        Joiner.on("', '").join(packageIdentifiers))
                .haveFailingRuleText("no class %s should reside in any package ['%s']",
                        SomeClass.class.getName(),
                        Joiner.on("', '").join(packageIdentifiers))
                .containFailureDetail(String.format("Class <%s> does reside in any package \\['%s'\\] in %s",
                        quote(SomeClass.class.getName()),
                        quote(Joiner.on("', '").join(packageIdentifiers)),
                        locationPattern(SomeClass.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] theClass_should_bePublic_rules() {
        return $$(
                $(theClass(SomeClass.class).should().bePublic(),
                        theClass(SomeClass.class).should().notBePublic()),
                $(theClass(SomeClass.class).should(ArchConditions.bePublic()),
                        theClass(SomeClass.class).should(ArchConditions.notBePublic())),
                $(theClass(SomeClass.class.getName()).should().bePublic(),
                        theClass(SomeClass.class.getName()).should().notBePublic()),
                $(theClass(SomeClass.class.getName()).should(ArchConditions.bePublic()),
                        theClass(SomeClass.class.getName()).should(ArchConditions.notBePublic()))
        );
    }

    @Test
    @UseDataProvider("theClass_should_bePublic_rules")
    public void theClass_should_bePublic(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_bePublic_rules() {
        return $$(
                $(noClass(SomeClass.class).should().notBePublic(),
                        noClass(SomeClass.class).should().bePublic()),
                $(noClass(SomeClass.class).should(ArchConditions.notBePublic()),
                        noClass(SomeClass.class).should(ArchConditions.bePublic())),
                $(noClass(SomeClass.class.getName()).should().notBePublic(),
                        noClass(SomeClass.class.getName()).should().bePublic()),
                $(noClass(SomeClass.class.getName()).should(ArchConditions.notBePublic()),
                        noClass(SomeClass.class.getName()).should(ArchConditions.bePublic()))
        );
    }

    @Test
    @UseDataProvider("noClass_should_bePublic_rules")
    public void noClass_should_bePublic(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_bePrivate_rules() {
        return $$(
                $(theClass(PrivateClass.class).should().bePrivate(),
                        theClass(PrivateClass.class).should().notBePrivate()),
                $(theClass(PrivateClass.class).should(ArchConditions.bePrivate()),
                        theClass(PrivateClass.class).should(ArchConditions.notBePrivate())),
                $(theClass(PrivateClass.class.getName()).should().bePrivate(),
                        theClass(PrivateClass.class.getName()).should().notBePrivate()),
                $(theClass(PrivateClass.class.getName()).should(ArchConditions.bePrivate()),
                        theClass(PrivateClass.class.getName()).should(ArchConditions.notBePrivate()))
        );
    }

    @Test
    @UseDataProvider("theClass_should_bePrivate_rules")
    public void theClass_should_bePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_bePrivate_rules() {
        return $$(
                $(noClass(PrivateClass.class).should().notBePrivate(),
                        noClass(PrivateClass.class).should().bePrivate()),
                $(noClass(PrivateClass.class).should(ArchConditions.notBePrivate()),
                        noClass(PrivateClass.class).should(ArchConditions.bePrivate())),
                $(noClass(PrivateClass.class.getName()).should().notBePrivate(),
                        noClass(PrivateClass.class.getName()).should().bePrivate()),
                $(noClass(PrivateClass.class.getName()).should(ArchConditions.notBePrivate()),
                        noClass(PrivateClass.class.getName()).should(ArchConditions.bePrivate()))
        );
    }

    @Test
    @UseDataProvider("noClass_should_bePrivate_rules")
    public void noClass_should_bePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_haveOnlyFinalFields_rules() {
        return $$(
                $(theClass(ClassWithFinalFields.class).should().haveOnlyFinalFields(),
                        theClass(ClassWithNonFinalFields.class).should().haveOnlyFinalFields()),
                $(theClass(ClassWithFinalFields.class).should(ArchConditions.haveOnlyFinalFields()),
                        theClass(ClassWithNonFinalFields.class).should(ArchConditions.haveOnlyFinalFields())),
                $(theClass(ClassWithFinalFields.class.getName()).should().haveOnlyFinalFields(),
                        theClass(ClassWithNonFinalFields.class.getName()).should().haveOnlyFinalFields()),
                $(theClass(ClassWithFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()),
                        theClass(ClassWithNonFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveOnlyFinalFields_rules")
    public void theClass_should_haveOnlyFinalFields(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_haveOnlyFinalFields_rules() {
        return $$(
                $(noClass(ClassWithNonFinalFields.class).should().haveOnlyFinalFields(),
                        noClass(ClassWithFinalFields.class).should().haveOnlyFinalFields()),
                $(noClass(ClassWithNonFinalFields.class).should(ArchConditions.haveOnlyFinalFields()),
                        noClass(ClassWithFinalFields.class).should(ArchConditions.haveOnlyFinalFields())),
                $(noClass(ClassWithNonFinalFields.class.getName()).should().haveOnlyFinalFields(),
                        noClass(ClassWithFinalFields.class.getName()).should().haveOnlyFinalFields()),
                $(noClass(ClassWithNonFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()),
                        noClass(ClassWithFinalFields.class.getName()).should(ArchConditions.haveOnlyFinalFields()))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveOnlyFinalFields_rules")
    public void noClass_should_haveOnlyFinalFields(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] classes_should_have_only_private_constructor_rules() {
        return testForEach(
                classes().should().haveOnlyPrivateConstructors(),
                classes().should(haveOnlyPrivateConstructors()));
    }

    @Test
    @UseDataProvider("classes_should_have_only_private_constructor_rules")
    public void classes_should_have_only_private_constructor(ArchRule rule) {
        assertThat(rule).hasDescriptionContaining("classes should have only private constructors");
        assertThat(rule).checking(importClasses(ClassWithPrivateConstructors.class))
                .hasNoViolation();
        assertThat(rule).checking(importClasses(ClassWithPublicAndPrivateConstructor.class))
                .hasOnlyViolations(String.format("Constructor <%s.<init>(%s)> is not private in (%s.java:0)",
                        ClassWithPublicAndPrivateConstructor.class.getName(), String.class.getName(), getClass().getSimpleName()));
    }

    @DataProvider
    public static Object[][] theClass_should_beProtected_rules() {
        return $$(
                $(theClass(ProtectedClass.class).should().beProtected(),
                        theClass(ProtectedClass.class).should().notBeProtected()),
                $(theClass(ProtectedClass.class).should(ArchConditions.beProtected()),
                        theClass(ProtectedClass.class).should(ArchConditions.notBeProtected())),
                $(theClass(ProtectedClass.class.getName()).should().beProtected(),
                        theClass(ProtectedClass.class.getName()).should().notBeProtected()),
                $(theClass(ProtectedClass.class.getName()).should(ArchConditions.beProtected()),
                        theClass(ProtectedClass.class.getName()).should(ArchConditions.notBeProtected()))
        );
    }

    @Test
    @UseDataProvider("theClass_should_beProtected_rules")
    public void theClass_should_beProtected(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_beProtected_rules() {
        return $$(
                $(noClass(ProtectedClass.class).should().notBeProtected(),
                        noClass(ProtectedClass.class).should().beProtected()),
                $(noClass(ProtectedClass.class).should(ArchConditions.notBeProtected()),
                        noClass(ProtectedClass.class).should(ArchConditions.beProtected())),
                $(noClass(ProtectedClass.class.getName()).should().notBeProtected(),
                        noClass(ProtectedClass.class.getName()).should().beProtected()),
                $(noClass(ProtectedClass.class.getName()).should(ArchConditions.notBeProtected()),
                        noClass(ProtectedClass.class.getName()).should(ArchConditions.beProtected()))
        );
    }

    @Test
    @UseDataProvider("noClass_should_beProtected_rules")
    public void noClass_should_beProtected(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_bePackagePrivate_rules() {
        return $$(
                $(theClass(PackagePrivateClass.class).should().bePackagePrivate(),
                        theClass(PackagePrivateClass.class).should().notBePackagePrivate()),
                $(theClass(PackagePrivateClass.class).should(ArchConditions.bePackagePrivate()),
                        theClass(PackagePrivateClass.class).should(ArchConditions.notBePackagePrivate())),
                $(theClass(PackagePrivateClass.class.getName()).should().bePackagePrivate(),
                        theClass(PackagePrivateClass.class.getName()).should().notBePackagePrivate()),
                $(theClass(PackagePrivateClass.class.getName()).should(ArchConditions.bePackagePrivate()),
                        theClass(PackagePrivateClass.class.getName()).should(ArchConditions.notBePackagePrivate()))
        );
    }

    @Test
    @UseDataProvider("theClass_should_bePackagePrivate_rules")
    public void theClass_should_bePackagePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_bePackagePrivate_rules() {
        return $$(
                $(noClass(PackagePrivateClass.class).should().notBePackagePrivate(),
                        noClass(PackagePrivateClass.class).should().bePackagePrivate()),
                $(noClass(PackagePrivateClass.class).should(ArchConditions.notBePackagePrivate()),
                        noClass(PackagePrivateClass.class).should(ArchConditions.bePackagePrivate())),
                $(noClass(PackagePrivateClass.class.getName()).should().notBePackagePrivate(),
                        noClass(PackagePrivateClass.class.getName()).should().bePackagePrivate()),
                $(noClass(PackagePrivateClass.class.getName()).should(ArchConditions.notBePackagePrivate()),
                        noClass(PackagePrivateClass.class.getName()).should(ArchConditions.bePackagePrivate()))
        );
    }

    @Test
    @UseDataProvider("noClass_should_bePackagePrivate_rules")
    public void noClass_should_bePackagePrivate(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_haveModifier_public_rules() {
        return $$(
                $(theClass(PublicClass.class).should().haveModifier(PUBLIC),
                        theClass(PublicClass.class).should().notHaveModifier(PUBLIC)),
                $(theClass(PublicClass.class).should(ArchConditions.haveModifier(PUBLIC)),
                        theClass(PublicClass.class).should(ArchConditions.notHaveModifier(PUBLIC))),
                $(theClass(PublicClass.class.getName()).should().haveModifier(PUBLIC),
                        theClass(PublicClass.class.getName()).should().notHaveModifier(PUBLIC)),
                $(theClass(PublicClass.class.getName()).should(ArchConditions.haveModifier(PUBLIC)),
                        theClass(PublicClass.class.getName()).should(ArchConditions.notHaveModifier(PUBLIC)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_haveModifier_public_rules")
    public void theClass_should_haveModifier_public(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_haveModifier_public_rules() {
        return $$(
                $(noClass(PublicClass.class).should().notHaveModifier(PUBLIC),
                        noClass(PublicClass.class).should().haveModifier(PUBLIC)),
                $(noClass(PublicClass.class).should(ArchConditions.notHaveModifier(PUBLIC)),
                        noClass(PublicClass.class).should(ArchConditions.haveModifier(PUBLIC))),
                $(noClass(PublicClass.class.getName()).should().notHaveModifier(PUBLIC),
                        noClass(PublicClass.class.getName()).should().haveModifier(PUBLIC)),
                $(noClass(PublicClass.class.getName()).should(ArchConditions.notHaveModifier(PUBLIC)),
                        noClass(PublicClass.class.getName()).should(ArchConditions.haveModifier(PUBLIC)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_haveModifier_public_rules")
    public void noClass_should_haveModifier_public(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_beAnnotatedWith_rules() {
        return $$(
                $(theClass(SomeAnnotatedClass.class).should().beAnnotatedWith(RuntimeRetentionAnnotation.class),
                        theClass(SomeAnnotatedClass.class).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                $(theClass(SomeAnnotatedClass.class).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        theClass(SomeAnnotatedClass.class).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class))),
                $(theClass(SomeAnnotatedClass.class.getName()).should().beAnnotatedWith(RuntimeRetentionAnnotation.class),
                        theClass(SomeAnnotatedClass.class.getName()).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                $(theClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        theClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_beAnnotatedWith_rules")
    public void theClass_should_beAnnotatedWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_beAnnotatedWith_rules() {
        return $$(
                $(noClass(SomeAnnotatedClass.class).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class),
                        noClass(SomeAnnotatedClass.class).should().beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                $(noClass(SomeAnnotatedClass.class).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        noClass(SomeAnnotatedClass.class).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class))),
                $(noClass(SomeAnnotatedClass.class.getName()).should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class),
                        noClass(SomeAnnotatedClass.class.getName()).should().beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                $(noClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        noClass(SomeAnnotatedClass.class.getName()).should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_beAnnotatedWith_rules")
    public void noClass_should_beAnnotatedWith(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_implement_rules() {
        return $$(
                $(theClass(ArrayList.class).should().implement(Collection.class),
                        theClass(ArrayList.class).should().notImplement(Collection.class)),
                $(theClass(ArrayList.class).should(ArchConditions.implement(Collection.class)),
                        theClass(ArrayList.class).should(ArchConditions.notImplement(Collection.class))),
                $(theClass(ArrayList.class.getName()).should().implement(Collection.class),
                        theClass(ArrayList.class.getName()).should().notImplement(Collection.class)),
                $(theClass(ArrayList.class.getName()).should(ArchConditions.implement(Collection.class)),
                        theClass(ArrayList.class.getName()).should(ArchConditions.notImplement(Collection.class)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_implement_rules")
    public void theClass_should_implement(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ArrayList.class, Object.class)
                .haveSuccessfulRuleText("the class %s should implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .haveFailingRuleText("the class %s should not implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> implements %s in %s",
                        quote(ArrayList.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(ArrayList.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] noClass_should_implement_rules() {
        return $$(
                $(noClass(ArrayList.class).should().notImplement(Collection.class),
                        noClass(ArrayList.class).should().implement(Collection.class)),
                $(noClass(ArrayList.class).should(ArchConditions.notImplement(Collection.class)),
                        noClass(ArrayList.class).should(ArchConditions.implement(Collection.class))),
                $(noClass(ArrayList.class.getName()).should().notImplement(Collection.class),
                        noClass(ArrayList.class.getName()).should().implement(Collection.class)),
                $(noClass(ArrayList.class.getName()).should(ArchConditions.notImplement(Collection.class)),
                        noClass(ArrayList.class.getName()).should(ArchConditions.implement(Collection.class)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_implement_rules")
    public void noClass_should_implement(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
        assertThatRules(satisfiedRule, unsatisfiedRule, ArrayList.class, Object.class)
                .haveSuccessfulRuleText("no class %s should not implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .haveFailingRuleText("no class %s should implement %s",
                        ArrayList.class.getName(), Collection.class.getName())
                .containFailureDetail(String.format("Class <%s> implements %s in %s",
                        quote(ArrayList.class.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(ArrayList.class)))
                .doNotContainFailureDetail(quote(Object.class.getName()));
    }

    @DataProvider
    public static Object[][] theClass_should_beAssignableTo_rules() {
        return $$(
                $(theClass(List.class).should().beAssignableTo(Collection.class),
                        theClass(List.class).should().notBeAssignableTo(Collection.class)),
                $(theClass(List.class).should(ArchConditions.beAssignableTo(Collection.class)),
                        theClass(List.class).should(ArchConditions.notBeAssignableTo(Collection.class))),
                $(theClass(List.class.getName()).should().beAssignableTo(Collection.class),
                        theClass(List.class.getName()).should().notBeAssignableTo(Collection.class)),
                $(theClass(List.class.getName()).should(ArchConditions.beAssignableTo(Collection.class)),
                        theClass(List.class.getName()).should(ArchConditions.notBeAssignableTo(Collection.class)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_beAssignableTo_rules")
    public void theClass_should_beAssignableTo(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_beAssignableTo_rules() {
        return $$(
                $(noClass(List.class).should().notBeAssignableTo(Collection.class),
                        noClass(List.class).should().beAssignableTo(Collection.class)),
                $(noClass(List.class).should(ArchConditions.notBeAssignableTo(Collection.class)),
                        noClass(List.class).should(ArchConditions.beAssignableTo(Collection.class))),
                $(noClass(List.class.getName()).should().notBeAssignableTo(Collection.class),
                        noClass(List.class.getName()).should().beAssignableTo(Collection.class)),
                $(noClass(List.class.getName()).should(ArchConditions.notBeAssignableTo(Collection.class)),
                        noClass(List.class.getName()).should(ArchConditions.beAssignableTo(Collection.class)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_beAssignableTo_rules")
    public void noClass_should_beAssignableTo(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_beAssignableFrom_rules() {
        return $$(
                $(theClass(Collection.class).should().beAssignableFrom(List.class),
                        theClass(Collection.class).should().notBeAssignableFrom(List.class)),
                $(theClass(Collection.class).should(ArchConditions.beAssignableFrom(List.class)),
                        theClass(Collection.class).should(ArchConditions.notBeAssignableFrom(List.class))),
                $(theClass(Collection.class.getName()).should().beAssignableFrom(List.class),
                        theClass(Collection.class.getName()).should().notBeAssignableFrom(List.class)),
                $(theClass(Collection.class.getName()).should(ArchConditions.beAssignableFrom(List.class)),
                        theClass(Collection.class.getName()).should(ArchConditions.notBeAssignableFrom(List.class)))
        );
    }

    @Test
    @UseDataProvider("theClass_should_beAssignableFrom_rules")
    public void theClass_should_beAssignableFrom(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_beAssignableFrom_rules() {
        return $$(
                $(noClass(Collection.class).should().notBeAssignableFrom(List.class),
                        noClass(Collection.class).should().beAssignableFrom(List.class)),
                $(noClass(Collection.class).should(ArchConditions.notBeAssignableFrom(List.class)),
                        noClass(Collection.class).should(ArchConditions.beAssignableFrom(List.class))),
                $(noClass(Collection.class.getName()).should().notBeAssignableFrom(List.class),
                        noClass(Collection.class.getName()).should().beAssignableFrom(List.class)),
                $(noClass(Collection.class.getName()).should(ArchConditions.notBeAssignableFrom(List.class)),
                        noClass(Collection.class.getName()).should(ArchConditions.beAssignableFrom(List.class)))
        );
    }

    @Test
    @UseDataProvider("noClass_should_beAssignableFrom_rules")
    public void noClass_should_beAssignableFrom(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_getField_rules() {
        return $$(
                $(theClass(ClassAccessingField.class).should().getField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class).should().getField(ClassWithField.class, "field")),
                $(theClass(ClassAccessingField.class).should(ArchConditions.getField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.getField(ClassWithField.class, "field"))),
                $(theClass(ClassAccessingField.class.getName()).should().getField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class.getName()).should().getField(ClassWithField.class, "field")),
                $(theClass(ClassAccessingField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")))
        );
    }

    @Test
    @UseDataProvider("theClass_should_getField_rules")
    public void theClass_should_getField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_getField_rules() {
        return $$(
                $(noClass(ClassAccessingWrongField.class).should().getField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class).should().getField(ClassWithField.class, "field")),
                $(noClass(ClassAccessingWrongField.class).should(ArchConditions.getField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class).should(ArchConditions.getField(ClassWithField.class, "field"))),
                $(noClass(ClassAccessingWrongField.class.getName()).should().getField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class.getName()).should().getField(ClassWithField.class, "field")),
                $(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.getField(ClassWithField.class, "field")))
        );
    }

    @Test
    @UseDataProvider("noClass_should_getField_rules")
    public void noClass_should_getField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_accessField_rules() {
        return $$(
                $(theClass(ClassAccessingField.class).should().accessField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class).should().accessField(ClassWithField.class, "field")),
                $(theClass(ClassAccessingField.class).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.accessField(ClassWithField.class, "field"))),
                $(theClass(ClassAccessingField.class.getName()).should().accessField(ClassWithField.class, "field"),
                        theClass(ClassAccessingWrongField.class.getName()).should().accessField(ClassWithField.class, "field")),
                $(theClass(ClassAccessingField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")))
        );
    }

    @Test
    @UseDataProvider("theClass_should_accessField_rules")
    public void theClass_should_accessField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_accessField_rules() {
        return $$(
                $(noClass(ClassAccessingWrongField.class).should().accessField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class).should().accessField(ClassWithField.class, "field")),
                $(noClass(ClassAccessingWrongField.class).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class).should(ArchConditions.accessField(ClassWithField.class, "field"))),
                $(noClass(ClassAccessingWrongField.class.getName()).should().accessField(ClassWithField.class, "field"),
                        noClass(ClassAccessingField.class.getName()).should().accessField(ClassWithField.class, "field")),
                $(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.accessField(ClassWithField.class, "field")))
        );
    }

    @Test
    @UseDataProvider("noClass_should_accessField_rules")
    public void noClass_should_accessField(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_getFieldWhere_rules() {
        return $$(
                $(theClass(ClassAccessingField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                $(theClass(ClassAccessingField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class)))),
                $(theClass(ClassAccessingField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                $(theClass(ClassAccessingField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @Test
    @UseDataProvider("theClass_should_getFieldWhere_rules")
    public void theClass_should_getFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_getFieldWhere_rules() {
        return $$(
                $(noClass(ClassAccessingWrongField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                $(noClass(ClassAccessingWrongField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class)))),
                $(noClass(ClassAccessingWrongField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class.getName()).should().getFieldWhere(accessTargetIs(ClassWithField.class))),
                $(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @Test
    @UseDataProvider("noClass_should_getFieldWhere_rules")
    public void noClass_should_getFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] theClass_should_accessFieldWhere_rules() {
        return $$(
                $(theClass(ClassAccessingField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                $(theClass(ClassAccessingField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class)))),
                $(theClass(ClassAccessingField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        theClass(ClassAccessingWrongField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                $(theClass(ClassAccessingField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        theClass(ClassAccessingWrongField.class.getName())
                                .should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @Test
    @UseDataProvider("theClass_should_accessFieldWhere_rules")
    public void theClass_should_accessFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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

    @DataProvider
    public static Object[][] noClass_should_accessFieldWhere_rules() {
        return $$(
                $(noClass(ClassAccessingWrongField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                $(noClass(ClassAccessingWrongField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class)))),
                $(noClass(ClassAccessingWrongField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class)),
                        noClass(ClassAccessingField.class.getName()).should().accessFieldWhere(accessTargetIs(ClassWithField.class))),
                $(noClass(ClassAccessingWrongField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))),
                        noClass(ClassAccessingField.class.getName()).should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))))
        );
    }

    @Test
    @UseDataProvider("noClass_should_accessFieldWhere_rules")
    public void noClass_should_accessFieldWhere(ArchRule satisfiedRule, ArchRule unsatisfiedRule) {
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
        private EvaluationResult satisfiedResult;
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

    private static void assertViolation(EvaluationResult result) {
        assertThat(result.hasViolation()).as("result has violation").isTrue();
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

    @SuppressWarnings({"unused", "WeakerAccess"})
    private static class ClassWithPublicAndPrivateConstructor {
        public ClassWithPublicAndPrivateConstructor(String s) {
        }

        private ClassWithPublicAndPrivateConstructor(Integer i) {
            this(i.toString());
        }
    }
}