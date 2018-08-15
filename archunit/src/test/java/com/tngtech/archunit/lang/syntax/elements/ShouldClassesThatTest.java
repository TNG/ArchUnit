package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ShouldClassesThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @DataProvider
    public static Object[][] no_classes_should_that_rule_starts() {
        return testForEach(
                noClasses().should().accessClassesThat(),
                noClasses().should().dependOnClassesThat());
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveFullyQualifiedName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontHaveFullyQualifiedName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.dontHaveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontHaveSimpleName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.dontHaveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveNameMatching(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveNameMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveNameNotMatching(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveNameNotMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameStartingWith(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameNotStartingWith(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameContaining(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameNotContaining(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameEndingWith(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveSimpleNameNotEndingWith(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.haveSimpleNameNotEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void resideInAPackage(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideInAPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void resideOutsideOfPackage(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideOutsideOfPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void resideInAnyPackage(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingConstructor.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void resideOutsideOfPackages(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void arePublic(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotPublic(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPrivateClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areProtected(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingProtectedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotProtected(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                ClassAccessingPackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void arePackagePrivate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotPackagePrivate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void arePrivate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.arePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotPrivate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.areNotPrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void haveModifier(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.haveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontHaveModifier(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(noClassesShouldThatRuleStart.dontHaveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAnnotatedWith_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAnnotatedWith_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAnnotatedWith_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAnnotatedWith_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAnnotatedWith_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAnnotatedWith_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areMetaAnnotatedWith_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingMetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotMetaAnnotatedWith_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areMetaAnnotatedWith_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingMetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotMetaAnnotatedWith_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areMetaAnnotatedWith_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingMetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotMetaAnnotatedWith_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingMetaAnnotatedClass.class, ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void implement_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontImplement_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.dontImplement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void implement_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontImplement_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.dontImplement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void implement_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.implement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void dontImplement_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.dontImplement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableTo_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableTo_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableTo_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableTo_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableTo_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableTo_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableFrom_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableFrom_type(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableFrom_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableFrom_typeName(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areAssignableFrom_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotAssignableFrom_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areInterfaces_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingCollection.class);
    }

    @Test
    @UseDataProvider("no_classes_should_that_rule_starts")
    public void areNotInterfaces_predicate(ClassesShouldThat noClassesShouldThatRuleStart) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClassesShouldThatRuleStart.areNotInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingSimpleClass.class);
    }

    @DataProvider
    public static Object[][] no_classes_should_predicate_rule_starts() {
        return testForEach(
                noClasses().should().accessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))),
                noClasses().should().dependOnClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class)))))
        );
    }

    @Test
    @UseDataProvider("no_classes_should_predicate_rule_starts")
    public void shouldThat_predicate(ArchRule rule) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void dependOnClassesThat_reports_all_dependencies() {
        Function<ArchRule, Set<JavaClass>> filterClassesInFailureReport = new Function<ArchRule, Set<JavaClass>>() {
            @Override
            public Set<JavaClass> apply(ArchRule rule) {
                return filterClassesAppearingInFailureReport(rule)
                        .on(ClassHavingFieldOfTypeList.class, ClassHavingMethodParameterOfTypeString.class,
                                ClassHavingConstructorParameterOfTypeCollection.class, ClassImplementingSerializable.class,
                                ClassHavingReturnTypeArrayList.class);
            }
        };

        Set<JavaClass> classes = filterClassesInFailureReport.apply(
                noClasses().should().dependOnClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThatClasses(classes).matchInAnyOrder(
                ClassHavingFieldOfTypeList.class, ClassHavingMethodParameterOfTypeString.class,
                ClassHavingReturnTypeArrayList.class, ClassImplementingSerializable.class);

        classes = filterClassesInFailureReport.apply(
                noClasses().should().accessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))));

        assertThat(classes).isEmpty();
    }

    private static DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static class ClassAccessingList {
        @SuppressWarnings("unused")
        void call(List<?> list) {
            list.size();
        }
    }

    private static class ClassHavingFieldOfTypeList {
        @SuppressWarnings("unused")
        List<?> list;
    }

    private static class ClassAccessingArrayList {
        @SuppressWarnings("unused")
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
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void call(String string) {
        }
    }

    private static class ClassAccessingCollection {
        @SuppressWarnings("unused")
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
}