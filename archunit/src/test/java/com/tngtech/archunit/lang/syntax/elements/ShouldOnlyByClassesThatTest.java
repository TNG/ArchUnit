package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.testclasses.access.ClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.anotheraccess.YetAnotherClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.otheraccess.ClassAlsoAccessingOtherClass;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterViolationCausesInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ShouldOnlyByClassesThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @DataProvider
    public static Object[][] should_only_be_by_rule_starts() {
        return testForEach(
                classes().should().onlyBeAccessed().byClassesThat(),
                classes().should().onlyHaveDependentClassesThat()
        );
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveFullyQualifiedName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveFullyQualifiedName(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontHaveFullyQualifiedName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.dontHaveFullyQualifiedName(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontHaveSimpleName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.dontHaveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveNameMatching(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveNameMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveNameNotMatching(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveNameNotMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameStartingWith(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameStartingWith("Fo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotStartingWith(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotStartingWith("Fo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameContaining(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameContaining("o"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotContaining(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotContaining("o"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameEndingWith(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameEndingWith("oo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotEndingWith(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotEndingWith("oo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideInAPackage(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideInAPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideOutsideOfPackage(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideOutsideOfPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideInAnyPackage(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideInAnyPackage("..access..", "..otheraccess.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                        YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideOutsideOfPackages(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideOutsideOfPackages("..access..", "..otheraccess..")
        ).on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePublic(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePublic())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PrivateClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPublic(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPublic())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, PublicClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areProtected(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areProtected())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                ClassAccessedByPackagePrivateClass.class, PublicClass.class,
                PrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotProtected(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotProtected())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByProtectedClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePackagePrivate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePackagePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPackagePrivate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPackagePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPackagePrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePrivate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPrivate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveModifier(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.haveModifier(PRIVATE))
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontHaveModifier(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.dontHaveModifier(PRIVATE))
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotMetaAnnotatedWith_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotMetaAnnotatedWith_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotMetaAnnotatedWith_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontImplement_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.dontImplement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontImplement_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.dontImplement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void dontImplement_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.dontImplement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_type(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_typeName(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areInterfaces_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areInterfaces())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, SimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotInterfaces_predicate(ClassesShouldThat classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotInterfaces())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);
    }

    @DataProvider
    public static Object[][] byClassesThat_predicate_rules() {
        return testForEach(
                classes().should().onlyBeAccessed().byClassesThat(are(not(assignableFrom(classWithNameOf(ClassExtendingClass.class))))),
                classes().should().onlyHaveDependentClassesThat(are(not(assignableFrom(classWithNameOf(ClassExtendingClass.class))))));
    }

    @Test
    @UseDataProvider("byClassesThat_predicate_rules")
    public void classesThat_predicate(ArchRule rule) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void onlyHaveDependentClassesThat_reports_all_dependents() {
        Function<ArchRule, Set<JavaClass>> filterViolationOriginsInFailureReport = new Function<ArchRule, Set<JavaClass>>() {
            @Override
            public Set<JavaClass> apply(ArchRule rule) {
                return filterViolationCausesInFailureReport(rule)
                        .on(ClassBeingDependedOnByFieldType.class,
                                ClassDependingViaFieldType.class,
                                ClassBeingDependedOnByMethodParameterType.class,
                                ClassDependingViaMethodParameter.class,
                                InterfaceBeingDependedOnByImplementing.class,
                                ClassDependingViaImplementing.class);
            }
        };

        Set<JavaClass> classes = filterViolationOriginsInFailureReport.apply(
                classes().should().onlyHaveDependentClassesThat(have(not(nameMatching(".*DependingVia.*")))));

        assertThatClasses(classes).matchInAnyOrder(ClassDependingViaFieldType.class,
                ClassDependingViaMethodParameter.class, ClassDependingViaImplementing.class);

        classes = filterViolationOriginsInFailureReport.apply(
                classes().should().onlyBeAccessed().byClassesThat(have(not(nameMatching(".*DependingVia.*")))));

        assertThat(classes).isEmpty();
    }

    @DataProvider
    public static Object[][] rule_starts_where_a_class_accesses_itself() {
        return testForEach(
                classes().should().onlyBeAccessed().byClassesThat(classWithNameOf(ClassAccessingClassAccessingItself.class)),
                classes().should().onlyHaveDependentClassesThat(classWithNameOf(ClassAccessingClassAccessingItself.class)));
    }

    @Test
    @UseDataProvider("rule_starts_where_a_class_accesses_itself")
    public void accesses_by_the_class_itself_are_ignored(ArchRule rule) {
        rule.check(new ClassFileImporter().importClasses(
                ClassAccessingItself.class, ClassAccessingClassAccessingItself.class));
    }

    private static DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static class ClassAccessedByFoo {
        void method() {
        }
    }

    @SuppressWarnings("unused")
    private static class Foo {
        ClassAccessedByFoo other;

        void call() {
            other.method();
        }
    }

    private static class ClassAccessedByBar {
        String field;
    }

    @SuppressWarnings("unused")
    private static class Bar {
        ClassAccessedByBar other;

        void call() {
            other.field = "bar";
        }
    }

    private static class ClassAccessedByBaz {
        ClassAccessedByBaz() {
        }
    }

    @SuppressWarnings("unused")
    private static class Baz {
        void call() {
            new ClassAccessedByBaz();
        }
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingSimpleClass {
        void call() {
            new SimpleClass();
        }
    }

    private static class ClassAccessedByPrivateClass {
    }

    private static class ClassAccessedByPackagePrivateClass {
    }

    private static class ClassAccessedByProtectedClass {
    }

    private static class ClassAccessedByPublicClass {
    }

    private static class ClassBeingAccessedByAnnotatedClass {
    }

    private static class ClassBeingAccessedByMetaAnnotatedClass {
    }

    private static class SimpleClass {
    }

    @SuppressWarnings("unused")
    private static class PrivateClass {
        void call() {
            new ClassAccessedByPrivateClass();
        }
    }

    @SuppressWarnings("unused")
    static class PackagePrivateClass {
        void call() {
            new ClassAccessedByPackagePrivateClass();
        }
    }

    @SuppressWarnings("unused")
    protected static class ProtectedClass {
        void call() {
            new ClassAccessedByProtectedClass();
        }
    }

    @SuppressWarnings("unused")
    public static class PublicClass {
        void call() {
            new ClassAccessedByPublicClass();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SomeAnnotation
    private @interface MetaAnnotatedAnnotation {
    }

    @SuppressWarnings("unused")
    @SomeAnnotation
    private static class AnnotatedClass {
        void call() {
            new ClassBeingAccessedByAnnotatedClass();
        }
    }

    @SuppressWarnings("unused")
    @MetaAnnotatedAnnotation
    private static class MetaAnnotatedClass {
        void call() {
            new ClassBeingAccessedByMetaAnnotatedClass();
        }
    }

    interface SomeInterface {
    }

    private static class ClassBeingAccessedByInterface {
        static final Object SOME_CONSTANT = "Some value";
    }

    @SuppressWarnings("unused")
    interface InterfaceAccessingAClass {
        Object SOME_CONSTANT = ClassBeingAccessedByInterface.SOME_CONSTANT;
    }

    @SuppressWarnings("unused")
    private static class ClassImplementingSomeInterface implements SomeInterface {
        void call() {
            new ClassBeingAccessedByClassImplementingSomeInterface();
        }
    }

    private static class ClassExtendingClass extends ClassImplementingSomeInterface {
    }

    private static class ClassBeingAccessedByClassImplementingSomeInterface {
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingItself {
        private String field;

        ClassAccessingItself(String field) {
            this.field = field;
        }
    }

    @SuppressWarnings("unused")
    private static class ClassAccessingClassAccessingItself {
        void call() {
            new ClassAccessingItself("foo");
        }
    }

    private static class ClassBeingDependedOnByFieldType {
    }

    @SuppressWarnings("unused")
    private static class ClassDependingViaFieldType {
        ClassBeingDependedOnByFieldType field;
    }

    private static class ClassBeingDependedOnByMethodParameterType {
    }

    @SuppressWarnings("unused")
    private static class ClassDependingViaMethodParameter {
        void foo(ClassBeingDependedOnByMethodParameterType parameter) {
        }
    }

    private interface InterfaceBeingDependedOnByImplementing {
    }

    private static class ClassDependingViaImplementing implements InterfaceBeingDependedOnByImplementing {
    }
}