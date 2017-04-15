package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.syntax.elements.testclasses.access.ClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.accessed.ClassBeingAccessedByOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.anotheraccess.YetAnotherClassAccessingOtherClass;
import com.tngtech.archunit.lang.syntax.elements.testclasses.otheraccess.ClassAlsoAccessingOtherClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldThatEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;

public class ShouldOnlyBeAccessedByClassesThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void areNamed() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNamed(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    public void areNotNamed() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotNamed(Foo.class.getName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().haveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().dontHaveSimpleName(Foo.class.getSimpleName()))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().haveNameMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByBar.class, Bar.class,
                ClassAccessedByBaz.class, Baz.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().haveNameNotMatching(".*\\$Foo"))
                .on(ClassAccessedByFoo.class, Foo.class,
                        ClassAccessedByBar.class, Bar.class,
                        ClassAccessedByBaz.class, Baz.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessedByFoo.class, Foo.class);
    }

    @Test
    public void resideInAPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().resideInAPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().resideOutsideOfPackage("..access.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().resideInAnyPackage("..access..", "..otheraccess.."))
                .on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                        YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().resideOutsideOfPackages("..access..", "..otheraccess..")
        ).on(ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class,
                YetAnotherClassAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingOtherClass.class, ClassAlsoAccessingOtherClass.class, ClassBeingAccessedByOtherClass.class);
    }

    @Test
    public void arePublic() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().arePublic())
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
    public void areNotPublic() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().areNotPublic())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPublicClass.class, PublicClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().areProtected())
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
    public void areNotProtected() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().areNotProtected())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByProtectedClass.class, ProtectedClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().arePackagePrivate())
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
    public void areNotPackagePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().areNotPackagePrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPackagePrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().arePrivate())
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
    public void areNotPrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().areNotPrivate())
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    public void haveModifier() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().haveModifier(PRIVATE))
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
    public void dontHaveModifier() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(classes().should().onlyBeAccessed().byClassesThat().dontHaveModifier(PRIVATE))
                .on(ClassAccessedByPublicClass.class, ClassAccessedByPrivateClass.class,
                        ClassAccessedByPackagePrivateClass.class, ClassAccessedByProtectedClass.class,
                        PublicClass.class, PrivateClass.class,
                        PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessedByPrivateClass.class, PrivateClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void areAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAnnotatedWith(hasNamePredicate))
                .on(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassBeingAccessedByAnnotatedClass.class, AnnotatedClass.class);
    }

    @Test
    public void implement_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().implement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void dontImplement_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().dontImplement(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void implement_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().implement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void dontImplement_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().dontImplement(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void implement_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().implement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void dontImplement_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().dontImplement(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableTo(SomeInterface.class))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableTo(SomeInterface.class.getName()))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableTo(classWithNameOf(SomeInterface.class)))
                .on(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class,
                        SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableFrom(ClassExtendingClass.class))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableFrom(ClassExtendingClass.class.getName()))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(SimpleClass.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat().areNotAssignableFrom(classWithNameOf(ClassExtendingClass.class)))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    @Test
    public void byClassesThat_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                classes().should().onlyBeAccessed().byClassesThat(are(not(assignableFrom(classWithNameOf(ClassExtendingClass.class))))))
                .on(ClassExtendingClass.class, ClassImplementingSomeInterface.class,
                        ClassBeingAccessedByClassImplementingSomeInterface.class, SimpleClass.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassExtendingClass.class,
                ClassImplementingSomeInterface.class, ClassBeingAccessedByClassImplementingSomeInterface.class);
    }

    private DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static class ClassAccessedByFoo {
        void method() {
        }
    }

    private static class Foo {
        ClassAccessedByFoo other;

        void call() {
            other.method();
        }
    }

    private static class ClassAccessedByBar {
        String field;
    }

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

    private static class Baz {
        void call() {
            new ClassAccessedByBaz();
        }
    }

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

    private static class SimpleClass {
    }

    private static class PrivateClass {
        void call() {
            new ClassAccessedByPrivateClass();
        }
    }

    static class PackagePrivateClass {
        void call() {
            new ClassAccessedByPackagePrivateClass();
        }
    }

    protected static class ProtectedClass {
        void call() {
            new ClassAccessedByProtectedClass();
        }
    }

    public static class PublicClass {
        void call() {
            new ClassAccessedByPublicClass();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @SomeAnnotation
    private static class AnnotatedClass {
        void call() {
            new ClassBeingAccessedByAnnotatedClass();
        }
    }

    interface SomeInterface {
    }

    private static class ClassImplementingSomeInterface implements SomeInterface {
        void call() {
            new ClassBeingAccessedByClassImplementingSomeInterface();
        }
    }

    private static class ClassExtendingClass extends ClassImplementingSomeInterface {
    }

    private static class ClassBeingAccessedByClassImplementingSomeInterface {

    }
}