package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importHierarchies;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.conditions.ArchConditions.bePackagePrivate;
import static com.tngtech.archunit.lang.conditions.ArchConditions.bePrivate;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beProtected;
import static com.tngtech.archunit.lang.conditions.ArchConditions.bePublic;
import static com.tngtech.archunit.lang.conditions.ArchConditions.haveModifier;
import static com.tngtech.archunit.lang.conditions.ArchConditions.notBePackagePrivate;
import static com.tngtech.archunit.lang.conditions.ArchConditions.notBePrivate;
import static com.tngtech.archunit.lang.conditions.ArchConditions.notBeProtected;
import static com.tngtech.archunit.lang.conditions.ArchConditions.notBePublic;
import static com.tngtech.archunit.lang.conditions.ArchConditions.notHaveModifier;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldTest {
    private static final String FAILURE_REPORT_NEWLINE_MARKER = "#";

    @DataProvider
    public static Object[][] haveFullyQualifiedName_rules() {
        return $$(
                $(classes().should().haveFullyQualifiedName(RightNamedClass.class.getName())),
                $(classes().should(ArchConditions.haveFullyQualifiedName(RightNamedClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("haveFullyQualifiedName_rules")
    public void haveFullyQualifiedName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have fully qualified name '%s'", RightNamedClass.class.getName()))
                .contains(String.format("class %s doesn't have fully qualified name '%s'",
                        WrongNamedClass.class.getName(), RightNamedClass.class.getName()))
                .doesNotMatch(String.format("%s .*name", RightNamedClass.class.getName()));
    }

    @DataProvider
    public static Object[][] notHaveFullyQualifiedName_rules() {
        return $$(
                $(classes().should().notHaveFullyQualifiedName(WrongNamedClass.class.getName())),
                $(classes().should(ArchConditions.notHaveFullyQualifiedName(WrongNamedClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("notHaveFullyQualifiedName_rules")
    public void notHaveFullyQualifiedName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not have fully qualified name '%s'", WrongNamedClass.class.getName()))
                .contains(String.format("%s has fully qualified name '%s'", WrongNamedClass.class.getName(), WrongNamedClass.class.getName()))
                .doesNotContain(String.format("%s .*name", RightNamedClass.class.getName()));
    }

    @DataProvider
    public static Object[][] haveSimpleName_rules() {
        return $$(
                $(classes().should().haveSimpleName(RightNamedClass.class.getSimpleName())),
                $(classes().should(ArchConditions.haveSimpleName(RightNamedClass.class.getSimpleName())))
        );
    }

    @Test
    @UseDataProvider("haveSimpleName_rules")
    public void haveSimpleName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name '%s'", RightNamedClass.class.getSimpleName()))
                .contains(String.format("class %s doesn't have simple name '%s'",
                        WrongNamedClass.class.getName(), RightNamedClass.class.getSimpleName()))
                .doesNotMatch(String.format(".*class %s .*simple name.*", RightNamedClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] notHaveSimpleName_rules() {
        return $$(
                $(classes().should().notHaveSimpleName(WrongNamedClass.class.getSimpleName())),
                $(classes().should(ArchConditions.notHaveSimpleName(WrongNamedClass.class.getSimpleName())))
        );
    }

    @Test
    @UseDataProvider("notHaveSimpleName_rules")
    public void notHaveSimpleName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not have simple name '%s'", WrongNamedClass.class.getSimpleName()))
                .contains(String.format("%s has simple name '%s'",
                        WrongNamedClass.class.getName(), WrongNamedClass.class.getSimpleName()))
                .doesNotMatch(String.format(".*class %s .*simple name.*", RightNamedClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] haveNameMatching_rules() {
        String regex = containsPartOfRegex(RightNamedClass.class.getSimpleName());
        return $$(
                $(classes().should().haveNameMatching(regex), regex),
                $(classes().should(ArchConditions.haveNameMatching(regex)), regex)
        );
    }

    @Test
    @UseDataProvider("haveNameMatching_rules")
    public void haveNameMatching(ArchRule rule, String regex) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name matching '%s'", regex))
                .contains(String.format("%s doesn't match '%s'",
                        WrongNamedClass.class.getName(), regex))
                .doesNotContain(String.format("%s", RightNamedClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] haveNameNotMatching_rules() {
        String regex = containsPartOfRegex(WrongNamedClass.class.getSimpleName());
        return $$(
                $(classes().should().haveNameNotMatching(regex), regex),
                $(classes().should(ArchConditions.haveNameNotMatching(regex)), regex)
        );
    }

    @Test
    @UseDataProvider("haveNameNotMatching_rules")
    public void haveNameNotMatching(ArchRule rule, String regex) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name not matching '%s'", regex))
                .contains(String.format("%s matches '%s'",
                        WrongNamedClass.class.getName(), regex))
                .doesNotContain(String.format("%s", RightNamedClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] haveSimpleClassnameEndingWith_rules() {
        String className = RightNamedClass.class.getSimpleName();
        String prefix = className.substring(1, className.length());
        return $$(
                $(classes().should().haveSimpleClassNameEndingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleClassNameEndingWith(prefix)), prefix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleClassnameEndingWith_rules")
    public void haveSimpleClassNameEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple class name ending with '%s'", suffix))
                .contains(String.format("simple class name of %s doesn't end with '%s'",
                        WrongNamedClass.class.getName(), suffix))
                .doesNotContain(RightNamedClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleClassNameNotEndingWith_rules() {
        String className = WrongNamedClass.class.getSimpleName();
        String prefix = className.substring(1, className.length());
        return $$(
                $(classes().should().haveSimpleClassNameNotEndingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleClassNameNotEndingWith(prefix)), prefix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleClassNameNotEndingWith_rules")
    public void haveSimpleClassNameNotEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                RightNamedClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple class name not ending with '%s'", suffix))
                .contains(String.format("simple class name of %s ends with '%s'",
                        WrongNamedClass.class.getName(), suffix))
                .doesNotContain(RightNamedClass.class.getName());
    }


    @DataProvider
    public static Object[][] resideInAPackage_rules() {
        String thePackage = ArchRule.class.getPackage().getName();
        return $$(
                $(classes().should().resideInAPackage(thePackage), thePackage),
                $(classes().should(ArchConditions.resideInAPackage(thePackage)), thePackage)
        );
    }

    @Test
    @UseDataProvider("resideInAPackage_rules")
    public void resideInAPackage(ArchRule rule, String packageIdentifier) {
        checkTestStillValid(packageIdentifier,
                ImmutableSet.of(ArchRule.class, ArchCondition.class),
                ImmutableSet.<Class<?>>of(ArchConfiguration.class),
                ImmutableSet.<Class<?>>of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside in a package '%s'", packageIdentifier))
                .contains(doesntResideInAPackageMessageFor(ArchConfiguration.class, packageIdentifier))
                .contains(doesntResideInAPackageMessageFor(GivenObjects.class, packageIdentifier))
                .doesNotContain(String.format("%s", ArchRule.class.getSimpleName()))
                .doesNotContain(String.format("%s", ArchCondition.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] resideInAnyPackage_rules() {
        String firstPackage = ArchRule.class.getPackage().getName();
        String secondPackage = ArchConfiguration.class.getPackage().getName();
        return $$(
                $(classes().should().resideInAnyPackage(firstPackage, secondPackage),
                        new String[]{firstPackage, secondPackage}),
                $(classes().should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        new String[]{firstPackage, secondPackage})
        );
    }

    @Test
    @UseDataProvider("resideInAnyPackage_rules")
    public void resideInAnyPackage(ArchRule rule, String... packageIdentifiers) {
        checkTestStillValid(packageIdentifiers,
                ImmutableSet.of(ArchRule.class, ArchConfiguration.class),
                ImmutableSet.<Class<?>>of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside in any package ['%s']",
                        Joiner.on("', '").join(packageIdentifiers)))
                .contains(doesntResideInAnyPackageMessageFor(GivenObjects.class, packageIdentifiers))
                .doesNotContain(String.format("%s", ArchRule.class.getSimpleName()))
                .doesNotContain(String.format("%s", ArchConfiguration.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] resideOutsideOfPackage_rules() {
        String thePackage = ArchRule.class.getPackage().getName();
        return $$(
                $(classes().should().resideOutsideOfPackage(thePackage), thePackage),
                $(classes().should(ArchConditions.resideOutsideOfPackage(thePackage)), thePackage)
        );
    }

    @Test
    @UseDataProvider("resideOutsideOfPackage_rules")
    public void resideOutsideOfPackage(ArchRule rule, String packageIdentifier) {
        checkTestStillValid(packageIdentifier,
                ImmutableSet.of(ArchRule.class, ArchCondition.class),
                ImmutableSet.<Class<?>>of(ArchConfiguration.class),
                ImmutableSet.<Class<?>>of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside outside of package '%s'", packageIdentifier))
                .contains(doesntResideOutsideOfPackageMessageFor(ArchRule.class, packageIdentifier))
                .contains(doesntResideOutsideOfPackageMessageFor(ArchCondition.class, packageIdentifier))
                .doesNotContain(String.format("%s", ArchConfiguration.class.getSimpleName()))
                .doesNotContain(String.format("%s", GivenObjects.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] resideOutsideOfPackages_rules() {
        String firstPackage = ArchRule.class.getPackage().getName();
        String secondPackage = ArchConfiguration.class.getPackage().getName();
        return $$(
                $(classes().should().resideOutsideOfPackages(firstPackage, secondPackage),
                        new String[]{firstPackage, secondPackage}),
                $(classes().should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        new String[]{firstPackage, secondPackage})
        );
    }

    @Test
    @UseDataProvider("resideOutsideOfPackages_rules")
    public void resideOutsideOfPackages(ArchRule rule, String... packageIdentifiers) {
        checkTestStillValid(packageIdentifiers,
                ImmutableSet.of(ArchRule.class, ArchConfiguration.class),
                ImmutableSet.<Class<?>>of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside outside of packages ['%s']",
                        Joiner.on("', '").join(packageIdentifiers)))
                .contains(doesntResideOutsideOfPackagesMessageFor(ArchRule.class, packageIdentifiers))
                .contains(doesntResideOutsideOfPackagesMessageFor(ArchCondition.class, packageIdentifiers))
                .doesNotContain(String.format("%s", GivenObjects.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] visibility_rules() {
        return $$(
                $(classes().should().bePublic(), PUBLIC, PublicClass.class, PrivateClass.class),
                $(classes().should(bePublic()), PUBLIC, PublicClass.class, PrivateClass.class),
                $(classes().should().beProtected(), PROTECTED, ProtectedClass.class, PrivateClass.class),
                $(classes().should(beProtected()), PROTECTED, ProtectedClass.class, PrivateClass.class),
                $(classes().should().bePrivate(), PRIVATE, PrivateClass.class, PublicClass.class),
                $(classes().should(bePrivate()), PRIVATE, PrivateClass.class, PublicClass.class));
    }

    @Test
    @UseDataProvider("visibility_rules")
    public void visibility(ArchRule rule, JavaModifier modifier,
            Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be %s", modifier.name().toLowerCase()))
                .containsPattern(String.format("class %s .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*class %s .* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    @DataProvider
    public static Object[][] not_visibility_rules() {
        return $$(
                $(classes().should().notBePublic(), PUBLIC, PrivateClass.class, PublicClass.class),
                $(classes().should(notBePublic()), PUBLIC, PrivateClass.class, PublicClass.class),
                $(classes().should().notBeProtected(), PROTECTED, PrivateClass.class, ProtectedClass.class),
                $(classes().should(notBeProtected()), PROTECTED, PrivateClass.class, ProtectedClass.class),
                $(classes().should().notBePrivate(), PRIVATE, PublicClass.class, PrivateClass.class),
                $(classes().should(notBePrivate()), PRIVATE, PublicClass.class, PrivateClass.class));
    }

    @Test
    @UseDataProvider("not_visibility_rules")
    public void notVisibility(ArchRule rule, JavaModifier modifier,
            Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be %s", modifier.name().toLowerCase()))
                .containsPattern(String.format("class %s .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*class %s .* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    @DataProvider
    public static Object[][] package_private_visibility_rules() {
        return $$(
                $(classes().should().bePackagePrivate(), "be package private"),
                $(classes().should(bePackagePrivate()), "be package private"));
    }

    @Test
    @UseDataProvider("package_private_visibility_rules")
    public void package_private_visibility(ArchRule rule, String description) {
        EvaluationResult result = rule.evaluate(importClasses(PackagePrivateClass.class, PrivateClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should " + description)
                .containsPattern(String.format("class %s .* modifier %s", quote(PrivateClass.class.getName()), PRIVATE))
                .doesNotMatch(String.format(".*class %s .* modifier.*", quote(PackagePrivateClass.class.getName())));
    }

    @DataProvider
    public static Object[][] non_package_private_visibility_rules() {
        return $$(
                $(classes().should().notBePackagePrivate(), "not be package private"),
                $(classes().should(notBePackagePrivate()), "not be package private"));
    }

    @Test
    @UseDataProvider("non_package_private_visibility_rules")
    public void non_package_private_visibility(ArchRule rule, String description) {
        EvaluationResult result = rule.evaluate(importClasses(PrivateClass.class, PackagePrivateClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should " + description)
                .contains(String.format("class %s", PackagePrivateClass.class.getName()))
                .contains("doesn't have modifier " + PUBLIC)
                .contains("doesn't have modifier " + PROTECTED)
                .contains("doesn't have modifier " + PRIVATE)
                .doesNotMatch(String.format(".*class %s .* modifier.*", quote(PrivateClass.class.getName())));
    }

    @DataProvider
    public static Object[][] modifiers_rules() {
        return $$(
                $(classes().should().haveModifier(PUBLIC), "", PUBLIC,
                        PublicClass.class, PrivateClass.class),
                $(classes().should(haveModifier(PUBLIC)), "", PUBLIC,
                        PublicClass.class, PrivateClass.class),
                $(classes().should().notHaveModifier(PUBLIC), "not ", PUBLIC,
                        PrivateClass.class, PublicClass.class),
                $(classes().should(notHaveModifier(PUBLIC)), "not ", PUBLIC,
                        PrivateClass.class, PublicClass.class));
    }

    @Test
    @UseDataProvider("modifiers_rules")
    public void modifiers(ArchRule rule, String havePrefix, JavaModifier modifier,
            Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %shave modifier %s", havePrefix, modifier.name()))
                .containsPattern(String.format("class %s .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*class %s .* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    @DataProvider
    public static Object[][] annotated_rules() {
        return $$(
                $(classes().should().beAnnotatedWith(SomeAnnotation.class),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(SomeAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beAnnotatedWith(SomeAnnotation.class.getName()),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(SomeAnnotation.class.getName())),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beAnnotatedWith(annotation(SomeAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(annotation(SomeAnnotation.class))),
                        SomeAnnotatedClass.class, String.class));
    }

    @Test
    @UseDataProvider("annotated_rules")
    public void annotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be annotated with @%s", SomeAnnotation.class.getSimpleName()))
                .contains(String.format("class %s is not annotated with @%s",
                        wrongClass.getName(), SomeAnnotation.class.getSimpleName()))
                .doesNotMatch(String.format(".*%s.*annotated.*", quote(correctClass.getName())));
    }

    @DataProvider
    public static Object[][] notAnnotated_rules() {
        return $$(
                $(classes().should().notBeAnnotatedWith(SomeAnnotation.class),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(SomeAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeAnnotatedWith(SomeAnnotation.class.getName()),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(SomeAnnotation.class.getName())),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeAnnotatedWith(annotation(SomeAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(annotation(SomeAnnotation.class))),
                        String.class, SomeAnnotatedClass.class));
    }

    @Test
    @UseDataProvider("notAnnotated_rules")
    public void notAnnotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be annotated with @" + SomeAnnotation.class.getSimpleName())
                .contains(String.format("class %s is annotated with @%s",
                        wrongClass.getName(), SomeAnnotation.class.getSimpleName()))
                .doesNotMatch(String.format(".*%s.*annotated.*", quote(correctClass.getName())));
    }

    @DataProvider
    public static Object[][] implement_satisfied_rules() {
        return $$(
                $(classes().should().implement(Collection.class), ArrayList.class),
                $(classes().should(ArchConditions.implement(Collection.class)), ArrayList.class),
                $(classes().should().implement(Collection.class.getName()), ArrayList.class),
                $(classes().should(ArchConditions.implement(Collection.class.getName())), ArrayList.class),
                $(classes().should().implement(name(Collection.class.getName()).as(Collection.class.getName())), ArrayList.class),
                $(classes().should(ArchConditions.implement(name(Collection.class.getName()).as(Collection.class.getName()))), ArrayList.class));
    }

    @Test
    @UseDataProvider("implement_satisfied_rules")
    public void implement_satisfied(ArchRule rule, Class<?> satisfied) {
        EvaluationResult result = rule.evaluate(importHierarchies(satisfied));

        assertThat(singleLineFailureReportOf(result))
                .doesNotMatch(String.format(".*class %s .* implement.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static List<List<?>> implement_not_satisfied_rules() {
        return ImmutableList.<List<?>>builder()
                .addAll(implementNotSatisfiedCases(Collection.class, List.class))
                .addAll(implementNotSatisfiedCases(AbstractList.class, ArrayList.class))
                .build();
    }

    private static List<List<?>> implementNotSatisfiedCases(Class<?> classToCheckAgainst, Class<?> violating) {
        return ImmutableList.<List<?>>of(
                ImmutableList.of(classes().should().implement(classToCheckAgainst),
                        classToCheckAgainst, violating),
                ImmutableList.of(classes().should(ArchConditions.implement(classToCheckAgainst)),
                        classToCheckAgainst, violating),
                ImmutableList.of(classes().should().implement(classToCheckAgainst.getName()),
                        classToCheckAgainst, violating),
                ImmutableList.of(classes().should(ArchConditions.implement(classToCheckAgainst.getName())),
                        classToCheckAgainst, violating),
                ImmutableList.of(classes().should().implement(name(classToCheckAgainst.getName()).as(classToCheckAgainst.getName())),
                        classToCheckAgainst, violating),
                ImmutableList.of(classes().should(ArchConditions.implement(name(classToCheckAgainst.getName()).as(classToCheckAgainst.getName()))),
                        classToCheckAgainst, violating));
    }

    @Test
    @UseDataProvider("implement_not_satisfied_rules")
    public void implement_not_satisfied(ArchRule rule, Class<?> classToCheckAgainst, Class<?> violating) {
        EvaluationResult result = rule.evaluate(importHierarchies(violating));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should implement %s", classToCheckAgainst.getName()))
                .contains(String.format("class %s doesn't implement %s", violating.getName(), classToCheckAgainst.getName()));
    }

    @DataProvider
    public static Object[][] notImplement_rules() {
        return $$(
                $(classes().should().notImplement(Collection.class), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(Collection.class)), List.class, ArrayList.class),
                $(classes().should().notImplement(Collection.class.getName()), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(Collection.class.getName())), List.class, ArrayList.class),
                $(classes().should().notImplement(name(Collection.class.getName()).as(Collection.class.getName())), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(name(Collection.class.getName()).as(Collection.class.getName()))),
                        List.class, ArrayList.class));
    }

    @Test
    @UseDataProvider("notImplement_rules")
    public void notImplement(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not implement %s", Collection.class.getName()))
                .contains(String.format("class %s implements %s", violated.getName(), Collection.class.getName()))
                .doesNotMatch(String.format(".*class %s .* implement.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] assignableTo_rules() {
        return $$(
                $(classes().should().beAssignableTo(Collection.class), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(Collection.class)), List.class, String.class),
                $(classes().should().beAssignableTo(Collection.class.getName()), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(Collection.class.getName())), List.class, String.class),
                $(classes().should().beAssignableTo(name(Collection.class.getName()).as(Collection.class.getName())), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(name(Collection.class.getName()).as(Collection.class.getName()))),
                        List.class, String.class));
    }

    @Test
    @UseDataProvider("assignableTo_rules")
    public void assignableTo(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be assignable to %s", Collection.class.getName()))
                .contains(String.format("class %s is not assignable to %s", violated.getName(), Collection.class.getName()))
                .doesNotMatch(String.format(".*class %s .* assignable.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notAssignableTo_rules() {
        return $$(
                $(classes().should().notBeAssignableTo(Collection.class), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(Collection.class)), String.class, List.class),
                $(classes().should().notBeAssignableTo(Collection.class.getName()), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(Collection.class.getName())), String.class, List.class),
                $(classes().should().notBeAssignableTo(name(Collection.class.getName()).as(Collection.class.getName())), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(name(Collection.class.getName()).as(Collection.class.getName()))),
                        String.class, List.class));
    }

    @Test
    @UseDataProvider("notAssignableTo_rules")
    public void notAssignableTo(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be assignable to %s", Collection.class.getName()))
                .contains(String.format("class %s is assignable to %s", violated.getName(), Collection.class.getName()))
                .doesNotMatch(String.format(".*class %s .* assignable.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] assignableFrom_rules() {
        return $$(
                $(classes().should().beAssignableFrom(List.class), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(List.class)), Collection.class, String.class),
                $(classes().should().beAssignableFrom(List.class.getName()), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(List.class.getName())), Collection.class, String.class),
                $(classes().should().beAssignableFrom(name(List.class.getName()).as(List.class.getName())), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(name(List.class.getName()).as(List.class.getName()))),
                        Collection.class, String.class));
    }

    @Test
    @UseDataProvider("assignableFrom_rules")
    public void assignableFrom(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be assignable from %s", List.class.getName()))
                .contains(String.format("class %s is not assignable from %s", violated.getName(), List.class.getName()))
                .doesNotMatch(String.format(".*class %s .* assignable.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notAssignableFrom_rules() {
        return $$(
                $(classes().should().notBeAssignableFrom(List.class), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(List.class)), String.class, Collection.class),
                $(classes().should().notBeAssignableFrom(List.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(List.class.getName())), String.class, Collection.class),
                $(classes().should().notBeAssignableFrom(name(List.class.getName()).as(List.class.getName())), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(name(List.class.getName()).as(List.class.getName()))),
                        String.class, Collection.class));
    }

    @Test
    @UseDataProvider("notAssignableFrom_rules")
    public void notAssignableFrom(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be assignable from %s", List.class.getName()))
                .contains(String.format("class %s is assignable from %s", violated.getName(), List.class.getName()))
                .doesNotMatch(String.format(".*class %s .* assignable.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] accessField_rules() {
        return $$(
                $(classes().should().getField(ClassWithField.class, "field"), "get", "gets"),
                $(classes().should(ArchConditions.getField(ClassWithField.class, "field")), "get", "gets"),
                $(classes().should().getField(ClassWithField.class.getName(), "field"), "get", "gets"),
                $(classes().should(ArchConditions.getField(ClassWithField.class.getName(), "field")), "get", "gets"),
                $(classes().should().setField(ClassWithField.class, "field"), "set", "sets"),
                $(classes().should(ArchConditions.setField(ClassWithField.class, "field")), "set", "sets"),
                $(classes().should().setField(ClassWithField.class.getName(), "field"), "set", "sets"),
                $(classes().should(ArchConditions.setField(ClassWithField.class.getName(), "field")), "set", "sets"),
                $(classes().should().accessField(ClassWithField.class, "field"), "access", "accesses"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class, "field")), "access", "accesses"),
                $(classes().should().accessField(ClassWithField.class.getName(), "field"), "access", "accesses"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class.getName(), "field")), "access", "accesses")
        );
    }

    @Test
    @UseDataProvider("accessField_rules")
    public void accessField(ArchRule rule, String accessTypePlural, String accessTypeSingular) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %s field %s.%s",
                        accessTypePlural, ClassWithField.class.getSimpleName(), "field"))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingular,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingular,
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] accessFieldWhere_rules() {
        return $$(
                $(classes().should().getFieldWhere(accessTargetIs(ClassWithField.class)), "get", "gets"),
                $(classes().should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))), "get", "gets"),
                $(classes().should().setFieldWhere(accessTargetIs(ClassWithField.class)), "set", "sets"),
                $(classes().should(ArchConditions.setFieldWhere(accessTargetIs(ClassWithField.class))), "set", "sets"),
                $(classes().should().accessFieldWhere(accessTargetIs(ClassWithField.class)), "access", "accesses"),
                $(classes().should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))), "access", "accesses")
        );
    }

    @Test
    @UseDataProvider("accessFieldWhere_rules")
    public void accessFieldWhere(ArchRule rule, String accessTypePlural, String accessTypeSingular) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %s field where target is %s",
                        accessTypePlural, ClassWithField.class.getSimpleName()))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingular,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingular,
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] callMethod_rules() {
        return $$(
                $(classes().should().callMethod(ClassWithMethod.class, "method", String.class)),
                $(classes().should(ArchConditions.callMethod(ClassWithMethod.class, "method", String.class))),
                $(classes().should().callMethod(ClassWithMethod.class.getName(), "method", String.class.getName())),
                $(classes().should(ArchConditions.callMethod(ClassWithMethod.class.getName(), "method", String.class.getName())))
        );
    }

    @Test
    @UseDataProvider("callMethod_rules")
    public void callMethod(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call method %s.%s(%s)",
                        ClassWithMethod.class.getSimpleName(), "method", String.class.getSimpleName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .doesNotMatch(callMethodRegex(
                        ClassCallingMethod.class,
                        ClassWithMethod.class, "method", String.class));
    }

    @DataProvider
    public static Object[][] callMethodWhere_rules() {
        return $$(
                $(classes().should().callMethodWhere(callTargetIs(ClassWithMethod.class))),
                $(classes().should(ArchConditions.callMethodWhere(callTargetIs(ClassWithMethod.class))))
        );
    }

    @Test
    @UseDataProvider("callMethodWhere_rules")
    public void callMethodWhere(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call method where target is %s",
                        ClassWithMethod.class.getSimpleName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .doesNotMatch(callMethodRegex(
                        ClassCallingMethod.class,
                        ClassWithMethod.class, "method", String.class));
    }

    @DataProvider
    public static Object[][] callConstructor_rules() {
        return $$(
                $(classes().should().callConstructor(ClassWithConstructor.class, String.class)),
                $(classes().should(ArchConditions.callConstructor(ClassWithConstructor.class, String.class))),
                $(classes().should().callConstructor(ClassWithConstructor.class.getName(), String.class.getName())),
                $(classes().should(ArchConditions.callConstructor(ClassWithConstructor.class.getName(), String.class.getName())))
        );
    }

    @Test
    @UseDataProvider("callConstructor_rules")
    public void callConstructor(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithConstructor.class, ClassCallingConstructor.class, ClassCallingWrongConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call constructor %s.<init>(%s)",
                        ClassWithConstructor.class.getSimpleName(), String.class.getSimpleName()))
                .containsPattern(callConstructorRegex(
                        ClassCallingWrongConstructor.class,
                        ClassCallingConstructor.class, int.class, Date.class))
                .doesNotMatch(callConstructorRegex(
                        ClassCallingConstructor.class,
                        ClassWithConstructor.class, String.class));
    }

    @DataProvider
    public static Object[][] callConstructorWhere_rules() {
        return $$(
                $(classes().should().callConstructorWhere(callTargetIs(ClassWithConstructor.class))),
                $(classes().should(ArchConditions.callConstructorWhere(callTargetIs(ClassWithConstructor.class))))
        );
    }

    @Test
    @UseDataProvider("callConstructorWhere_rules")
    public void callConstructorWhere(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithConstructor.class, ClassCallingConstructor.class, ClassCallingWrongConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call constructor where target is %s",
                        ClassWithConstructor.class.getSimpleName()))
                .containsPattern(callConstructorRegex(
                        ClassCallingWrongConstructor.class,
                        ClassCallingConstructor.class, int.class, Date.class))
                .doesNotMatch(callConstructorRegex(
                        ClassCallingConstructor.class,
                        ClassWithConstructor.class, String.class));
    }

    @DataProvider
    public static Object[][] accessTargetWhere_rules() {
        return $$(
                $(classes().should().accessTargetWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))),
                $(classes().should(ArchConditions.accessTargetWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))))
        );
    }

    @Test
    @UseDataProvider("accessTargetWhere_rules")
    public void accessTargetWhere(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithFieldMethodAndConstructor.class, ClassAccessingFieldMethodAndConstructor.class,
                ClassAccessingWrongFieldMethodAndConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should access target where target is %s",
                        ClassWithFieldMethodAndConstructor.class.getSimpleName()))
                .containsPattern(accessTargetRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
                .containsPattern(accessTargetRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, CONSTRUCTOR_NAME))
                .containsPattern(accessTargetRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "call"))
                .doesNotMatch(accessTargetRegex(
                        ClassAccessingFieldMethodAndConstructor.class,
                        ClassWithFieldMethodAndConstructor.class, ""));
    }

    @DataProvider
    public static Object[][] callCodeUnitWhere_rules() {
        return $$(
                $(classes().should().callCodeUnitWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))),
                $(classes().should(ArchConditions.callCodeUnitWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))))
        );
    }

    @Test
    @UseDataProvider("callCodeUnitWhere_rules")
    public void callCodeUnitWhere(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithFieldMethodAndConstructor.class, ClassAccessingFieldMethodAndConstructor.class,
                ClassAccessingWrongFieldMethodAndConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call code unit where target is %s",
                        ClassWithFieldMethodAndConstructor.class.getSimpleName()))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, CONSTRUCTOR_NAME, int.class, Date.class))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "call"))
                .doesNotMatch(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
                .doesNotMatch(callCodeUnitRegex(
                        ClassAccessingFieldMethodAndConstructor.class,
                        ClassWithFieldMethodAndConstructor.class, ""));
    }

    @DataProvider
    public static Object[][] beInterfaces_rules() {
        return $$(
                $(classes().should().beInterfaces(), Collection.class, String.class),
                $(classes().should(ArchConditions.beInterfaces()), Collection.class, String.class));
    }

    @Test
    @UseDataProvider("beInterfaces_rules")
    public void beInterfaces(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be interfaces")
                .contains(String.format("class %s is not an interface", violated.getName()))
                .doesNotMatch(String.format(".*class %s .* interface.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeInterfaces_rules() {
        return $$(
                $(classes().should().notBeInterfaces(), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeInterfaces()), String.class, Collection.class));
    }

    @Test
    @UseDataProvider("notBeInterfaces_rules")
    public void notBeInterfaces(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be interfaces")
                .contains(String.format("class %s is an interface", violated.getName()))
                .doesNotMatch(String.format(".*class %s .* interface.*", quote(satisfied.getName())));
    }

    private String singleLineFailureReportOf(EvaluationResult result) {
        return result.getFailureReport().toString().replaceAll("\\r?\\n", FAILURE_REPORT_NEWLINE_MARKER);
    }

    private static DescribedPredicate<JavaAnnotation> annotation(final Class<? extends Annotation> type) {
        return new DescribedPredicate<JavaAnnotation>("@" + type.getSimpleName()) {
            @Override
            public boolean apply(JavaAnnotation input) {
                return input.getType().getName().equals(type.getName());
            }
        };
    }

    private static String containsPartOfRegex(String fullString) {
        return String.format(".*%s.*", fullString.substring(1, fullString.length() - 1));
    }

    private String doesntResideInAPackageMessageFor(Class<?> clazz, String packageIdentifier) {
        return String.format("%s doesn't reside in a package '%s'",
                clazz.getName(), packageIdentifier);
    }

    private String doesntResideOutsideOfPackageMessageFor(Class<?> clazz, String packageIdentifier) {
        return String.format("%s doesn't reside outside of package '%s'",
                clazz.getName(), packageIdentifier);
    }

    private String doesntResideInAnyPackageMessageFor(Class<?> clazz, String[] packageIdentifiers) {
        return String.format("%s doesn't reside in any package ['%s']",
                clazz.getName(), Joiner.on("', '").join(packageIdentifiers));
    }

    private String doesntResideOutsideOfPackagesMessageFor(Class<?> clazz, String[] packageIdentifiers) {
        return String.format("%s doesn't reside outside of packages ['%s']",
                clazz.getName(), Joiner.on("', '").join(packageIdentifiers));
    }

    private static DescribedPredicate<JavaCall<?>> callTargetIs(Class<?> type) {
        return JavaCall.Predicates.target(owner(type(type))).as("target is " + type.getSimpleName());
    }

    private static DescribedPredicate<JavaAccess<?>> accessTargetIs(Class<?> type) {
        return JavaAccess.Predicates.target(owner(type(type))).as("target is " + type.getSimpleName());
    }

    private void checkTestStillValid(String[] packages,
            Set<Class<?>> inSomePackage,
            Set<Class<?>> notInAnyPackage) {
        for (Class<?> c : inSomePackage) {
            assertThat(packageMatches(c, packages)).as("Package matches").isTrue();
        }
        for (Class<?> c : notInAnyPackage) {
            assertThat(packageMatches(c, packages)).as("Package matches").isFalse();
        }
    }

    private boolean packageMatches(Class<?> c, String[] packages) {
        for (String p : packages) {
            if (c.getPackage().getName().equals(p)) {
                return true;
            }
        }
        return false;
    }

    private void checkTestStillValid(String thePackage,
            Set<Class<?>> inPackage,
            Set<Class<?>> inSuperPackage,
            Set<Class<?>> inSubPackage) {
        for (Class<?> c : inPackage) {
            assertThat(c.getPackage().getName()).isEqualTo(thePackage);
        }
        for (Class<?> c : inSuperPackage) {
            assertThat(thePackage).startsWith(c.getPackage().getName());
        }
        for (Class<?> c : inSubPackage) {
            assertThat(c.getPackage().getName()).startsWith(thePackage);
        }
    }

    private Pattern accessesFieldRegex(Class<?> origin, String accessType, Class<?> targetClass, String fieldName) {
        String originAccesses = String.format("%s[^%s]* %s", quote(origin.getName()), FAILURE_REPORT_NEWLINE_MARKER, accessType);
        String target = String.format("[^%s]*%s\\.%s", FAILURE_REPORT_NEWLINE_MARKER, quote(targetClass.getName()), fieldName);
        return Pattern.compile(String.format(".*%s field %s.*", originAccesses, target));
    }

    private Pattern callConstructorRegex(Class<?> origin, Class<?> targetClass, Class<?>... paramTypes) {
        return callRegex(origin, targetClass, "constructor", CONSTRUCTOR_NAME, paramTypes);
    }

    private Pattern callMethodRegex(Class<?> origin, Class<?> targetClass, String methodName, Class<?>... paramTypes) {
        return callRegex(origin, targetClass, "method", methodName, paramTypes);
    }

    private Pattern callCodeUnitRegex(Class<?> origin, Class<?> targetClass, String methodName, Class<?>... paramTypes) {
        return callRegex(origin, targetClass, "(method|constructor)", methodName, paramTypes);
    }

    private Pattern callRegex(Class<?> origin, Class<?> targetClass, String targetType, String methodName, Class<?>... paramTypes) {
        String params = Joiner.on(", ").join(JavaClass.namesOf(paramTypes));
        String originCalls = String.format("%s[^%s]* calls", quote(origin.getName()), FAILURE_REPORT_NEWLINE_MARKER);
        String target = String.format("[^%s]*%s\\.%s\\(%s\\)",
                FAILURE_REPORT_NEWLINE_MARKER, quote(targetClass.getName()), methodName, quote(params));
        return Pattern.compile(String.format(".*%s %s %s.*", originCalls, targetType, target));
    }

    private Pattern accessTargetRegex(Class<?> origin, Class<?> targetClass, String memberName) {
        String originAccesses = String.format("%s[^%s]* (accesses|calls|sets|gets)",
                quote(origin.getName()), FAILURE_REPORT_NEWLINE_MARKER);
        String target = String.format("[^%s]*%s\\.%s",
                FAILURE_REPORT_NEWLINE_MARKER, quote(targetClass.getName()), memberName);
        return Pattern.compile(String.format(".*%s (field|method|constructor) %s.*", originAccesses, target));
    }

    private static class RightNamedClass {
    }

    private static class WrongNamedClass {
    }

    private static class ClassWithField {
        String field;
    }

    private static class ClassAccessingField {
        ClassWithField classWithField;

        String access() {
            classWithField.field = "new";
            return classWithField.field;
        }
    }

    private static class ClassAccessingWrongField {
        ClassAccessingField classAccessingField;

        ClassWithField wrongAccess() {
            classAccessingField.classWithField = null;
            return classAccessingField.classWithField;
        }
    }

    private static class ClassWithMethod {
        void method(String param) {
        }
    }

    private static class ClassCallingMethod {
        ClassWithMethod classWithMethod;

        void call() {
            classWithMethod.method("param");
        }
    }

    private static class ClassCallingWrongMethod {
        ClassCallingMethod classCallingMethod;

        void callWrong() {
            classCallingMethod.call();
        }
    }

    private static class ClassWithConstructor {
        ClassWithConstructor(String param) {
        }
    }

    private static class ClassCallingConstructor {
        ClassCallingConstructor(int number, Date date) {
        }

        void call() {
            new ClassWithConstructor("param");
        }
    }

    private static class ClassCallingWrongConstructor {
        void callWrong() {
            new ClassCallingConstructor(0, null);
        }
    }

    private static class ClassWithFieldMethodAndConstructor {
        String field;

        ClassWithFieldMethodAndConstructor(String param) {
        }

        void method(String param) {
        }
    }

    private static class ClassAccessingFieldMethodAndConstructor {
        String wrongField;

        ClassAccessingFieldMethodAndConstructor(int number, Date date) {
        }

        void call() {
            ClassWithFieldMethodAndConstructor instance = new ClassWithFieldMethodAndConstructor("param");
            instance.field = "field";
            instance.method("param");
        }
    }

    private static class ClassAccessingWrongFieldMethodAndConstructor {
        void callWrong() {
            ClassAccessingFieldMethodAndConstructor instance = new ClassAccessingFieldMethodAndConstructor(0, null);
            instance.wrongField = "field";
            instance.call();
        }
    }

    public static class PublicClass {
    }

    protected static class ProtectedClass {
    }

    static class PackagePrivateClass {
    }

    private static class PrivateClass {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class SomeAnnotatedClass {
    }
}
