package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatRule;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static java.util.regex.Pattern.quote;

public class ShouldClassesThatTest {

    static Stream<ClassesThat<ClassesShouldConjunction>> no_classes_should_that_rule_starts() {
        return Stream.of(
                noClasses().should().accessClassesThat(),
                noClasses().should().dependOnClassesThat());
    }

    static Stream<ClassesThat<ClassesShouldConjunction>> classes_should_only_that_rule_starts() {
        return Stream.of(
                classes().should().onlyAccessClassesThat(),
                classes().should().onlyDependOnClassesThat());
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotHaveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotHaveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotHaveSimpleName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotHaveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveNameMatching(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveNameMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveNameNotMatching(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveNameNotMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameStartingWith(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameNotStartingWith(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameContaining(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameNotContaining(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameEndingWith(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveSimpleNameNotEndingWith(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void resideInAPackage(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideInAPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void resideOutsideOfPackage(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideOutsideOfPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void resideInAnyPackage(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingConstructor.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void resideOutsideOfPackages(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void arePublic(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotPublic(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPrivateClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areProtected(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotProtected(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                ClassAccessingPackagePrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void arePackagePrivate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPackagePrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotPackagePrivate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void arePrivate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotPrivate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void haveModifier(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.haveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotHaveModifier(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.doNotHaveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    private static class Data_of_containAnyMembersThat {
        @SuppressWarnings("unused")
        static class ViolatingOrigin {
            void call() {
                new ViolatingTarget("").aMethod();
            }
        }

        @SuppressWarnings("unused")
        static class ViolatingTarget {
            static {
                System.out.println("static initializer");
            }

            Object aField;

            ViolatingTarget(Object aParam) {
            }

            void aMethod() {
            }
        }

        @SuppressWarnings("unused")
        static class OkayOrigin {
            void call() {
                new Data_of_containAnyMembersThat.OkayTarget("").bMethod();
            }
        }

        @SuppressWarnings("unused")
        static class OkayTarget {
            String bField;

            OkayTarget(String bParam) {
            }

            void bMethod() {
            }
        }
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyMembersThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.containAnyMembersThat(have(name("aField"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyFieldsThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.containAnyFieldsThat(have(name("aField"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyCodeUnitsThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.containAnyCodeUnitsThat(have(name("aMethod"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyMethodsThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.containAnyMethodsThat(have(name("aMethod"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyConstructorsThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        ArchRule rule = noClassesShouldThatRuleStart.containAnyConstructorsThat(have(fullNameMatching(".*" + quote(Object.class.getName()) + ".*")));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void containAnyStaticInitializersThat(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        ArchRule rule = noClassesShouldThatRuleStart.containAnyStaticInitializersThat(
                have(fullNameMatching(quote(Data_of_containAnyMembersThat.ViolatingTarget.class.getName()) + ".*")));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class,
                        Data_of_containAnyMembersThat.OkayTarget.class, Data_of_containAnyMembersThat.ViolatingTarget.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.ViolatingTarget.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areMetaAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().dependOnClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areMetaAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().dependOnClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areMetaAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate_access() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate_dependency() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().dependOnClassesThat().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void implement_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotImplement_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotImplement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void implement_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotImplement_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotImplement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void implement_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotImplement_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotImplement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableTo_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableTo_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableFrom_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableFrom_type(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areInterfaces_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingCollection.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotInterfaces_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areEnums_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areEnums())
                .on(ClassAccessingEnum.class, ClassAccessingString.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingEnum.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotEnums_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotEnums())
                .on(ClassAccessingEnum.class, ClassAccessingString.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAnnotations_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotations())
                .on(ClassAccessingAnnotation.class, ClassAccessingString.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAnnotations_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotations())
                .on(ClassAccessingAnnotation.class, ClassAccessingString.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @Test
    public void areRecords_predicate() {
        // Tested in ShouldClassesThatRecordsTest, we'll satisfy the consistency test with this quick hack
        noClasses().should().accessClassesThat().areRecords();
    }

    @Test
    public void areNotRecords_predicate() {
        // Tested in ShouldClassesThatRecordsTest, we'll satisfy the consistency test with this quick hack
        noClasses().should().accessClassesThat().areNotRecords();
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areTopLevelClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areTopLevelClasses())
                .on(ClassAccessingTopLevelClass.class, ClassAccessingStaticNestedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotTopLevelClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotTopLevelClasses())
                .on(ClassAccessingTopLevelClass.class, ClassAccessingStaticNestedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNestedClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNestedClasses())
                .on(ClassAccessingStaticNestedClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotNestedClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotNestedClasses())
                .on(ClassAccessingStaticNestedClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areMemberClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMemberClasses())
                .on(ClassAccessingStaticNestedClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotMemberClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotMemberClasses())
                .on(ClassAccessingStaticNestedClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areInnerClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areInnerClasses())
                .on(ClassAccessingInnerMemberClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingInnerMemberClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotInnerClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotInnerClasses())
                .on(ClassAccessingInnerMemberClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areAnonymousClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnonymousClasses())
                .on(ClassAccessingAnonymousClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingAnonymousClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotAnonymousClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnonymousClasses())
                .on(ClassAccessingAnonymousClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areLocalClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areLocalClasses())
                .on(ClassAccessingLocalClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingLocalClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void areNotLocalClasses_predicate(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotLocalClasses())
                .on(ClassAccessingLocalClass.class, ClassAccessingTopLevelClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingTopLevelClass.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void belongToAnyOf(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.belongToAnyOf(ClassWithInnerClasses.class, String.class))
                .on(ClassAccessingNestedInnerClass.class, ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class,
                        ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingNestedInnerClass.class,
                ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class, ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_that_rule_starts")
    void doNotBelongToAnyOf(ClassesThat<ClassesShouldConjunction> noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.doNotBelongToAnyOf(ClassWithInnerClasses.class, String.class))
                .on(ClassAccessingNestedInnerClass.class, ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class,
                        ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotHaveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.doNotHaveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotHaveSimpleName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.doNotHaveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveNameMatching(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveNameMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveNameNotMatching(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveNameNotMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameStartingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameNotStartingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameNotStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameContaining(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameNotContaining(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameNotContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameEndingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveSimpleNameNotEndingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.haveSimpleNameNotEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_resideInAPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.resideInAPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_resideOutsideOfPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.resideOutsideOfPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_resideInAnyPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_resideOutsideOfPackages(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingConstructor.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_arePublic(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.arePublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPrivateClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotPublic(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.areNotPublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areProtected(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.areProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                ClassAccessingPackagePrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotProtected(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.areNotProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_arePackagePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.arePackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotPackagePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.areNotPackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPackagePrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_arePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.arePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotPrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.areNotPrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_haveModifier(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.haveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotHaveModifier(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyThatRuleStart.doNotHaveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_type_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyAccessClassesThat().areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_type_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyDependOnClassesThat().areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotMetaAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_typeName_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyAccessClassesThat().areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_typeName_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyDependOnClassesThat().areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotMetaAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_predicate_access() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyAccessClassesThat().areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class);
    }

    @Test
    public void only_areMetaAnnotatedWith_predicate_dependency() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyDependOnClassesThat().areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotMetaAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_implement_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.implement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotImplement_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.doNotImplement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_implement_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.implement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotImplement_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.doNotImplement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_implement_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.implement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_doNotImplement_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.doNotImplement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableTo_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableTo_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatType(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableFrom_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableFrom_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areInterfaces_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingSimpleClass.class);
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_that_rule_starts")
    void only_areNotInterfaces_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyThatRuleStart.areNotInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingCollection.class);
    }

    static Stream<ArchRule> no_classes_should_predicate_rule_starts() {
        return Stream.of(
                noClasses().should().accessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))),
                noClasses().should().dependOnClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class)))))
        );
    }

    @ParameterizedTest
    @MethodSource("no_classes_should_predicate_rule_starts")
    void shouldThat_predicate(ArchRule rule) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    static Stream<ArchRule> classes_should_only_predicate_rule_starts() {
        return Stream.of(
                classes().should().onlyAccessClassesThat(are(assignableFrom(classWithNameOf(Collection.class)))),
                classes().should().onlyDependOnClassesThat(are(assignableFrom(classWithNameOf(Collection.class))))
        );
    }

    @ParameterizedTest
    @MethodSource("classes_should_only_predicate_rule_starts")
    void shouldThatOnly_predicate(ArchRule rule) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void dependOnClassesThat_reports_all_dependencies() {
        Function<ArchRule, Set<JavaClass>> filterClassesInFailureReport = rule -> filterClassesAppearingInFailureReport(rule)
                .on(ClassHavingFieldOfTypeList.class, ClassHavingMethodParameterOfTypeString.class,
                        ClassHavingConstructorParameterOfTypeCollection.class, ClassImplementingSerializable.class,
                        ClassHavingReturnTypeArrayList.class);

        Set<JavaClass> classes = filterClassesInFailureReport.apply(
                noClasses().should().dependOnClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThatTypes(classes).matchInAnyOrder(
                ClassHavingFieldOfTypeList.class, ClassHavingMethodParameterOfTypeString.class,
                ClassHavingReturnTypeArrayList.class, ClassImplementingSerializable.class);

        classes = filterClassesInFailureReport.apply(
                noClasses().should().accessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThat(classes).isEmpty();
    }

    @Test
    public void onlyDependOnClassesThat_reports_all_dependencies() {
        Function<ArchRule, Set<JavaClass>> filterClassesInFailureReport = rule -> filterClassesAppearingInFailureReport(rule)
                .on(ClassHavingFieldOfTypeList.class, ClassHavingMethodParameterOfTypeString.class,
                        ClassHavingConstructorParameterOfTypeCollection.class, ClassImplementingSerializable.class,
                        ClassHavingReturnTypeArrayList.class);

        Set<JavaClass> classes = filterClassesInFailureReport.apply(
                classes().should().onlyDependOnClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThatTypes(classes).matchInAnyOrder(ClassHavingConstructorParameterOfTypeCollection.class);

        classes = filterClassesInFailureReport.apply(
                classes().should().onlyAccessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThat(classes).isEmpty();
    }

    private static class TransitivelyDependOnClassesThatTestCases {
        @SuppressWarnings("unused")
        static class TestClass {
            DirectlyDependentClass1 directDependency1;
            DirectlyDependentClass2 directDependency2;
            DirectlyDependentClass3 directDependency3;
        }

        @SuppressWarnings("unused")
        static class TestClassNotViolatingBecauseOnlyDependingOnOtherSelectedClass {
            TestClass testClass;
        }

        @SuppressWarnings("unused")
        static class DirectlyDependentClass1 {
            Level1TransitivelyDependentClass1 transitiveDependency1;
        }

        @SuppressWarnings("unused")
        static class DirectlyDependentClass2 {
            DirectlyDependentClass1 otherDependency;
            Level2TransitivelyDependentClass2 transitiveDependency2;
        }

        static class DirectlyDependentClass3 {
        }

        @SuppressWarnings("unused")
        static class Level1TransitivelyDependentClass1 {
             Level2TransitivelyDependentClass1 transitiveDependency1;
        }

        static class Level2TransitivelyDependentClass1 {
        }

        @SuppressWarnings("unused")
        static class Level2TransitivelyDependentClass2 {
            Level2TransitivelyDependentClass1 transitiveDependency1;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void transitivelyDependOnClassesThat_reports_all_transitive_dependencies(boolean viaPredicate) {
        Class<?> testClass1 = TransitivelyDependOnClassesThatTestCases.TestClass.class;
        Class<?> testClass2 = TransitivelyDependOnClassesThatTestCases.TestClassNotViolatingBecauseOnlyDependingOnOtherSelectedClass.class;
        Class<?> directlyDependentClass1 = TransitivelyDependOnClassesThatTestCases.DirectlyDependentClass1.class;
        Class<?> directlyDependentClass2 = TransitivelyDependOnClassesThatTestCases.DirectlyDependentClass2.class;
        Class<?> directlyDependentClass3 = TransitivelyDependOnClassesThatTestCases.DirectlyDependentClass3.class;
        Class<?> level1TransitivelyDependentClass1 = TransitivelyDependOnClassesThatTestCases.Level1TransitivelyDependentClass1.class;
        Class<?> level2TransitivelyDependentClass1 = TransitivelyDependOnClassesThatTestCases.Level2TransitivelyDependentClass1.class;
        Class<?> level2TransitivelyDependentClass2 = TransitivelyDependOnClassesThatTestCases.Level2TransitivelyDependentClass2.class;
        Class<?>[] matchingTransitivelyDependentClasses =
                new Class<?>[]{level2TransitivelyDependentClass1, level2TransitivelyDependentClass2, directlyDependentClass3};

        JavaClasses classes = new ClassFileImporter().importClasses(
                testClass1,
                testClass2,
                directlyDependentClass1,
                directlyDependentClass2,
                directlyDependentClass3,
                level1TransitivelyDependentClass1,
                level2TransitivelyDependentClass1,
                level2TransitivelyDependentClass2
        );

        ClassesShould noClassesShould = noClasses().that().haveSimpleNameStartingWith("TestClass").should();
        ArchRule rule = viaPredicate
                ? noClassesShould.transitivelyDependOnClassesThat(Predicates.belongToAnyOf(matchingTransitivelyDependentClasses))
                : noClassesShould.transitivelyDependOnClassesThat().belongToAnyOf(matchingTransitivelyDependentClasses);

        assertThatRule(rule).checking(classes)
                .hasNumberOfViolations(3)
                .hasViolationMatching(String.format(".*<%s> transitively depends on <(?:%s|%s)> by \\[%s->.*\\] in .*",
                        quote(testClass1.getName()),
                        quote(level2TransitivelyDependentClass1.getName()),
                        quote(level2TransitivelyDependentClass2.getName()),
                        quote(directlyDependentClass2.getName())
                ))
                .hasViolationMatching(String.format(".*<%s> transitively depends on <%s> by \\[%s->%s->%s\\] in .*",
                        quote(testClass1.getName()),
                        quote(level2TransitivelyDependentClass1.getName()),
                        quote(directlyDependentClass1.getName()),
                        quote(level1TransitivelyDependentClass1.getName()),
                        quote(level2TransitivelyDependentClass1.getName())
                ))
                .hasViolationMatching(String.format(".*<%s> depends on <%s> in .*",
                        quote(testClass1.getName()),
                        quote(directlyDependentClass3.getName())
                ));
    }

    private static DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static class ClassAccessingList {
        @SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
        void call(List<?> list) {
            list.size();
        }
    }

    private static class ClassHavingFieldOfTypeList {
        @SuppressWarnings("unused")
        List<?> list;
    }

    private static class ClassAccessingArrayList {
        @SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
        void call(ArrayList<?> list) {
            list.size();
        }
    }

    private static class ClassHavingReturnTypeArrayList {
        @SuppressWarnings("unused")
        ArrayList<?> call() {
            return null;
        }
    }

    private static class ClassAccessingString {
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void call() {
            "string".length();
        }
    }

    private static class ClassHavingMethodParameterOfTypeString {
        @SuppressWarnings({"unused"})
        void call(String string) {
        }
    }

    private static class ClassAccessingCollection {
        @SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
        void call(Collection<?> collection) {
            collection.size();
        }
    }

    private static class ClassHavingConstructorParameterOfTypeCollection {
        @SuppressWarnings("unused")
        ClassHavingConstructorParameterOfTypeCollection(Collection<?> collection) {
        }
    }

    private static class ClassAccessingIterable {
        @SuppressWarnings("unused")
        void call(Iterable<?> iterable) {
            iterable.iterator();
        }
    }

    private static class ClassImplementingSerializable implements Serializable {
    }

    private static class ClassAccessingConstructor {
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void call(Constructor<?> constructor) {
            constructor.getModifiers();
        }
    }

    private static class ClassAccessingSimpleClass {
        @SuppressWarnings("unused")
        void call() {
            new SimpleClass();
        }
    }

    private static class ClassAccessingPrivateClass {
        @SuppressWarnings("unused")
        void call() {
            new PrivateClass();
        }
    }

    private static class ClassAccessingPackagePrivateClass {
        @SuppressWarnings("unused")
        void call() {
            new PackagePrivateClass();
        }
    }

    private static class ClassAccessingProtectedClass {
        @SuppressWarnings("unused")
        void call() {
            new ProtectedClass();
        }
    }

    private static class ClassAccessingPublicClass {
        @SuppressWarnings("unused")
        void call() {
            new PublicClass();
        }
    }

    private static class ClassAccessingAnnotatedClass {
        @SuppressWarnings("unused")
        void call() {
            new AnnotatedClass();
        }
    }

    private static class ClassAccessingMetaAnnotatedClass {
        @SuppressWarnings("unused")
        void call() {
            new MetaAnnotatedClass();
        }
    }

    private static class SimpleClass {
    }

    private static class PrivateClass {
    }

    @SuppressWarnings("WeakerAccess")
    static class PackagePrivateClass {
    }

    @SuppressWarnings("WeakerAccess")
    protected static class ProtectedClass {
    }

    @SuppressWarnings("WeakerAccess")
    public static class PublicClass {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SomeAnnotation
    private @interface MetaAnnotatedAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
    }

    @MetaAnnotatedAnnotation
    private static class MetaAnnotatedClass {
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingNestedInnerClass {
        ClassWithInnerClasses.InnerClass.EvenMoreInnerClass evenMoreInnerClass;

        void access() {
            evenMoreInnerClass.callMe();
        }
    }

    private static class ClassWithInnerClasses {
        private static class InnerClass {
            private static class EvenMoreInnerClass {
                void callMe() {
                }
            }
        }
    }

    private static class ClassAccessingEnum {
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void access() {
            StandardCopyOption.ATOMIC_MOVE.name();
        }
    }

    private static class ClassAccessingAnnotation {
        Deprecated deprecated;

        @SuppressWarnings({"unused"})
        void access() {
            deprecated.annotationType();
        }
    }

    private static class ClassAccessingTopLevelClass {
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void access() {
            String.valueOf(123);
        }
    }

    private static class ClassAccessingStaticNestedClass {
        @SuppressWarnings("unused")
        void access() {
            StaticNestedClass.access();
        }
    }

    private static class StaticNestedClass {
        static void access() {
        }
    }

    private static class ClassAccessingInnerMemberClass {
        @SuppressWarnings("unused")
        void access() {
            new InnerMemberClass().access();
        }

        @SuppressWarnings("InnerClassMayBeStatic")
        private class InnerMemberClass {
            void access() {
            }
        }
    }

    private static class ClassAccessingAnonymousClass {
        @SuppressWarnings("unused")
        void access() {
            new Serializable() {
                void access() {
                }
            }.access();
        }
    }

    private static class ClassAccessingLocalClass {
        @SuppressWarnings("unused")
        void access() {
            class LocalClass {
                void access() {
                }
            }
            new LocalClass().access();
        }
    }
}
