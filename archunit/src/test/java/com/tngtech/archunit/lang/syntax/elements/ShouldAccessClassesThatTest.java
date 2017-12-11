package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import org.junit.Rule;
import org.junit.Test;
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
import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldThatEvaluator.filterClassesAppearingInFailureReport;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;

public class ShouldAccessClassesThatTest {

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void haveFullyQualifiedName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void dontHaveFullyQualifiedName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().dontHaveFullyQualifiedName(List.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().dontHaveSimpleName(List.class.getSimpleName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveNameMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveNameNotMatching(".*\\.List"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveSimpleNameStartingWith() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void haveSimpleNameNotStartingWith() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameNotStartingWith("Lis"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveSimpleNameContaining() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void haveSimpleNameNotContaining() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameNotContaining("is"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void haveSimpleNameEndingWith() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void haveSimpleNameNotEndingWith() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().haveSimpleNameNotEndingWith("ist"))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void resideInAPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().resideInAPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().resideOutsideOfPackage("..tngtech.."))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingConstructor.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(ClassAccessingPublicClass.class, ClassAccessingString.class, ClassAccessingConstructor.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class);
    }

    @Test
    public void arePublic() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().arePublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class);
    }

    @Test
    public void areNotPublic() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().areNotPublic())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPrivateClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().areProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingProtectedClass.class);
    }

    @Test
    public void areNotProtected() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().areNotProtected())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                ClassAccessingPackagePrivateClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().arePackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPackagePrivateClass.class);
    }

    @Test
    public void areNotPackagePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().areNotPackagePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().arePrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    public void areNotPrivate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().areNotPrivate())
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void haveModifier() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().haveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingPrivateClass.class);
    }

    @Test
    public void dontHaveModifier() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(noClasses().should().accessClassesThat().dontHaveModifier(PRIVATE))
                .on(ClassAccessingPublicClass.class, ClassAccessingPrivateClass.class,
                        ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassAccessingPublicClass.class, ClassAccessingPackagePrivateClass.class, ClassAccessingProtectedClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAnnotatedWith(SomeAnnotation.class))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingAnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_TYPE.is(classWithNameOf(SomeAnnotation.class));
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAnnotatedWith(hasNamePredicate))
                .on(ClassAccessingAnnotatedClass.class, ClassAccessingSimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingSimpleClass.class);
    }

    @Test
    public void implement_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().implement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    public void dontImplement_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().dontImplement(Collection.class))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    public void implement_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().implement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    public void dontImplement_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().dontImplement(Collection.class.getName()))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    public void implement_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().implement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingArrayList.class);
    }

    @Test
    public void dontImplement_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().dontImplement(classWithNameOf(Collection.class)))
                .on(ClassAccessingArrayList.class, ClassAccessingList.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableTo(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableTo(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThat(getOnlyElement(classes)).matches(ClassAccessingList.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingIterable.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableFrom(Collection.class))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableFrom(Collection.class.getName()))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingCollection.class, ClassAccessingIterable.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    @Test
    public void areInterfaces_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingCollection.class);
    }

    @Test
    public void areNotInterfaces_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat().areNotInterfaces())
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingSimpleClass.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingString.class, ClassAccessingSimpleClass.class);
    }

    @Test
    public void accessClassesThat_predicate() {
        List<JavaClass> classes = filterClassesAppearingInFailureReport(
                noClasses().should().accessClassesThat(are(not(assignableFrom(classWithNameOf(Collection.class))))))
                .on(ClassAccessingList.class, ClassAccessingString.class,
                        ClassAccessingCollection.class, ClassAccessingIterable.class);

        assertThatClasses(classes).matchInAnyOrder(ClassAccessingList.class, ClassAccessingString.class);
    }

    private DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private static class ClassAccessingList {
        @SuppressWarnings("unused")
        void call(List<?> list) {
            list.size();
        }
    }

    private static class ClassAccessingArrayList {
        @SuppressWarnings("unused")
        void call(ArrayList<?> list) {
            list.size();
        }
    }

    private static class ClassAccessingString {
        @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
        void call() {
            "string".length();
        }
    }

    private static class ClassAccessingCollection {
        @SuppressWarnings("unused")
        void call(Collection<?> collection) {
            collection.size();
        }
    }

    private static class ClassAccessingIterable {
        @SuppressWarnings("unused")
        void call(Iterable<?> iterable) {
            iterable.iterator();
        }
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

    private static class SimpleClass {
    }

    private static class PrivateClass {
    }

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

    @SomeAnnotation
    private static class AnnotatedClass {
    }
}