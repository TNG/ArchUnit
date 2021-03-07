package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.testclasses.access.ClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.anotheraccess.YetAnotherClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.innerclassaccess.ClassWithInnerClasses.ClassAccessingInnerMemberClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.innerclassaccess.ClassWithInnerClasses.InnerMemberClassBeingAccessed;
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
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldEvaluator.filterViolationCausesInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.regex.Pattern.quote;
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
    public void haveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveFullyQualifiedName(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotHaveFullyQualifiedName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.doNotHaveFullyQualifiedName(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotHaveSimpleName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.doNotHaveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveNameMatching(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveNameMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveNameNotMatching(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveNameNotMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameStartingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameStartingWith("Fo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotStartingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotStartingWith("Fo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameContaining(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameContaining("o"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotContaining(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotContaining("o"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameEndingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameEndingWith("oo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveSimpleNameNotEndingWith(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.haveSimpleNameNotEndingWith("oo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideInAPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideInAPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideOutsideOfPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideOutsideOfPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideInAnyPackage(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideInAnyPackage("..access..", "..otheraccess.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                        YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void resideOutsideOfPackages(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.resideOutsideOfPackages("..access..", "..otheraccess..")
        ).on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePublic(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePublic())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PrivateClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPublic(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPublic())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, PublicClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areProtected(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areProtected())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                ClassAccessedByPackagePrivateClass.class, PublicClass.class,
                PrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotProtected(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotProtected())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByProtectedClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePackagePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePackagePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPackagePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPackagePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPackagePrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void arePrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.arePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotPrivate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.areNotPrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void haveModifier(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.haveModifier(PRIVATE))
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, ClassAccessedByPackagePrivateClass.class,
                ClassAccessedByProtectedClass.class, PublicClass.class,
                PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotHaveModifier(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.doNotHaveModifier(PRIVATE))
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyHaveDependentClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class, MetaAnnotatedAnnotation.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName_access() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName_dependency() {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyHaveDependentClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class, MetaAnnotatedAnnotation.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMetaAnnotatedWith_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMetaAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate_access() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate_dependency() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.is(classWithNameOf(SomeAnnotation.class));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyHaveDependentClassesThat().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class,
                        ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class,
                        MetaAnnotatedAnnotation.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassBeingAccessedByMetaAnnotatedClass.class, MetaAnnotatedClass.class, MetaAnnotatedAnnotation.class,
                ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotImplement_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.doNotImplement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotImplement_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.doNotImplement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void implement_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.implement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotImplement_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.doNotImplement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableTo_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_type(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_typeName(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAssignableFrom_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areInterfaces_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areInterfaces())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, SimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotInterfaces_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotInterfaces())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByInterface.class, InterfaceAccessingAClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areEnums_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areEnums())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByEnum.class, EnumAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, SimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotEnums_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotEnums())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByEnum.class, EnumAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByEnum.class, EnumAccessingAClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnnotations_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnnotations())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByAnnotation.class, AnnotationAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingSimpleClass.class, SimpleClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnnotations_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnnotations())
                .on(ClassAccessingSimpleClass.class, SimpleClass.class, ClassBeingAccessedByAnnotation.class, AnnotationAccessingAClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassBeingAccessedByAnnotation.class, AnnotationAccessingAClass.class);
    }

    @Test
    public void areRecords_predicate() {
        // Tested in ShouldOnlyByClassesThatRecordsTest, we'll satisfy the consistency test with this quick hack
        classes().should().onlyBeAccessed().byClassesThat().areRecords();
    }

    @Test
    public void areNotRecords_predicate() {
        // Tested in ShouldOnlyByClassesThatRecordsTest, we'll satisfy the consistency test with this quick hack
        classes().should().onlyBeAccessed().byClassesThat().areNotRecords();
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areTopLevelClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areTopLevelClasses())
                .on(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class,
                        ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotTopLevelClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotTopLevelClasses())
                .on(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class,
                        ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNestedClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNestedClasses())
                .on(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotNestedClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotNestedClasses())
                .on(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areMemberClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areMemberClasses())
                .on(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotMemberClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotMemberClasses())
                .on(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingStaticNestedClass.class, StaticNestedClassBeingAccessed.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areInnerClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areInnerClasses())
                .on(ClassAccessingInnerMemberClass.class, InnerMemberClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotInnerClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotInnerClasses())
                .on(ClassAccessingInnerMemberClass.class, InnerMemberClassBeingAccessed.class,
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(
                ClassAccessingInnerMemberClass.class, InnerMemberClassBeingAccessed.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areAnonymousClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areAnonymousClasses())
                .on(ClassAccessingAnonymousClass_Reference, anonymousClassBeingAccessed.getClass(),
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotAnonymousClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotAnonymousClasses())
                .on(ClassAccessingAnonymousClass_Reference, anonymousClassBeingAccessed.getClass(),
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingAnonymousClass_Reference, anonymousClassBeingAccessed.getClass());
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areLocalClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areLocalClasses())
                .on(ClassAccessingLocalClass_Reference, ClassBeingAccessedByLocalClass.getLocalClass(),
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void areNotLocalClasses_predicate(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(
                classesShouldOnlyBeBy.areNotLocalClasses())
                .on(ClassAccessingLocalClass_Reference, ClassBeingAccessedByLocalClass.getLocalClass(),
                        ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassAccessingLocalClass_Reference, ClassBeingAccessedByLocalClass.getLocalClass());
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void belongToAnyOf(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterViolationCausesInFailureReport(
                classesShouldOnlyBeBy.belongToAnyOf(ClassWithInnerClasses.class))
                .on(ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class,
                        ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                        AnotherClassWithInnerClasses.class, AnotherClassWithInnerClasses.InnerClass.class,
                        AnotherClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                        ClassBeingAccessedByInnerClass.class);

        assertThatTypes(classes).matchInAnyOrder(AnotherClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void doNotBelongToAnyOf(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterViolationCausesInFailureReport(
                classesShouldOnlyBeBy.doNotBelongToAnyOf(ClassWithInnerClasses.class))
                .on(ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class,
                        ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                        AnotherClassWithInnerClasses.class, AnotherClassWithInnerClasses.InnerClass.class,
                        AnotherClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                        ClassBeingAccessedByInnerClass.class);

        assertThatTypes(classes).matchInAnyOrder(ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class);
    }

    private static class Data_of_containAnyMembersThat {
        @SuppressWarnings("unused")
        static class OkayOrigin {
            static {
                System.out.println("static initializer");
            }

            Object aField;

            OkayOrigin(Object aParam) {
            }

            void aMethod() {
                new Target();
            }
        }

        @SuppressWarnings("unused")
        static class ViolatingOrigin {
            String bField;

            ViolatingOrigin(String bParam) {
            }

            void bMethod() {
                new Data_of_containAnyMembersThat.Target();
            }
        }

        static class Target {
        }
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyMembersThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.containAnyMembersThat(have(name("aField"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyFieldsThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.containAnyFieldsThat(have(name("aField"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyCodeUnitsThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.containAnyCodeUnitsThat(have(name("aMethod"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyMethodsThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(classesShouldOnlyBeBy.containAnyMethodsThat(have(name("aMethod"))))
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyConstructorsThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        ArchRule rule = classesShouldOnlyBeBy.containAnyConstructorsThat(have(fullNameMatching(".*" + quote(Object.class.getName()) + ".*")));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
    }

    @Test
    @UseDataProvider("should_only_be_by_rule_starts")
    public void containAnyStaticInitializersThat(ClassesThat<ClassesShouldConjunction> classesShouldOnlyBeBy) {
        ArchRule rule = classesShouldOnlyBeBy.containAnyStaticInitializersThat(have(fullNameMatching(quote(Data_of_containAnyMembersThat.OkayOrigin.class.getName()) + ".*")));
        Set<JavaClass> classes = filterClassesAppearingInFailureReport(rule)
                .on(Data_of_containAnyMembersThat.OkayOrigin.class, Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);

        assertThatTypes(classes).matchInAnyOrder(Data_of_containAnyMembersThat.ViolatingOrigin.class, Data_of_containAnyMembersThat.Target.class);
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

        assertThatTypes(classes).matchInAnyOrder(ClassExtendingClass.class,
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

        assertThatTypes(classes).matchInAnyOrder(ClassDependingViaFieldType.class,
                ClassDependingViaMethodParameter.class, ClassDependingViaImplementing.class);

        classes = filterViolationOriginsInFailureReport.apply(
                classes().should().onlyBeAccessed().byClassesThat(have(not(nameMatching(".*DependingVia.*")))));

        assertThat(classes).isEmpty();
    }

    private static DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static class ClassAccessingItself {
        private final String field;

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

    @SuppressWarnings({"unused"})
    private static class ClassWithInnerClasses {
        private static class InnerClass {
            private static class EvenMoreInnerClass {
                ClassBeingAccessedByInnerClass classBeingAccessedByInnerClass;

                void access() {
                    classBeingAccessedByInnerClass.callMe();
                }
            }
        }
    }

    @SuppressWarnings({"unused"})
    private static class AnotherClassWithInnerClasses {
        private static class InnerClass {
            private static class EvenMoreInnerClass {
                ClassBeingAccessedByInnerClass classBeingAccessedByInnerClass;

                void access() {
                    classBeingAccessedByInnerClass.callMe();
                }
            }
        }
    }

    private static class ClassBeingAccessedByInnerClass {
        void callMe() {
        }
    }

    private static class ClassBeingAccessedByEnum {
    }

    @SuppressWarnings({"unused"})
    private enum EnumAccessingAClass {
        VALUE;

        static void access() {
            new ClassBeingAccessedByEnum();
        }
    }

    private static class ClassBeingAccessedByAnnotation {
    }

    @SuppressWarnings({"unused"})
    private @interface AnnotationAccessingAClass {
        ClassBeingAccessedByAnnotation access = new ClassBeingAccessedByAnnotation();
    }

    private static class ClassAccessingStaticNestedClass {
        @SuppressWarnings("unused")
        void access() {
            new StaticNestedClassBeingAccessed();
        }
    }

    private static class StaticNestedClassBeingAccessed {
    }

    // This must be loaded via Reflection, otherwise the test will be tainted by the dependency on the class object
    private static final Class<?> ClassAccessingAnonymousClass_Reference = classForName("com.tngtech.archunit.lang.syntax.elements.ShouldOnlyByClassesThatTest$ClassAccessingAnonymousClass");
    private static class ClassAccessingAnonymousClass {
        @SuppressWarnings("unused")
        void access() {
            anonymousClassBeingAccessed.run();
        }
    }

    private static final Runnable anonymousClassBeingAccessed = new Runnable() {
        @Override
        public void run() {
            new ClassAccessingAnonymousClass();
        }
    };

    // This must be loaded via Reflection, otherwise the test will be tainted by the dependency on the class object
    private static final Class<?> ClassAccessingLocalClass_Reference = classForName("com.tngtech.archunit.lang.syntax.elements.ShouldOnlyByClassesThatTest$ClassBeingAccessedByLocalClass");

    private static class ClassBeingAccessedByLocalClass {
        static Class<?> getLocalClass() {
            @SuppressWarnings({"unused", "InstantiationOfUtilityClass"})
            class LocalClass {
                void access() {
                    new ClassBeingAccessedByLocalClass();
                }
            }

            // This must be loaded via Reflection, otherwise the test will be tainted by the dependency on the class object
            return classForName("com.tngtech.archunit.lang.syntax.elements.ShouldOnlyByClassesThatTest$ClassBeingAccessedByLocalClass$1LocalClass");
        }
    }
}
