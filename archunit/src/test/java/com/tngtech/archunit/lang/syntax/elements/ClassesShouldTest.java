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
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.ClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.DefaultClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.SourceRetentionAnnotation;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.testclasses.SomeClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.WrongNamedClass;
import com.tngtech.archunit.testutil.ArchConfigurationExtension;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
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
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClassesShouldTest {
    static final String FAILURE_REPORT_NEWLINE_MARKER = "#";

    @RegisterExtension
    ArchConfigurationExtension archConfiguration = new ArchConfigurationExtension();

    static Stream<Arguments> haveFullyQualifiedName_rules() {
        return Stream.of(
                $(classes().should().haveFullyQualifiedName(SomeClass.class.getName())),
                $(classes().should(ArchConditions.haveFullyQualifiedName(SomeClass.class.getName())))
        );
    }

    @ParameterizedTest
    @MethodSource("haveFullyQualifiedName_rules")
    void haveFullyQualifiedName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have fully qualified name '%s'", SomeClass.class.getName()))
                .containsPattern(String.format("Class <%s> does not have fully qualified name '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(SomeClass.class.getName()),
                        locationPattern(WrongNamedClass.class)))
                .doesNotMatch(String.format(".*<%s>.*name.*", quote(SomeClass.class.getName())));
    }

    static Stream<Arguments> notHaveFullyQualifiedName_rules() {
        return Stream.of(
                $(classes().should().notHaveFullyQualifiedName(WrongNamedClass.class.getName())),
                $(classes().should(ArchConditions.notHaveFullyQualifiedName(WrongNamedClass.class.getName())))
        );
    }

    @ParameterizedTest
    @MethodSource("notHaveFullyQualifiedName_rules")
    void notHaveFullyQualifiedName(ArchRule rule) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not have fully qualified name '%s'", WrongNamedClass.class.getName()))
                .contains(String.format("Class <%s> has fully qualified name '%s'", WrongNamedClass.class.getName(), WrongNamedClass.class.getName()))
                .doesNotContain(String.format("<%s>.*name", SomeClass.class.getName()));
    }

    static Stream<Arguments> haveSimpleName_rules() {
        return Stream.of(
                $(classes().should().haveSimpleName(SomeClass.class.getSimpleName())),
                $(classes().should(ArchConditions.haveSimpleName(SomeClass.class.getSimpleName())))
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleName_rules")
    void haveSimpleName(ArchRule rule) {
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

    static Stream<Arguments> notHaveSimpleName_rules() {
        return Stream.of(
                $(classes().should().notHaveSimpleName(WrongNamedClass.class.getSimpleName())),
                $(classes().should(ArchConditions.notHaveSimpleName(WrongNamedClass.class.getSimpleName())))
        );
    }

    @ParameterizedTest
    @MethodSource("notHaveSimpleName_rules")
    void notHaveSimpleName(ArchRule rule) {
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

    static Stream<Arguments> haveNameMatching_rules() {
        String regex = containsPartOfRegex(SomeClass.class.getSimpleName());
        return Stream.of(
                $(classes().should().haveNameMatching(regex), regex),
                $(classes().should(ArchConditions.haveNameMatching(regex)), regex)
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameMatching_rules")
    void haveNameMatching(ArchRule rule, String regex) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name matching '%s'", regex))
                .containsPattern(String.format("Class <%s> does not have name matching '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(regex),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(String.format("%s", SomeClass.class.getSimpleName()));
    }

    static Stream<Arguments> haveNameNotMatching_rules() {
        String regex = containsPartOfRegex(WrongNamedClass.class.getSimpleName());
        return Stream.of(
                $(classes().should().haveNameNotMatching(regex), regex),
                $(classes().should(ArchConditions.haveNameNotMatching(regex)), regex)
        );
    }

    @ParameterizedTest
    @MethodSource("haveNameNotMatching_rules")
    void haveNameNotMatching(ArchRule rule, String regex) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have name not matching '%s'", regex))
                .containsPattern(String.format("Class <%s> has name matching '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(regex),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(String.format("%s", SomeClass.class.getSimpleName()));
    }

    static Stream<Arguments> haveSimpleNameStartingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return Stream.of(
                $(classes().should().haveSimpleNameStartingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleNameStartingWith(prefix)), prefix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameStartingWith_rules")
    void haveSimpleNameStartingWith(ArchRule rule, String prefix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name starting with '%s'", prefix))
                .containsPattern(String.format("Class <%s> does not have simple name starting with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(prefix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> haveSimpleNameNotStartingWith_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String prefix = simpleName.substring(0, simpleName.length() - 1);
        return Stream.of(
                $(classes().should().haveSimpleNameNotStartingWith(prefix), prefix),
                $(classes().should(ArchConditions.haveSimpleNameNotStartingWith(prefix)), prefix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameNotStartingWith_rules")
    void haveSimpleNameNotStartingWith(ArchRule rule, String prefix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not starting with '%s'", prefix))
                .containsPattern(String.format("Class <%s> has simple name starting with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(prefix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> haveSimpleNameContaining_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return Stream.of(
                $(classes().should().haveSimpleNameContaining(infix), infix),
                $(classes().should(ArchConditions.haveSimpleNameContaining(infix)), infix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameContaining_rules")
    void haveSimpleNameContaining(ArchRule rule, String infix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name containing '%s'", infix))
                .containsPattern(String.format("Class <%s> does not have simple name containing '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(infix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> haveSimpleNameNotContaining_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String infix = simpleName.substring(1, simpleName.length() - 1);
        return Stream.of(
                $(classes().should().haveSimpleNameNotContaining(infix), infix),
                $(classes().should(ArchConditions.haveSimpleNameNotContaining(infix)), infix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameNotContaining_rules")
    void haveSimpleNameNotContaining(ArchRule rule, String infix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not containing '%s'", infix))
                .containsPattern(String.format("Class <%s> has simple name containing '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(infix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> haveSimpleNameEndingWith_rules() {
        String simpleName = SomeClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return Stream.of(
                $(classes().should().haveSimpleNameEndingWith(suffix), suffix),
                $(classes().should(ArchConditions.haveSimpleNameEndingWith(suffix)), suffix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameEndingWith_rules")
    void haveSimpleNameEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name ending with '%s'", suffix))
                .containsPattern(String.format("Class <%s> does not have simple name ending with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(suffix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> haveSimpleNameNotEndingWith_rules() {
        String simpleName = WrongNamedClass.class.getSimpleName();
        String suffix = simpleName.substring(1);
        return Stream.of(
                $(classes().should().haveSimpleNameNotEndingWith(suffix), suffix),
                $(classes().should(ArchConditions.haveSimpleNameNotEndingWith(suffix)), suffix)
        );
    }

    @ParameterizedTest
    @MethodSource("haveSimpleNameNotEndingWith_rules")
    void haveSimpleNameNotEndingWith(ArchRule rule, String suffix) {
        EvaluationResult result = rule.evaluate(importClasses(
                SomeClass.class, WrongNamedClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should have simple name not ending with '%s'", suffix))
                .containsPattern(String.format("Class <%s> has simple name ending with '%s' in %s",
                        quote(WrongNamedClass.class.getName()),
                        quote(suffix),
                        locationPattern(WrongNamedClass.class)))
                .doesNotContain(SomeClass.class.getName());
    }

    static Stream<Arguments> resideInAPackage_rules() {
        String thePackage = ArchRule.class.getPackage().getName();
        return Stream.of(
                $(classes().should().resideInAPackage(thePackage), thePackage),
                $(classes().should(ArchConditions.resideInAPackage(thePackage)), thePackage)
        );
    }

    @ParameterizedTest
    @MethodSource("resideInAPackage_rules")
    void resideInAPackage(ArchRule rule, String packageIdentifier) {
        checkTestStillValid(packageIdentifier,
                ImmutableSet.of(ArchRule.class, ArchCondition.class),
                ImmutableSet.of(ArchConfiguration.class),
                ImmutableSet.of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside in a package '%s'", packageIdentifier))
                .containsPattern(doesntResideInAPackagePatternFor(ArchConfiguration.class, packageIdentifier))
                .containsPattern(doesntResideInAPackagePatternFor(GivenObjects.class, packageIdentifier))
                .doesNotContain(String.format("%s", ArchRule.class.getSimpleName()))
                .doesNotContain(String.format("%s", ArchCondition.class.getSimpleName()));
    }

    static Stream<Arguments> resideInAnyPackage_rules() {
        String firstPackage = ArchRule.class.getPackage().getName();
        String secondPackage = ArchConfiguration.class.getPackage().getName();
        return Stream.of(
                $(classes().should().resideInAnyPackage(firstPackage, secondPackage),
                        new String[]{firstPackage, secondPackage}),
                $(classes().should(ArchConditions.resideInAnyPackage(firstPackage, secondPackage)),
                        new String[]{firstPackage, secondPackage})
        );
    }

    @ParameterizedTest
    @MethodSource("resideInAnyPackage_rules")
    void resideInAnyPackage(ArchRule rule, String... packageIdentifiers) {
        checkTestStillValid(packageIdentifiers,
                ImmutableSet.of(ArchRule.class, ArchConfiguration.class),
                ImmutableSet.of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside in any package [%s]", joinSingleQuoted(packageIdentifiers)))
                .containsPattern(doesntResideInAnyPackagePatternFor(GivenObjects.class, packageIdentifiers))
                .doesNotContain(String.format("%s", ArchRule.class.getSimpleName()))
                .doesNotContain(String.format("%s", ArchConfiguration.class.getSimpleName()));
    }

    static Stream<Arguments> resideOutsideOfPackage_rules() {
        String thePackage = ArchRule.class.getPackage().getName();
        return Stream.of(
                $(classes().should().resideOutsideOfPackage(thePackage), thePackage),
                $(classes().should(ArchConditions.resideOutsideOfPackage(thePackage)), thePackage)
        );
    }

    @ParameterizedTest
    @MethodSource("resideOutsideOfPackage_rules")
    void resideOutsideOfPackage(ArchRule rule, String packageIdentifier) {
        checkTestStillValid(packageIdentifier,
                ImmutableSet.of(ArchRule.class, ArchCondition.class),
                ImmutableSet.of(ArchConfiguration.class),
                ImmutableSet.of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside outside of package '%s'", packageIdentifier))
                .containsPattern(doesntResideOutsideOfPackagePatternFor(ArchRule.class, packageIdentifier))
                .containsPattern(doesntResideOutsideOfPackagePatternFor(ArchCondition.class, packageIdentifier))
                .doesNotContain(String.format("%s", ArchConfiguration.class.getSimpleName()))
                .doesNotContain(String.format("%s", GivenObjects.class.getSimpleName()));
    }

    static Stream<Arguments> resideOutsideOfPackages_rules() {
        String firstPackage = ArchRule.class.getPackage().getName();
        String secondPackage = ArchConfiguration.class.getPackage().getName();
        return Stream.of(
                $(classes().should().resideOutsideOfPackages(firstPackage, secondPackage),
                        new String[]{firstPackage, secondPackage}),
                $(classes().should(ArchConditions.resideOutsideOfPackages(firstPackage, secondPackage)),
                        new String[]{firstPackage, secondPackage})
        );
    }

    @ParameterizedTest
    @MethodSource("resideOutsideOfPackages_rules")
    void resideOutsideOfPackages(ArchRule rule, String... packageIdentifiers) {
        checkTestStillValid(packageIdentifiers,
                ImmutableSet.of(ArchRule.class, ArchConfiguration.class),
                ImmutableSet.of(GivenObjects.class));

        EvaluationResult result = rule.evaluate(importClasses(
                ArchRule.class, ArchCondition.class, ArchConfiguration.class, GivenObjects.class));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should reside outside of packages [%s]", joinSingleQuoted(packageIdentifiers)))
                .containsPattern(doesntResideOutsideOfPackagesPatternFor(ArchRule.class, packageIdentifiers))
                .containsPattern(doesntResideOutsideOfPackagesPatternFor(ArchCondition.class, packageIdentifiers))
                .doesNotContain(String.format("%s", GivenObjects.class.getSimpleName()));
    }

    static Stream<Arguments> visibility_rules() {
        return Stream.of(
                $(classes().should().bePublic(), PUBLIC, PublicClass.class, PrivateClass.class),
                $(classes().should(bePublic()), PUBLIC, PublicClass.class, PrivateClass.class),
                $(classes().should().beProtected(), PROTECTED, ProtectedClass.class, PrivateClass.class),
                $(classes().should(beProtected()), PROTECTED, ProtectedClass.class, PrivateClass.class),
                $(classes().should().bePrivate(), PRIVATE, PrivateClass.class, PublicClass.class),
                $(classes().should(bePrivate()), PRIVATE, PrivateClass.class, PublicClass.class));
    }

    @ParameterizedTest
    @MethodSource("visibility_rules")
    void visibility(ArchRule rule, JavaModifier modifier, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be %s", modifier.name().toLowerCase()))
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    static Stream<Arguments> not_visibility_rules() {
        return Stream.of(
                $(classes().should().notBePublic(), PUBLIC, PrivateClass.class, PublicClass.class),
                $(classes().should(notBePublic()), PUBLIC, PrivateClass.class, PublicClass.class),
                $(classes().should().notBeProtected(), PROTECTED, PrivateClass.class, ProtectedClass.class),
                $(classes().should(notBeProtected()), PROTECTED, PrivateClass.class, ProtectedClass.class),
                $(classes().should().notBePrivate(), PRIVATE, PublicClass.class, PrivateClass.class),
                $(classes().should(notBePrivate()), PRIVATE, PublicClass.class, PrivateClass.class));
    }

    @ParameterizedTest
    @MethodSource("not_visibility_rules")
    void notVisibility(ArchRule rule, JavaModifier modifier, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be %s", modifier.name().toLowerCase()))
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(violated.getName()), modifier))
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    static Stream<Arguments> package_private_visibility_rules() {
        return Stream.of(
                $(classes().should().bePackagePrivate(), "be package private"),
                $(classes().should(bePackagePrivate()), "be package private"));
    }

    @ParameterizedTest
    @MethodSource("package_private_visibility_rules")
    void package_private_visibility(ArchRule rule, String description) {
        EvaluationResult result = rule.evaluate(importClasses(PackagePrivateClass.class, PrivateClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should " + description)
                .containsPattern(String.format("Class <%s> .* modifier %s", quote(PrivateClass.class.getName()), PRIVATE))
                .doesNotMatch(String.format(".*<%s>.* modifier.*", quote(PackagePrivateClass.class.getName())));
    }

    static Stream<Arguments> non_package_private_visibility_rules() {
        return Stream.of(
                $(classes().should().notBePackagePrivate(), "not be package private"),
                $(classes().should(notBePackagePrivate()), "not be package private"));
    }

    @ParameterizedTest
    @MethodSource("non_package_private_visibility_rules")
    void non_package_private_visibility(ArchRule rule, String description) {
        EvaluationResult result = rule.evaluate(importClasses(PrivateClass.class, PackagePrivateClass.class));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should " + description)
                .contains(String.format("Class <%s>", PackagePrivateClass.class.getName()))
                .contains("does not have modifier " + PUBLIC)
                .contains("does not have modifier " + PROTECTED)
                .contains("does not have modifier " + PRIVATE)
                .doesNotMatch(String.format(".*<%s>.* modifier.*", quote(PrivateClass.class.getName())));
    }

    static Stream<Arguments> modifiers_rules() {
        return Stream.of(
                $(classes().should().haveModifier(PUBLIC), "", PUBLIC,
                        PublicClass.class, PrivateClass.class),
                $(classes().should(haveModifier(PUBLIC)), "", PUBLIC,
                        PublicClass.class, PrivateClass.class),
                $(classes().should().notHaveModifier(PUBLIC), "not ", PUBLIC,
                        PrivateClass.class, PublicClass.class),
                $(classes().should(notHaveModifier(PUBLIC)), "not ", PUBLIC,
                        PrivateClass.class, PublicClass.class));
    }

    @ParameterizedTest
    @MethodSource("modifiers_rules")
    void modifiers(ArchRule rule, String havePrefix, JavaModifier modifier,
                Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should %shave modifier %s", havePrefix, modifier.name()))
                .containsPattern(String.format("Class <%s> .* modifier %s in %s",
                        quote(violated.getName()), modifier, locationPattern(getClass()))) // -> location == enclosingClass()
                .doesNotMatch(String.format(".*<%s>.* modifier %s.*", quote(satisfied.getName()), modifier));
    }

    static Stream<Arguments> annotated_rules() {
        return Stream.of(
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

    @ParameterizedTest
    @MethodSource("annotated_rules")
    void annotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
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

        expectInvalidSyntaxUsageForRetentionSource(() -> classes().should().beAnnotatedWith(SourceRetentionAnnotation.class));
    }

    static Stream<Arguments> notAnnotated_rules() {
        return Stream.of(
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

    @ParameterizedTest
    @MethodSource("notAnnotated_rules")
    void notAnnotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be annotated with @" + RuntimeRetentionAnnotation.class.getSimpleName())
                .containsPattern(String.format("Class <%s> is annotated with @%s in %s",
                        quote(wrongClass.getName()),
                        RuntimeRetentionAnnotation.class.getSimpleName(),
                        locationPattern(getClass())))
                .doesNotMatch(String.format(".*<%s>.*annotated.*", quote(correctClass.getName())));
    }

    static Stream<Arguments> metaAnnotated_rules() {
        return Stream.of(
                $(classes().should().beMetaAnnotatedWith(SomeMetaAnnotation.class),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beMetaAnnotatedWith(SomeMetaAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beMetaAnnotatedWith(SomeMetaAnnotation.class.getName()),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beMetaAnnotatedWith(SomeMetaAnnotation.class.getName())),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should().beMetaAnnotatedWith(annotation(SomeMetaAnnotation.class)),
                        SomeAnnotatedClass.class, String.class),
                $(classes().should(ArchConditions.beMetaAnnotatedWith(annotation(SomeMetaAnnotation.class))),
                        SomeAnnotatedClass.class, String.class));
    }

    @ParameterizedTest
    @MethodSource("metaAnnotated_rules")
    void metaAnnotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be meta-annotated with @%s", SomeMetaAnnotation.class.getSimpleName()))
                .containsPattern(String.format("Class <%s> is not meta-annotated with @%s in %s",
                        quote(wrongClass.getName()),
                        SomeMetaAnnotation.class.getSimpleName(),
                        locationPattern(String.class)))
                .doesNotMatch(String.format(".*<%s>.*meta-annotated.*", quote(correctClass.getName())));
    }

    static Stream<Arguments> notMetaAnnotated_rules() {
        return Stream.of(
                $(classes().should().notBeMetaAnnotatedWith(SomeMetaAnnotation.class),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeMetaAnnotatedWith(SomeMetaAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeMetaAnnotatedWith(SomeMetaAnnotation.class.getName()),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeMetaAnnotatedWith(SomeMetaAnnotation.class.getName())),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should().notBeMetaAnnotatedWith(annotation(SomeMetaAnnotation.class)),
                        String.class, SomeAnnotatedClass.class),
                $(classes().should(ArchConditions.notBeMetaAnnotatedWith(annotation(SomeMetaAnnotation.class))),
                        String.class, SomeAnnotatedClass.class));
    }

    @ParameterizedTest
    @MethodSource("notMetaAnnotated_rules")
    void notMetaAnnotatedWith(ArchRule rule, Class<?> correctClass, Class<?> wrongClass) {
        EvaluationResult result = rule.evaluate(importClasses(correctClass, wrongClass));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be meta-annotated with @" + SomeMetaAnnotation.class.getSimpleName())
                .containsPattern(String.format("Class <%s> is meta-annotated with @%s in %s",
                        quote(wrongClass.getName()),
                        SomeMetaAnnotation.class.getSimpleName(),
                        locationPattern(getClass())))
                .doesNotMatch(String.format(".*<%s>.*meta-annotated.*", quote(correctClass.getName())));
    }

    /**
     * Compare {@link CanBeAnnotatedTest#annotatedWith_Retention_Source_is_rejected}
     */
    @Test
    public void notBeAnnotatedWith_Retention_Source_is_rejected() {
        classes().should().notBeAnnotatedWith(RuntimeRetentionAnnotation.class);
        classes().should().notBeAnnotatedWith(ClassRetentionAnnotation.class);
        classes().should().notBeAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(() -> classes().should().notBeAnnotatedWith(SourceRetentionAnnotation.class));
    }

    static Stream<Arguments> implement_satisfied_rules() {
        return Stream.of(
                $(classes().should().implement(Collection.class), ArrayList.class),
                $(classes().should(ArchConditions.implement(Collection.class)), ArrayList.class),
                $(classes().should().implement(Collection.class.getName()), ArrayList.class),
                $(classes().should(ArchConditions.implement(Collection.class.getName())), ArrayList.class),
                $(classes().should().implement(name(Collection.class.getName()).as(Collection.class.getName())), ArrayList.class),
                $(classes().should(ArchConditions.implement(name(Collection.class.getName()).as(Collection.class.getName()))), ArrayList.class));
    }

    @ParameterizedTest
    @MethodSource("implement_satisfied_rules")
    void implement_satisfied(ArchRule rule, Class<?> satisfied) {
        EvaluationResult result = rule.evaluate(importHierarchies(satisfied));

        assertThat(singleLineFailureReportOf(result))
                .doesNotMatch(String.format(".*%s.* implement.*", quote(satisfied.getName())));
    }

    @Test
    public void implement_rejects_non_interface_types() {
        classes().should().implement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(AbstractList.class, () -> classes().should().implement(AbstractList.class));
    }

    static List<Arguments> implement_not_satisfied_rules() {
        return ImmutableList.<Arguments>builder()
                .addAll(implementNotSatisfiedCases(Collection.class, List.class))
                .addAll(implementNotSatisfiedCases(Set.class, ArrayList.class))
                .build();
    }

    private static List<Arguments> implementNotSatisfiedCases(Class<?> classToCheckAgainst, Class<?> violating) {
        return ImmutableList.of(
                $(classes().should().implement(classToCheckAgainst),
                        classToCheckAgainst, violating),
                $(classes().should(ArchConditions.implement(classToCheckAgainst)),
                        classToCheckAgainst, violating),
                $(classes().should().implement(classToCheckAgainst.getName()),
                        classToCheckAgainst, violating),
                $(classes().should(ArchConditions.implement(classToCheckAgainst.getName())),
                        classToCheckAgainst, violating),
                $(classes().should().implement(name(classToCheckAgainst.getName()).as(classToCheckAgainst.getName())),
                        classToCheckAgainst, violating),
                $(classes().should(ArchConditions.implement(name(classToCheckAgainst.getName()).as(classToCheckAgainst.getName()))),
                        classToCheckAgainst, violating));
    }

    @ParameterizedTest
    @MethodSource("implement_not_satisfied_rules")
    void implement_not_satisfied(ArchRule rule, Class<?> classToCheckAgainst, Class<?> violating) {
        EvaluationResult result = rule.evaluate(importHierarchies(violating));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should implement %s", classToCheckAgainst.getName()))
                .containsPattern(String.format("Class <%s> does not implement %s in %s",
                        quote(violating.getName()),
                        quote(classToCheckAgainst.getName()),
                        locationPattern(violating)));
    }

    static Stream<Arguments> notImplement_rules() {
        return Stream.of(
                $(classes().should().notImplement(Collection.class), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(Collection.class)), List.class, ArrayList.class),
                $(classes().should().notImplement(Collection.class.getName()), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(Collection.class.getName())), List.class, ArrayList.class),
                $(classes().should().notImplement(name(Collection.class.getName()).as(Collection.class.getName())), List.class, ArrayList.class),
                $(classes().should(ArchConditions.notImplement(name(Collection.class.getName()).as(Collection.class.getName()))),
                        List.class, ArrayList.class));
    }

    @ParameterizedTest
    @MethodSource("notImplement_rules")
    void notImplement(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not implement %s", Collection.class.getName()))
                .containsPattern(String.format("Class <%s> does implement %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* implement.*", quote(satisfied.getName())));
    }

    @Test
    public void notImplement_rejects_non_interface_types() {
        classes().should().notImplement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(AbstractList.class, () -> classes().should().notImplement(AbstractList.class));
    }

    static Stream<Arguments> assignableTo_rules() {
        return Stream.of(
                $(classes().should().beAssignableTo(Collection.class), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(Collection.class)), List.class, String.class),
                $(classes().should().beAssignableTo(Collection.class.getName()), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(Collection.class.getName())), List.class, String.class),
                $(classes().should().beAssignableTo(name(Collection.class.getName()).as(Collection.class.getName())), List.class, String.class),
                $(classes().should(ArchConditions.beAssignableTo(name(Collection.class.getName()).as(Collection.class.getName()))),
                        List.class, String.class));
    }

    @ParameterizedTest
    @MethodSource("assignableTo_rules")
    void assignableTo(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be assignable to %s", Collection.class.getName()))
                .containsPattern(String.format("Class <%s> is not assignable to %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notAssignableTo_rules() {
        return Stream.of(
                $(classes().should().notBeAssignableTo(Collection.class), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(Collection.class)), String.class, List.class),
                $(classes().should().notBeAssignableTo(Collection.class.getName()), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(Collection.class.getName())), String.class, List.class),
                $(classes().should().notBeAssignableTo(name(Collection.class.getName()).as(Collection.class.getName())), String.class, List.class),
                $(classes().should(ArchConditions.notBeAssignableTo(name(Collection.class.getName()).as(Collection.class.getName()))),
                        String.class, List.class));
    }

    @ParameterizedTest
    @MethodSource("notAssignableTo_rules")
    void notAssignableTo(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be assignable to %s", Collection.class.getName()))
                .containsPattern(String.format("Class <%s> is assignable to %s in %s",
                        quote(violated.getName()),
                        quote(Collection.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> assignableFrom_rules() {
        return Stream.of(
                $(classes().should().beAssignableFrom(List.class), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(List.class)), Collection.class, String.class),
                $(classes().should().beAssignableFrom(List.class.getName()), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(List.class.getName())), Collection.class, String.class),
                $(classes().should().beAssignableFrom(name(List.class.getName()).as(List.class.getName())), Collection.class, String.class),
                $(classes().should(ArchConditions.beAssignableFrom(name(List.class.getName()).as(List.class.getName()))),
                        Collection.class, String.class));
    }

    @ParameterizedTest
    @MethodSource("assignableFrom_rules")
    void assignableFrom(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should be assignable from %s", List.class.getName()))
                .containsPattern(String.format("Class <%s> is not assignable from %s in %s",
                        quote(violated.getName()),
                        quote(List.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notAssignableFrom_rules() {
        return Stream.of(
                $(classes().should().notBeAssignableFrom(List.class), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(List.class)), String.class, Collection.class),
                $(classes().should().notBeAssignableFrom(List.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(List.class.getName())), String.class, Collection.class),
                $(classes().should().notBeAssignableFrom(name(List.class.getName()).as(List.class.getName())), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeAssignableFrom(name(List.class.getName()).as(List.class.getName()))),
                        String.class, Collection.class));
    }

    @ParameterizedTest
    @MethodSource("notAssignableFrom_rules")
    void notAssignableFrom(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains(String.format("classes should not be assignable from %s", List.class.getName()))
                .containsPattern(String.format("Class <%s> is assignable from %s in %s",
                        quote(violated.getName()),
                        quote(List.class.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* assignable.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> accessField_rules() {
        return Stream.of(
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

    @ParameterizedTest
    @MethodSource("accessField_rules")
    void accessField(ArchRule rule, String accessTypePlural, String accessTypeSingularRegex) {
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

    static Stream<Arguments> accessFieldWhere_rules() {
        return Stream.of(
                $(classes().should().getFieldWhere(accessTargetIs(ClassWithField.class)), "get", "gets"),
                $(classes().should(ArchConditions.getFieldWhere(accessTargetIs(ClassWithField.class))), "get", "gets"),
                $(classes().should().setFieldWhere(accessTargetIs(ClassWithField.class)), "set", "sets"),
                $(classes().should(ArchConditions.setFieldWhere(accessTargetIs(ClassWithField.class))), "set", "sets"),
                $(classes().should().accessFieldWhere(accessTargetIs(ClassWithField.class)), "access", "(gets|sets)"),
                $(classes().should(ArchConditions.accessFieldWhere(accessTargetIs(ClassWithField.class))), "access", "(gets|sets)")
        );
    }

    @ParameterizedTest
    @MethodSource("accessFieldWhere_rules")
    void accessFieldWhere(ArchRule rule, String accessTypePlural, String accessTypeSingularRegex) {
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

    static Stream<ArchRule> onlyAccessFieldsThat_rules() {
        return Stream.of(
                classes().should().onlyAccessFieldsThat(are(declaredIn(ClassWithField.class))),
                classes().should(ArchConditions.onlyAccessFieldsThat(are(declaredIn(ClassWithField.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyAccessFieldsThat_rules")
    void onlyAccessFieldsThat(ArchRule rule) {
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

    static Stream<ArchRule> callMethod_rules() {
        return Stream.of(
                classes().should().callMethod(ClassWithMethod.class, "method", String.class),
                classes().should(ArchConditions.callMethod(ClassWithMethod.class, "method", String.class)),
                classes().should().callMethod(ClassWithMethod.class.getName(), "method", String.class.getName()),
                classes().should(ArchConditions.callMethod(ClassWithMethod.class.getName(), "method", String.class.getName()))
        );
    }

    @ParameterizedTest
    @MethodSource("callMethod_rules")
    void callMethod(ArchRule rule) {
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

    static Stream<Arguments> callMethodWhere_rules() {
        return Stream.of(
                $(classes().should().callMethodWhere(callTargetIs(ClassWithMethod.class))),
                $(classes().should(ArchConditions.callMethodWhere(callTargetIs(ClassWithMethod.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("callMethodWhere_rules")
    void callMethodWhere(ArchRule rule) {
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

    static Stream<Arguments> onlyCallMethodsThat_rules() {
        return Stream.of(
                $(classes().should().onlyCallMethodsThat(are(declaredIn(ClassWithMethod.class)))),
                $(classes().should(ArchConditions.onlyCallMethodsThat(are(declaredIn(ClassWithMethod.class)))))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyCallMethodsThat_rules")
    void onlyCallMethodsThat(ArchRule rule) {
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

    static Stream<Arguments> callConstructor_rules() {
        return Stream.of(
                $(classes().should().callConstructor(ClassWithConstructor.class, String.class)),
                $(classes().should(ArchConditions.callConstructor(ClassWithConstructor.class, String.class))),
                $(classes().should().callConstructor(ClassWithConstructor.class.getName(), String.class.getName())),
                $(classes().should(ArchConditions.callConstructor(ClassWithConstructor.class.getName(), String.class.getName())))
        );
    }

    @ParameterizedTest
    @MethodSource("callConstructor_rules")
    void callConstructor(ArchRule rule) {
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

    static Stream<Arguments> callConstructorWhere_rules() {
        return Stream.of(
                $(classes().should().callConstructorWhere(callTargetIs(ClassWithConstructor.class))),
                $(classes().should(ArchConditions.callConstructorWhere(callTargetIs(ClassWithConstructor.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("callConstructorWhere_rules")
    void callConstructorWhere(ArchRule rule) {
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

    static Stream<Arguments> onlyCallConstructorsThat_rules() {
        return Stream.of(
                $(classes().should().onlyCallConstructorsThat(are(declaredIn(ClassWithConstructor.class)))),
                $(classes().should(ArchConditions.onlyCallConstructorsThat(are(declaredIn(ClassWithConstructor.class)))))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyCallConstructorsThat_rules")
    void onlyCallConstructorsThat(ArchRule rule) {
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

    static Stream<Arguments> accessTargetWhere_rules() {
        return Stream.of(
                $(classes().should().accessTargetWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))),
                $(classes().should(ArchConditions.accessTargetWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("accessTargetWhere_rules")
    void accessTargetWhere(ArchRule rule) {
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

    static Stream<Arguments> callCodeUnitWhere_rules() {
        return Stream.of(
                $(classes().should().callCodeUnitWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))),
                $(classes().should(ArchConditions.callCodeUnitWhere(accessTargetIs(ClassWithFieldMethodAndConstructor.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("callCodeUnitWhere_rules")
    void callCodeUnitWhere(ArchRule rule) {
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

    static Stream<Arguments> onlyCallCodeUnitsThat_rules() {
        return Stream.of(
                $(classes().should().onlyCallCodeUnitsThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))),
                $(classes().should(ArchConditions.onlyCallCodeUnitsThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyCallCodeUnitsThat_rules")
    void onlyCallCodeUnitsThat(ArchRule rule) {
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

    static Stream<Arguments> onlyAccessMembersThat_rules() {
        return Stream.of(
                $(classes().should().onlyAccessMembersThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))),
                $(classes().should(ArchConditions.onlyAccessMembersThat(are(declaredIn(ClassWithFieldMethodAndConstructor.class)))))
        );
    }

    @ParameterizedTest
    @MethodSource("onlyAccessMembersThat_rules")
    void onlyAccessMembersThat(ArchRule rule) {
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

    static Stream<Arguments> beInterfaces_rules() {
        return Stream.of(
                $(classes().should().beInterfaces(), Collection.class, String.class),
                $(classes().should(ArchConditions.beInterfaces()), Collection.class, String.class));
    }

    @ParameterizedTest
    @MethodSource("beInterfaces_rules")
    void beInterfaces(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be interfaces")
                .containsPattern(String.format("Class <%s> is no interface in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* interface.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeInterfaces_rules() {
        return Stream.of(
                $(classes().should().notBeInterfaces(), String.class, Collection.class),
                $(classes().should(ArchConditions.notBeInterfaces()), String.class, Collection.class));
    }

    @ParameterizedTest
    @MethodSource("notBeInterfaces_rules")
    void notBeInterfaces(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be interfaces")
                .containsPattern(String.format("Class <%s> is an interface in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* interface.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beEnums_rules() {
        return Stream.of(
                $(classes().should().beEnums(), StandardCopyOption.class, String.class),
                $(classes().should(ArchConditions.beEnums()), StandardCopyOption.class, String.class));
    }

    @ParameterizedTest
    @MethodSource("beEnums_rules")
    void beEnums(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be enums")
                .containsPattern(String.format("Class <%s> is no enum in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* enum.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeEnums_rules() {
        return Stream.of(
                $(classes().should().notBeEnums(), String.class, StandardCopyOption.class),
                $(classes().should(ArchConditions.notBeEnums()), String.class, StandardCopyOption.class));
    }

    @ParameterizedTest
    @MethodSource("notBeEnums_rules")
    void notBeEnums(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be enums")
                .containsPattern(String.format("Class <%s> is an enum in %s",
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* enum.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beTopLevelClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().beTopLevelClasses(), topLevelClass, staticNestedClass),
                $(classes().should(ArchConditions.beTopLevelClasses()), topLevelClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beTopLevelClasses_rules")
    void beTopLevelClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be top level classes")
                .containsPattern(String.format("Class <%s> is no top level class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* top level class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeTopLevelClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().notBeTopLevelClasses(), staticNestedClass, topLevelClass),
                $(classes().should(ArchConditions.notBeTopLevelClasses()), staticNestedClass, topLevelClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeTopLevelClasses_rules")
    void notBeTopLevelClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be top level classes")
                .containsPattern(String.format("Class <%s> is a top level class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* top level class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beNestedClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().beNestedClasses(), staticNestedClass, topLevelClass),
                $(classes().should(ArchConditions.beNestedClasses()), staticNestedClass, topLevelClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beNestedClasses_rules")
    void beNestedClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be nested classes")
                .containsPattern(String.format("Class <%s> is no nested class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* nested class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeNestedClasses_rules() {
        Class<?> topLevelClass = List.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().notBeNestedClasses(), topLevelClass, staticNestedClass),
                $(classes().should(ArchConditions.notBeNestedClasses()), topLevelClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeNestedClasses_rules")
    void notBeNestedClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be nested classes")
                .containsPattern(String.format("Class <%s> is a nested class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* nested class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beMemberClasses_rules() {
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();

        return Stream.of(
                $(classes().should().beMemberClasses(), staticNestedClass, anonymousClass),
                $(classes().should(ArchConditions.beMemberClasses()), staticNestedClass, anonymousClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beMemberClasses_rules")
    void beMemberClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be member classes")
                .containsPattern(String.format("Class <%s> is no member class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* member class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeMemberClasses_rules() {
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();

        return Stream.of(
                $(classes().should().notBeMemberClasses(), anonymousClass, staticNestedClass),
                $(classes().should(ArchConditions.notBeMemberClasses()), anonymousClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeMemberClasses_rules")
    void notBeMemberClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be member classes")
                .containsPattern(String.format("Class <%s> is a member class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* member class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beInnerClasses_rules() {
        Class<?> innerMemberClass = NestedClassWithSomeMoreClasses.InnerMemberClass.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().beInnerClasses(), innerMemberClass, staticNestedClass),
                $(classes().should(ArchConditions.beInnerClasses()), innerMemberClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beInnerClasses_rules")
    void beInnerClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be inner classes")
                .containsPattern(String.format("Class <%s> is no inner class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* inner class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeInnerClasses_rules() {
        Class<?> nonStaticNestedClass = NestedClassWithSomeMoreClasses.InnerMemberClass.class;
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().notBeInnerClasses(), staticNestedClass, nonStaticNestedClass),
                $(classes().should(ArchConditions.notBeInnerClasses()), staticNestedClass, nonStaticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeInnerClasses_rules")
    void notBeInnerClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be inner classes")
                .containsPattern(String.format("Class <%s> is an inner class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* inner class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beAnonymousClasses_rules() {
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().beAnonymousClasses(), anonymousClass, staticNestedClass),
                $(classes().should(ArchConditions.beAnonymousClasses()), anonymousClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beAnonymousClasses_rules")
    void beAnonymousClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be anonymous classes")
                .containsPattern(String.format("Class <%s> is no anonymous class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* anonymous class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeAnonymousClasses_rules() {
        Class<?> anonymousClass = NestedClassWithSomeMoreClasses.getAnonymousClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().notBeAnonymousClasses(), staticNestedClass, anonymousClass),
                $(classes().should(ArchConditions.notBeAnonymousClasses()), staticNestedClass, anonymousClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeAnonymousClasses_rules")
    void notBeAnonymousClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be anonymous classes")
                .containsPattern(String.format("Class <%s> is an anonymous class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* anonymous class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> beLocalClasses_rules() {
        Class<?> localClass = NestedClassWithSomeMoreClasses.getLocalClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().beLocalClasses(), localClass, staticNestedClass),
                $(classes().should(ArchConditions.beLocalClasses()), localClass, staticNestedClass)
        );
    }

    @ParameterizedTest
    @MethodSource("beLocalClasses_rules")
    void beLocalClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be local classes")
                .containsPattern(String.format("Class <%s> is no local class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* local class.*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeLocalClasses_rules() {
        Class<?> localClass = NestedClassWithSomeMoreClasses.getLocalClass();
        Class<?> staticNestedClass = NestedClassWithSomeMoreClasses.StaticNestedClass.class;

        return Stream.of(
                $(classes().should().notBeLocalClasses(), staticNestedClass, localClass),
                $(classes().should(ArchConditions.notBeLocalClasses()), staticNestedClass, localClass)
        );
    }

    @ParameterizedTest
    @MethodSource("notBeLocalClasses_rules")
    void notBeLocalClasses(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be local classes")
                .containsPattern(String.format("Class <%s> is a local class", quote(violated.getName())))
                .doesNotMatch(String.format(".*%s.* local class.*", quote(satisfied.getName())));
    }

    @Test
    public void containNumberOfElements_passes_on_matching_predicate() {
        assertThatRule(classes().should().containNumberOfElements(alwaysTrue()))
                .checking(importClasses(String.class, Integer.class))
                .hasNoViolation();
    }

    @Test
    public void containNumberOfElements_fails_on_mismatching_predicate() {
        assertThatRule(classes().should().containNumberOfElements(alwaysFalse()))
                .checking(importClasses(String.class, Integer.class))
                .hasOnlyViolations("there is/are 2 element(s) in [java.lang.Integer, java.lang.String]");
    }

    static Stream<Arguments> beClass_rules() {
        return Stream.of(
                $(classes().should().be(String.class), String.class, Collection.class),
                $(classes().should().be(String.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.be(String.class)), String.class, Collection.class),
                $(classes().should(ArchConditions.be(String.class.getName())), String.class, Collection.class));
    }

    @ParameterizedTest
    @MethodSource("beClass_rules")
    void beClass(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should be " + satisfied.getName())
                .containsPattern(String.format("Class <%s> is not %s in %s",
                        quote(violated.getName()),
                        quote(satisfied.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*<%s>.* is .*", quote(satisfied.getName())));
    }

    static Stream<Arguments> notBeClass_rules() {
        return Stream.of(
                $(classes().should().notBe(Collection.class), String.class, Collection.class),
                $(classes().should().notBe(Collection.class.getName()), String.class, Collection.class),
                $(classes().should(ArchConditions.notBe(Collection.class)), String.class, Collection.class),
                $(classes().should(ArchConditions.notBe(Collection.class.getName())), String.class, Collection.class));
    }

    @ParameterizedTest
    @MethodSource("notBeClass_rules")
    void notBeClass(ArchRule rule, Class<?> satisfied, Class<?> violated) {
        EvaluationResult result = rule.evaluate(importClasses(satisfied, violated));

        assertThat(singleLineFailureReportOf(result))
                .contains("classes should not be " + violated.getName())
                .containsPattern(String.format("Class <%s> is %s in %s",
                        quote(violated.getName()),
                        quote(violated.getName()),
                        locationPattern(violated)))
                .doesNotMatch(String.format(".*%s.* is .*", quote(satisfied.getName())));
    }

    static Stream<Arguments> onlyAccessRules_rules() {
        return Stream.of(
                $(classes().should().onlyCallMethodsThat(are(not(declaredIn(ClassWithMethod.class)))), ClassCallingMethod.class),
                $(classes().should(ArchConditions.onlyCallMethodsThat(are(not(declaredIn(ClassWithMethod.class))))), ClassCallingMethod.class),
                $(classes().should().onlyCallConstructorsThat(are(not(declaredIn(ClassWithConstructor.class)))), ClassCallingConstructor.class),
                $(classes().should(ArchConditions.onlyCallConstructorsThat(are(not(declaredIn(ClassWithConstructor.class))))), ClassCallingConstructor.class),
                $(classes().should().onlyCallCodeUnitsThat(are(not(declaredIn(ClassWithMethod.class)))), ClassCallingMethod.class),
                $(classes().should(ArchConditions.onlyCallCodeUnitsThat(are(not(declaredIn(ClassWithMethod.class))))), ClassCallingMethod.class),
                $(classes().should().onlyAccessFieldsThat(are(not(declaredIn(ClassWithField.class)))), ClassAccessingField.class),
                $(classes().should(ArchConditions.onlyAccessFieldsThat(are(not(declaredIn(ClassWithField.class))))), ClassAccessingField.class),
                $(classes().should().onlyAccessMembersThat(are(not(declaredIn(ClassWithField.class)))), ClassAccessingField.class),
                $(classes().should(ArchConditions.onlyAccessMembersThat(are(not(declaredIn(ClassWithField.class))))), ClassAccessingField.class));
    }

    @ParameterizedTest
    @MethodSource("onlyAccessRules_rules")
    void onlyCall_should_report_success_if_targets_are_non_resolvable(ArchRule rule, Class<?> classCallingUnresolvableTarget) {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);

        assertThatRule(rule).checking(importClasses(classCallingUnresolvableTarget)).hasNoViolation();
    }

    @Test
    public void should_fail_on_empty_should_by_default() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                ruleWithEmptyShould().check(new ClassFileImporter().importClasses(getClass()));
            }
        }).isInstanceOf(AssertionError.class)
                .hasMessageContaining("failed to check any classes");
    }

    @Test
    public void should_allow_empty_should_if_configured() {
        archConfiguration.setFailOnEmptyShould(false);

        ruleWithEmptyShould().check(new ClassFileImporter().importClasses(getClass()));
    }

    @Test
    public void should_allow_empty_should_if_overridden_by_rule() {
        archConfiguration.setFailOnEmptyShould(true);

        ruleWithEmptyShould().allowEmptyShould(true).check(new ClassFileImporter().importClasses(getClass()));
    }

    private static ArchRule ruleWithEmptyShould() {
        return classes().that(alwaysFalse()).should().bePublic();
    }

    static String locationPattern(Class<?> clazz) {
        return String.format("\\(%s.java:\\d+\\)", quote(clazz.getSimpleName()));
    }

    static String singleLineFailureReportOf(EvaluationResult result) {
        return result.getFailureReport().toString().replaceAll("\\r?\\n", FAILURE_REPORT_NEWLINE_MARKER);
    }

    @SuppressWarnings("SameParameterValue")
    private static DescribedPredicate<JavaAnnotation<?>> annotation(Class<? extends Annotation> type) {
        return new DescribedPredicate<JavaAnnotation<?>>("@" + type.getSimpleName()) {
            @Override
            public boolean test(JavaAnnotation<?> input) {
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
        return String.format("Class <%s> does not reside in any package \\[%s\\] in %s",
                quote(clazz.getName()), quote(joinSingleQuoted(packageIdentifiers)), locationPattern(clazz));
    }

    private String doesntResideOutsideOfPackagesPatternFor(Class<?> clazz, String[] packageIdentifiers) {
        return String.format("Class <%s> does not reside outside of packages \\[%s\\] in %s",
                quote(clazz.getName()), quote(joinSingleQuoted(packageIdentifiers)), locationPattern(clazz));
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
        return stream(packages).anyMatch(p -> c.getPackage().getName().equals(p));
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
        String params = Joiner.on(", ").join(formatNamesOf(paramTypes));
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

    @SomeAnnotation
    @RuntimeRetentionAnnotation
    private static class SomeAnnotatedClass {
    }

    @interface SomeMetaAnnotation {
    }

    @SomeMetaAnnotation
    @interface SomeAnnotation {
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
