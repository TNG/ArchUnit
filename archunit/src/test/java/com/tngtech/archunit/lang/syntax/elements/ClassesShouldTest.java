package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.file.StandardCopyOption;
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
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.ClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.DefaultClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.SourceRetentionAnnotation;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testclasses.SomeClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.WrongNamedClass;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.greaterThan;
import static com.tngtech.archunit.base.DescribedPredicate.greaterThanOrEqualTo;
import static com.tngtech.archunit.base.DescribedPredicate.lessThan;
import static com.tngtech.archunit.base.DescribedPredicate.lessThanOrEqualTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaClassTest.expectInvalidSyntaxUsageForClassInsteadOfInterface;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importHierarchies;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.expectInvalidSyntaxUsageForRetentionSource;
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
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldTest {
    static final String FAILURE_REPORT_NEWLINE_MARKER = "#";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @DataProvider
    public static Object[][] haveFullyQualifiedName_rules() {
        return $$(
                $(classes().should().haveFullyQualifiedName(SomeClass.class.getName())),
                $(classes().should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @Test
    @UseDataProvider("haveFullyQualifiedName_rules")
    public void haveFullyQualifiedName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have fully qualified name '%s'", SomeClass.class.getName()))
                .containsPattern(String.format("Class <%s> does not have fully qualified name '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(SomeClass.class.getName()),
                        locationPattern(WrongNamedClass.class)))
                .doesNotMatch(String.format(".*<%s>.*name.*", quote(SomeClass.class.getName())));
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
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not have fully qualified name '%s'", WrongNamedClass.class.getName()))
                .contains(String.format("Class <%s> has fully qualified name '%s'", WrongNamedClass.class.getName(), WrongNamedClass.class.getName()))
                .doesNotContain(String.format("<%s>.*name", SomeClass.class.getName()));
    }

    @DataProvider
    public static Object[][] haveSimpleName_rules() {
        return $$(
                $(classes().should().haveSimpleName(SomeClass.class.getSimpleName())),
                $(classes().should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @Test
    @UseDataProvider("haveSimpleName_rules")
    public void haveSimpleName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name '%s'", SomeClass.class.getSimpleName()))
                .containsPattern(String.format("Class <%s> does not have simple name '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(SomeClass.class.getSimpleName()),
                        locationPattern(WrongNamedClass.class)))
                .doesNotMatch(String.format(".*<%s>.*simple name.*", SomeClass.class.getSimpleName()));
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
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not have simple name '%s'", WrongNamedClass.class.getSimpleName()))
                .containsPattern(String.format("Class <%s> has simple name '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(WrongNamedClass.class.getSimpleName()),
                        locationPattern(WrongNamedClass.class)))
                .doesNotMatch(String.format(".*<%s>.*simple name.*", SomeClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return $$(
                $(classes().should().haveNameMatching(regex), regex),
                $(classes().should(ArchConditions.haveNameMatching(regex)), regex)
        );
    }

    @Test
    @UseDataProvider("haveNameMatching_rules")
    public void haveNameMatching(ArchRule rule, String regex) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name matching '%s'", regex))
                .containsPattern(String.format("Class <%s> does not match '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(regex),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(String.format("%s", SomeClass.class.getSimpleName()));
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
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name not matching '%s'", regex))
                .containsPattern(String.format("Class <%s> matches '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(regex),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(String.format("%s", SomeClass.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] haveSimpleNameStartingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return $$(
                $(classes().should().haveSimpleNameStartingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleNameStartingWith(prefix)), prefix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameStartingWith_rules")
    public void haveSimpleNameStartingWith(ArchRule rule, String prefix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name starting with '%s'", prefix))
                .containsPattern(String.format("simple name of %s does not start with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(prefix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleNameContaining_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return $$(
                $(classes().should().haveSimpleNameContaining(infix), infix),
                $(classes().should(ArchConditions.haveSimpleNameContaining(infix)), infix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameContaining_rules")
    public void haveSimpleNameContaining(ArchRule rule, String infix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name containing '%s'", infix))
                .containsPattern(String.format("simple name of %s does not contain '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(infix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleNameNotContaining_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return $$(
                $(classes().should().haveSimpleNameNotContaining(infix), infix),
                $(classes().should(ArchConditions.haveSimpleNameNotContaining(infix)), infix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameNotContaining_rules")
    public void haveSimpleNameNotContaining(ArchRule rule, String infix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not containing '%s'", infix))
                .containsPattern(String.format("simple name of %s contains '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(infix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleNameNotStartingWith_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return $$(
                $(classes().should().haveSimpleNameNotStartingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleNameNotStartingWith(prefix)), prefix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameNotStartingWith_rules")
    public void haveSimpleNameNotStartingWith(ArchRule rule, String prefix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not starting with '%s'", prefix))
                .containsPattern(String.format("simple name of %s starts with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(prefix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleNameEndingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return $$(
                $(classes().should().haveSimpleNameEndingWith(suffix), suffix),
                $(classes().should(ArchConditions.haveSimpleNameEndingWith(suffix)), suffix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameEndingWith_rules")
    public void haveSimpleNameEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name ending with '%s'", suffix))
                .containsPattern(String.format("simple name of %s does not end with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(suffix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    @DataProvider
    public static Object[][] haveSimpleNameNotEndingWith_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return $$(
                $(classes().should().haveSimpleNameNotEndingWith(suffix), suffix),
                $(classes().should(ArchConditions.haveSimpleNameNotEndingWith(suffix)), suffix)
        );
    }

    @Test
    @UseDataProvider("haveSimpleNameNotEndingWith_rules")
    public void haveSimpleNameNotEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not ending with '%s'", suffix))
                .containsPattern(String.format("simple name of %s ends with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(suffix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
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
                .containsPattern(doesntResideInAPackagePatternFor(ArchConfiguration.class, packageIdentifier))
                .containsPattern(doesntResideInAPackagePatternFor(GivenObjects.class, packageIdentifier))
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
                .containsPattern(doesntResideInAnyPackagePatternFor(GivenObjects.class, packageIdentifiers))
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
                .containsPattern(doesntResideOutsideOfPackagePatternFor(ArchRule.class, packageIdentifier))
                .containsPattern(doesntResideOutsideOfPackagePatternFor(ArchCondition.class, packageIdentifier))
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
                .containsPattern(doesntResideOutsideOfPackagesPatternFor(ArchRule.class, packageIdentifiers))
                .containsPattern(doesntResideOutsideOfPackagesPatternFor(ArchCondition.class, packageIdentifiers))
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
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
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
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
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
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(PrivateClass.class.getName()), PRIVATE))
                .doesNotMatch(String.format(".*<%s>.* modifier.*", quote(PackagePrivateClass.class.getName())));
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
                .contains(String.format("Class <%s>", PackagePrivateClass.class.getName()))
                .contains("does not have modifier " + PUBLIC)
                .contains("does not have modifier " + PROTECTED)
                .contains("does not have modifier " + PRIVATE)
                .doesNotMatch(String.format(".*<%s>.* modifier.*", quote(PrivateClass.class.getName())));
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
                .containsPattern(String.format("Class <%s> .* modifier %s in %s",
                        quote(violated.getName()), modifier, locationPattern(getClass()))) // -> location == enclosingClass()
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    @DataProvider
    public static Object[][] annotated_rules() {
        return $$(
                $(classes().should().beAnnotatedWith(RuntimeRetentionAnnotation.class),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beAnnotatedWith(RuntimeRetentionAnnotation.class.getName()),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(RuntimeRetentionAnnotation.class.getName())),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beAnnotatedWith(annotation(RuntimeRetentionAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beAnnotatedWith(annotation(RuntimeRetentionAnnotation.class))),
                        SomeAnnotatedClass.class, String.class));
    }

    @Test
    @UseDataProvider("annotated_rules")
    public void annotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be annotated with @%s", RuntimeRetentionAnnotation.class.getSimpleName()))
                .containsPattern(String.format("Class <%s> is not annotated with @%s in %s",
                        quote(wrongClass.getName()),
                        RuntimeRetentionAnnotation.class.getSimpleName(),
                        locationPattern(String.class)))
                .doesNotMatch(String.format(".*<%s>.*annotated.*", quote(correctClass.getName())));
    }

    /**
     * Compare {@link CanBeAnnotatedTest#annotatedWith_Retention_Source_is_rejected}
     */
    @Test
    public void beAnnotatedWith_Retention_Source_is_rejected() {
        classes().should().beAnnotatedWith(RuntimeRetentionAnnotation.class);
        classes().should().beAnnotatedWith(ClassRetentionAnnotation.class);
        classes().should().beAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        classes().should().beAnnotatedWith(SourceRetentionAnnotation.class);
    }

    @DataProvider
    public static Object[][] notAnnotated_rules() {
        return $$(
                $(classes().should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class.getName()),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(RuntimeRetentionAnnotation.class.getName())),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeAnnotatedWith(annotation(RuntimeRetentionAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeAnnotatedWith(annotation(RuntimeRetentionAnnotation.class))),
                        String.class, SomeAnnotatedClass.class));
    }

    @Test
    @UseDataProvider("notAnnotated_rules")
    public void notAnnotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be annotated with @" + RuntimeRetentionAnnotation.class.getSimpleName())
                .containsPattern(String.format("Class <%s> is annotated with @%s in %s",
                        quote(wrongClass.getName()),
                        RuntimeRetentionAnnotation.class.getSimpleName(),
                        locationPattern(getClass())))
                .doesNotMatch(String.format(".*<%s>.*annotated.*", quote(correctClass.getName())));
    }

    /**
     * Compare {@link CanBeAnnotatedTest#annotatedWith_Retention_Source_is_rejected}
     */
    @Test
    public void notBeAnnotatedWith_Retention_Source_is_rejected() {
        classes().should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class);
        classes().should().notBeAnnotatedWith(ClassRetentionAnnotation.class);
        classes().should().notBeAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        classes().should().notBeAnnotatedWith(SourceRetentionAnnotation.class);
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
                .doesNotMatch(String.format(".*%s.* implement.*", quote(satisfied.getName())));
    }

    @Test
    public void implement_rejects_non_interface_types() {
        classes().should().implement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().should().implement(AbstractList.class);
    }

    @DataProvider
    public static List<List<?>> implement_not_satisfied_rules() {
        return ImmutableList.<List<?>>builder()
                .addAll(implementNotSatisfiedCases(Collection.class, List.class))
                .addAll(implementNotSatisfiedCases(Set.class, ArrayList.class))
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
                .containsPattern(String.format("Class <%s> does not implement %s in %s",
                        quote(violating.getName()),
                        quote(classToCheckAgainst.getName()),
                        locationPattern(violating)));
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
                .containsPattern(String.format("Class <%s> implements %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* implement.*", quote(satisfied.getName())));
    }

    @Test
    public void notImplement_rejects_non_interface_types() {
        classes().should().notImplement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().should().notImplement(AbstractList.class);
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
                .containsPattern(String.format("Class <%s> is not assignable to %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
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
                .containsPattern(String.format("Class <%s> is assignable to %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
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
                .containsPattern(String.format("Class <%s> is not assignable from %s in %s",
                        quote(violated.getName()),
                        quote(List.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
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
                .containsPattern(String.format("Class <%s> is assignable from %s in %s",
                        quote(violated.getName()),
                        quote(List.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
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
                $(classes().should().accessField(ClassWithField.class, "field"), "access", "(gets|sets)"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class, "field")), "access", "(gets|sets)"),
                $(classes().should().accessField(ClassWithField.class.getName(), "field"), "access", "(gets|sets)"),
                $(classes().should(ArchConditions.accessField(ClassWithField.class.getName(), "field")), "access", "(gets|sets)")
        );
    }

    @Test
    @UseDataProvider("accessField_rules")
    public void accessField(ArchRule rule, String accessTypePlural, String accessTypeSingularRegex) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %s field %s.%s",
                        accessTypePlural, ClassWithField.class.getSimpleName(), "field"))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingularRegex,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingularRegex,
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] accessFieldWhere_rules() {
        return $$(
                $(classes().should().getFieldWhere(accessTargetIs(ClassWithField.class)), "get", "gets"),
                $(classes().should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))), "get", "gets"),
                $(classes().should().setFieldWhere(accessTargetIs(ClassWithField.class)), "set", "sets"),
                $(classes().should(ArchConditions.setFieldWhere(accessTargetIs(ClassWithField.class))), "set", "sets"),
                $(classes().should().accessFieldWhere(accessTargetIs(ClassWithField.class)), "access", "(gets|sets)"),
                $(classes().should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))), "access", "(gets|sets)")
        );
    }

    @Test
    @UseDataProvider("accessFieldWhere_rules")
    public void accessFieldWhere(ArchRule rule, String accessTypePlural, String accessTypeSingularRegex) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %s field where target is %s",
                        accessTypePlural, ClassWithField.class.getSimpleName()))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, accessTypeSingularRegex,
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, accessTypeSingularRegex,
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] onlyAccessFieldsThat_rules() {
        return testForEach(
                classes().should().onlyAccessFieldsThat(are(declaredIn(ClassWithField.class))),
                classes().should(ArchConditions.onlyAccessFieldsThat(are(declaredIn(ClassWithField.class))))
        );
    }

    @Test
    @UseDataProvider("onlyAccessFieldsThat_rules")
    public void onlyAccessFieldsThat(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithField.class, ClassAccessingField.class, ClassAccessingWrongField.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should only access fields that are declared in " + ClassWithField.class.getName())
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongField.class, "(sets|gets|accesses)",
                        ClassAccessingField.class, "classWithField"))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingField.class, "(sets|gets|accesses)",
                        ClassAccessingField.class, "classWithField"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingField.class, "(sets|gets|accesses)",
                        ClassWithField.class, "field"))
                .doesNotMatch(accessesFieldRegex(
                        ClassWithField.class, "(sets|gets|accesses)",
                        ClassWithField.class, "field"));
    }

    @DataProvider
    public static Object[][] callMethod_rules() {
        return testForEach(
                classes().should().callMethod(ClassWithMethod.class, "method", String.class),
                classes().should(ArchConditions.callMethod(ClassWithMethod.class, "method", String.class)),
                classes().should().callMethod(ClassWithMethod.class.getName(), "method", String.class.getName()),
                classes().should(ArchConditions.callMethod(ClassWithMethod.class.getName(), "method", String.class.getName()))
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
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class, ClassCallingSelf.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should call method where target is %s",
                        ClassWithMethod.class.getSimpleName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .containsPattern(callMethodRegex(
                        ClassCallingSelf.class,
                        ClassCallingSelf.class, "target"))
                .doesNotMatch(callMethodRegex(
                        ClassCallingMethod.class,
                        ClassWithMethod.class, "method", String.class));
    }

    @DataProvider
    public static Object[][] onlyCallMethodsThat_rules() {
        return $$(
                $(classes().should().onlyCallMethodsThat(are(declaredIn(ClassWithMethod.class)))),
                $(classes().should(ArchConditions.onlyCallMethodsThat(are(declaredIn(ClassWithMethod.class)))))
        );
    }

    @Test
    @UseDataProvider("onlyCallMethodsThat_rules")
    public void onlyCallMethodsThat(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithMethod.class, ClassCallingMethod.class, ClassCallingWrongMethod.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should only call methods that are declared in %s",
                        ClassWithMethod.class.getName()))
                .containsPattern(callMethodRegex(
                        ClassCallingWrongMethod.class,
                        ClassCallingMethod.class, "call"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class, "sets",
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
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
    public static Object[][] onlyCallConstructorsThat_rules() {
        return $$(
                $(classes().should().onlyCallConstructorsThat(are(declaredIn(ClassWithConstructor.class)))),
                $(classes().should(ArchConditions.onlyCallConstructorsThat(are(declaredIn(ClassWithConstructor.class)))))
        );
    }

    @Test
    @UseDataProvider("onlyCallConstructorsThat_rules")
    public void onlyCallConstructorsThat(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithConstructor.class, ClassCallingConstructor.class, ClassCallingWrongConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should only call constructors that are declared in %s",
                        ClassWithConstructor.class.getName()))
                .containsPattern(callConstructorRegex(
                        ClassCallingWrongConstructor.class,
                        ClassCallingConstructor.class, int.class, Date.class))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class, "sets",
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
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
    public static Object[][] onlyCallCodeUnitsThat_rules() {
        return $$(
                $(classes().should().onlyCallCodeUnitsThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))),
                $(classes().should(ArchConditions.onlyCallCodeUnitsThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))))
        );
    }

    @Test
    @UseDataProvider("onlyCallCodeUnitsThat_rules")
    public void onlyCallCodeUnitsThat(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithFieldMethodAndConstructor.class, ClassAccessingFieldMethodAndConstructor.class,
                ClassAccessingWrongFieldMethodAndConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should only call code units that are declared in %s",
                        ClassWithFieldMethodAndConstructor.class.getName()))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, CONSTRUCTOR_NAME, int.class, Date.class))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "call"))
                .doesNotMatch(accessesFieldRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class, "sets",
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
                .doesNotMatch(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
                .doesNotMatch(callCodeUnitRegex(
                        ClassAccessingFieldMethodAndConstructor.class,
                        ClassWithFieldMethodAndConstructor.class, ""));
    }

    @DataProvider
    public static Object[][] onlyAccessMembersThat_rules() {
        return $$(
                $(classes().should().onlyAccessMembersThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))),
                $(classes().should(ArchConditions.onlyAccessMembersThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))))
        );
    }

    @Test
    @UseDataProvider("onlyAccessMembersThat_rules")
    public void onlyAccessMembersThat(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                ClassWithFieldMethodAndConstructor.class, ClassAccessingFieldMethodAndConstructor.class,
                ClassAccessingWrongFieldMethodAndConstructor.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should only access members that are declared in %s",
                        ClassWithFieldMethodAndConstructor.class.getName()))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, CONSTRUCTOR_NAME, int.class, Date.class))
                .containsPattern(callCodeUnitRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class,
                        ClassAccessingFieldMethodAndConstructor.class, "call"))
                .containsPattern(accessesFieldRegex(
                        ClassAccessingWrongFieldMethodAndConstructor.class, "sets",
                        ClassAccessingFieldMethodAndConstructor.class, "wrongField"))
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
                .containsPattern(String.format("Class <%s> is not an interface in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* interface.*", quote(satisfied.getName())));
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
                .containsPattern(String.format("Class <%s> is an interface in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* interface.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beEnums_rules() {
        return $$(
                $(classes().should().beEnums(), StandardCopyOption.class, String.class),
                $(classes().should(ArchConditions.beEnums()), StandardCopyOption.class, String.class));
    }

    @Test
    @UseDataProvider("beEnums_rules")
    public void beEnums(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be enums")
                .containsPattern(String.format("Class <%s> is not an enum in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* enum.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeEnums_rules() {
        return $$(
                $(classes().should().notBeEnums(), String.class, StandardCopyOption.class),
                $(classes().should(ArchConditions.notBeEnums()), String.class, StandardCopyOption.class));
    }

    @Test
    @UseDataProvider("notBeEnums_rules")
    public void notBeEnums(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be enums")
                .containsPattern(String.format("Class <%s> is an enum in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* enum.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beTopLevelClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().beTopLevelClasses(), topLevelClass, staticNestedClass),
                $(classes().should(ArchConditions.beTopLevelClasses()), topLevelClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("beTopLevelClasses_rules")
    public void beTopLevelClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be top level classes")
                .containsPattern(String.format("Class <%s> is not a top level class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* top level class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeTopLevelClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().notBeTopLevelClasses(), staticNestedClass, topLevelClass),
                $(classes().should(ArchConditions.notBeTopLevelClasses()), staticNestedClass, topLevelClass)
        );
    }

    @Test
    @UseDataProvider("notBeTopLevelClasses_rules")
    public void notBeTopLevelClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be top level classes")
                .containsPattern(String.format("Class <%s> is a top level class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* top level class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beNestedClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().beNestedClasses(), staticNestedClass, topLevelClass),
                $(classes().should(ArchConditions.beNestedClasses()), staticNestedClass, topLevelClass)
        );
    }

    @Test
    @UseDataProvider("beNestedClasses_rules")
    public void beNestedClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be nested classes")
                .containsPattern(String.format("Class <%s> is not a nested class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* nested class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeNestedClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().notBeNestedClasses(), topLevelClass, staticNestedClass),
                $(classes().should(ArchConditions.notBeNestedClasses()), topLevelClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("notBeNestedClasses_rules")
    public void notBeNestedClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be nested classes")
                .containsPattern(String.format("Class <%s> is a nested class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* nested class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beMemberClasses_rules() {
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();

        return $$(
                $(classes().should().beMemberClasses(), staticNestedClass, anonymousClass),
                $(classes().should(ArchConditions.beMemberClasses()), staticNestedClass, anonymousClass)
        );
    }

    @Test
    @UseDataProvider("beMemberClasses_rules")
    public void beMemberClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be member classes")
                .containsPattern(String.format("Class <%s> is not a member class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* member class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeMemberClasses_rules() {
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();

        return $$(
                $(classes().should().notBeMemberClasses(), anonymousClass, staticNestedClass),
                $(classes().should(ArchConditions.notBeMemberClasses()), anonymousClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("notBeMemberClasses_rules")
    public void notBeMemberClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be member classes")
                .containsPattern(String.format("Class <%s> is a member class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* member class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beInnerClasses_rules() {
        Class<?> innerMemberClass = NestedClassWithSomeMoreClasses.InnerMemberClass.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().beInnerClasses(), innerMemberClass, staticNestedClass),
                $(classes().should(ArchConditions.beInnerClasses()), innerMemberClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("beInnerClasses_rules")
    public void beInnerClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be inner classes")
                .containsPattern(String.format("Class <%s> is not an inner class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* inner class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeInnerClasses_rules() {
        Class<?> nonStaticNestedClass = NestedClassWithSomeMoreClasses.InnerMemberClass.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().notBeInnerClasses(), staticNestedClass, nonStaticNestedClass),
                $(classes().should(ArchConditions.notBeInnerClasses()), staticNestedClass, nonStaticNestedClass)
        );
    }

    @Test
    @UseDataProvider("notBeInnerClasses_rules")
    public void notBeInnerClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be inner classes")
                .containsPattern(String.format("Class <%s> is an inner class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* inner class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beAnonymousClasses_rules() {
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().beAnonymousClasses(), anonymousClass, staticNestedClass),
                $(classes().should(ArchConditions.beAnonymousClasses()), anonymousClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("beAnonymousClasses_rules")
    public void beAnonymousClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be anonymous classes")
                .containsPattern(String.format("Class <%s> is not an anonymous class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* anonymous class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeAnonymousClasses_rules() {
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().notBeAnonymousClasses(), staticNestedClass, anonymousClass),
                $(classes().should(ArchConditions.notBeAnonymousClasses()), staticNestedClass, anonymousClass)
        );
    }

    @Test
    @UseDataProvider("notBeAnonymousClasses_rules")
    public void notBeAnonymousClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be anonymous classes")
                .containsPattern(String.format("Class <%s> is an anonymous class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* anonymous class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] beLocalClasses_rules() {
        Class<?> localClass = NestedClassWithSomeMoreClasses.getLocalClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().beLocalClasses(), localClass, staticNestedClass),
                $(classes().should(ArchConditions.beLocalClasses()), localClass, staticNestedClass)
        );
    }

    @Test
    @UseDataProvider("beLocalClasses_rules")
    public void beLocalClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be local classes")
                .containsPattern(String.format("Class <%s> is not a local class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* local class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeLocalClasses_rules() {
        Class<?> localClass = NestedClassWithSomeMoreClasses.getLocalClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return $$(
                $(classes().should().notBeLocalClasses(), staticNestedClass, localClass),
                $(classes().should(ArchConditions.notBeLocalClasses()), staticNestedClass, localClass)
        );
    }

    @Test
    @UseDataProvider("notBeLocalClasses_rules")
    public void notBeLocalClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be local classes")
                .containsPattern(String.format("Class <%s> is a local class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* local class.*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] containNumberOfElements_rules() {
        return $$(
                $(equalTo(999)),
                $(lessThan(0)),
                $(lessThan(1)),
                $(lessThan(2)),
                $(greaterThan(2)),
                $(greaterThan(3)),
                $(greaterThan(999)),
                $(lessThanOrEqualTo(0)),
                $(lessThanOrEqualTo(1)),
                $(greaterThanOrEqualTo(3)),
                $(greaterThanOrEqualTo(999)));
    }

    @Test
    @UseDataProvider("containNumberOfElements_rules")
    public void containNumberOfElements(DescribedPredicate<Integer> predicate) {
        EvaluationResult result = classes().should().containNumberOfElements(predicate).evaluate(importClasses(String.class, Integer.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("contain number of elements " + predicate.getDescription())
                .contains("there is/are 2 element(s) in classes [java.lang.Integer, java.lang.String]");
    }

    @DataProvider
    public static Object[][] beClass_rules() {
        return $$(
                $(classes().should().be(String.class), String.class, Collection.class),
                $(classes().should().be(String.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.be(String.class)), String.class, Collection.class),
                $(classes().should(ArchConditions.be(String.class.getName())), String.class, Collection.class));
    }

    @Test
    @UseDataProvider("beClass_rules")
    public void beClass(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be " + satisfied.getName())
                .containsPattern(String.format("Class <%s> is not %s in %s",
                        quote(violated.getName()),
                        quote(satisfied.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*<%s>.* is .*", quote(satisfied.getName())));
    }

    @DataProvider
    public static Object[][] notBeClass_rules() {
        return $$(
                $(classes().should().notBe(Collection.class), String.class, Collection.class),
                $(classes().should().notBe(Collection.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.notBe(Collection.class)), String.class, Collection.class),
                $(classes().should(ArchConditions.notBe(Collection.class.getName())), String.class, Collection.class));
    }

    @Test
    @UseDataProvider("notBeClass_rules")
    public void notBeClass(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be " + violated.getName())
                .containsPattern(String.format("Class <%s> is %s in %s",
                        quote(violated.getName()),
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* is .*", quote(satisfied.getName())));
    }

    static String locationPattern(Class<?> clazz) {
        return String.format("\\(%s.java:0\\)", quote(clazz.getSimpleName()));
    }

    static String singleLineFailureReportOf(EvaluationResult result) {
        return result.getFailureReport().toString().replaceAll("\\r?\\n", FAILURE_REPORT_NEWLINE_MARKER);
    }

    @SuppressWarnings("SameParameterValue")
    private static DescribedPredicate<JavaAnnotation<?>> annotation(final Class<? extends Annotation> type) {
        return new DescribedPredicate<JavaAnnotation<?>>("@" + type.getSimpleName()) {
            @Override
            public boolean apply(JavaAnnotation<?> input) {
                return input.getRawType().getName().equals(type.getName());
            }
        };
    }

    static String containsPartOfRegex(String fullString) {
        return String.format(".*%s.*", fullString.substring(1, fullString.length() - 1));
    }

    private String doesntResideInAPackagePatternFor(Class<?> clazz, String packageIdentifier) {
        return String.format("Class <%s> does not reside in a package '%s' in %s",
                quote(clazz.getName()), quote(packageIdentifier), locationPattern(clazz));
    }

    private String doesntResideOutsideOfPackagePatternFor(Class<?> clazz, String packageIdentifier) {
        return String.format("Class <%s> does not reside outside of package '%s' in %s",
                quote(clazz.getName()), quote(packageIdentifier), locationPattern(clazz));
    }

    @SuppressWarnings("SameParameterValue")
    private String doesntResideInAnyPackagePatternFor(Class<?> clazz, String[] packageIdentifiers) {
        return String.format("Class <%s> does not reside in any package \\['%s'\\] in %s",
                quote(clazz.getName()), quote(Joiner.on("', '").join(packageIdentifiers)), locationPattern(clazz));
    }

    private String doesntResideOutsideOfPackagesPatternFor(Class<?> clazz, String[] packageIdentifiers) {
        return String.format("Class <%s> does not reside outside of packages \\['%s'\\] in %s",
                quote(clazz.getName()), quote(Joiner.on("', '").join(packageIdentifiers)), locationPattern(clazz));
    }

    private static DescribedPredicate<JavaCall<?>> callTargetIs(Class<?> type) {
        return JavaCall.Predicates.target(owner(type(type))).as("target is " + type.getSimpleName());
    }

    static DescribedPredicate<JavaAccess<?>> accessTargetIs(Class<?> type) {
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

    static Pattern accessesFieldRegex(Class<?> origin, String accessType, Class<?> targetClass, String fieldName) {
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

    @SuppressWarnings({"ConstantConditions", "unused"})
    private static class ClassAccessingWrongField {
        ClassAccessingField classAccessingField;

        ClassWithField wrongAccess() {
            classAccessingField.classWithField = null;
            return classAccessingField.classWithField;
        }
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
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

    @SuppressWarnings("unused")
    private static class ClassCallingWrongMethod {
        ClassCallingMethod classCallingMethod;

        void callWrong() {
            classCallingMethod.call();
        }
    }

    @SuppressWarnings("unused")
    private static class ClassCallingSelf {
        void origin() {
            target();
        }

        void target() {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithConstructor {
        ClassWithConstructor(String param) {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassCallingConstructor {
        ClassCallingConstructor(int number, Date date) {
        }

        void call() {
            new ClassWithConstructor("param");
        }
    }

    @SuppressWarnings("unused")
    private static class ClassCallingWrongConstructor {
        void callWrong() {
            new ClassCallingConstructor(0, null);
        }
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private static class ClassWithFieldMethodAndConstructor {
        String field;

        ClassWithFieldMethodAndConstructor(String param) {
        }

        void method(String param) {
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    private static class ClassAccessingWrongFieldMethodAndConstructor {
        void callWrong() {
            ClassAccessingFieldMethodAndConstructor instance = new ClassAccessingFieldMethodAndConstructor(0, null);
            instance.wrongField = "field";
            instance.call();
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

    @RuntimeRetentionAnnotation
    private static class SomeAnnotatedClass {
    }

    private static class NestedClassWithSomeMoreClasses {

        static class StaticNestedClass {
        }

        @SuppressWarnings("InnerClassMayBeStatic")
        class InnerMemberClass {
        }

        static Class<?> getAnonymousClass() {
            return new Serializable() {
            }.getClass();
        }

        static Class<?> getLocalClass() {
            class LocalClass {
            }
            return LocalClass.class;
        }
    }

}
