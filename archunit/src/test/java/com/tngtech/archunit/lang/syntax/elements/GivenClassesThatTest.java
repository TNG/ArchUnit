package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.AttributedString;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.ClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.DefaultClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.SourceRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClassTest.expectInvalidSyntaxUsageForClassInsteadOfInterface;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.expectInvalidSyntaxUsageForRetentionSource;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;

public class GivenClassesThatTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void haveFullyQualifiedName() {
        List<JavaClass> classes = filterResultOf(classes().that().haveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void dontHaveFullyQualifiedName() {
        List<JavaClass> classes = filterResultOf(classes().that().dontHaveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void doNotHaveFullyQualifiedName() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotHaveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaClass> classes = filterResultOf(classes().that().dontHaveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void doNotHaveSimpleName() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotHaveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaClass> classes = filterResultOf(classes().that().haveNameMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaClass> classes = filterResultOf(classes().that().haveNameNotMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameStartingWith() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameStartingWith("String"))
                .on(AttributedString.class, String.class, StringBuilder.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, StringBuilder.class);
    }

    @Test
    public void haveSimpleNameNotStartingWith() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameNotStartingWith("String"))
                .on(AttributedString.class, String.class, StringBuilder.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(AttributedString.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameContaining() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameContaining("rin"))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class);
    }

    @Test
    public void haveSimpleNameNotContaining() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameNotContaining("rin"))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameEndingWith() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameEndingWith("String"))
                .on(String.class, AttributedString.class, StringBuilder.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, AttributedString.class);
    }

    @Test
    public void haveSimpleNameNotEndingWith() {
        List<JavaClass> classes = filterResultOf(classes().that().haveSimpleNameNotEndingWith("String"))
                .on(String.class, AttributedString.class, StringBuilder.class);

        assertThatClasses(classes).matchInAnyOrder(StringBuilder.class);
    }

    @Test
    public void resideInAPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideInAPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(getClass());
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideOutsideOfPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaClass> classes = filterResultOf(classes().that().resideInAnyPackage("..tngtech..", "java.lang.reflect"))
                .on(getClass(), String.class, Constructor.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), Constructor.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaClass> classes = filterResultOf(classes().that()
                .resideOutsideOfPackages("..tngtech..", "java.lang.reflect")
        ).on(getClass(), String.class, Constructor.class);

        assertThatClasses(classes).matchInAnyOrder(String.class);
    }

    @Test
    public void arePublic() {
        List<JavaClass> classes = filterResultOf(classes().that().arePublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass());
    }

    @Test
    public void areNotPublic() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaClass> classes = filterResultOf(classes().that().areProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(ProtectedClass.class);
    }

    @Test
    public void areNotProtected() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().arePackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PackagePrivateClass.class);
    }

    @Test
    public void areNotPackagePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().arePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class);
    }

    @Test
    public void areNotPrivate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotPrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void haveModifiers() {
        List<JavaClass> classes = filterResultOf(classes().that().haveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(PrivateClass.class);
    }

    @Test
    public void dontHaveModifiers() {
        List<JavaClass> classes = filterResultOf(classes().that().dontHaveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void doNotHaveModifiers() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotHaveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatClasses(classes).matchInAnyOrder(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    /**
     * Compare {@link CanBeAnnotatedTest#annotatedWith_Retention_Source_is_rejected}
     */
    @Test
    public void annotatedWith_Retention_Source_is_rejected() {
        classes().that().areAnnotatedWith(RuntimeRetentionAnnotation.class);
        classes().that().areAnnotatedWith(ClassRetentionAnnotation.class);
        classes().that().areAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        classes().that().areAnnotatedWith(SourceRetentionAnnotation.class);
    }

    @Test
    public void areNotAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    /**
     * Compare {@link CanBeAnnotatedTest#annotatedWith_Retention_Source_is_rejected}
     */
    @Test
    public void notAnnotatedWith_Retention_Source_is_rejected() {
        classes().that().areNotAnnotatedWith(RuntimeRetentionAnnotation.class);
        classes().that().areNotAnnotatedWith(ClassRetentionAnnotation.class);
        classes().that().areNotAnnotatedWith(DefaultClassRetentionAnnotation.class);

        expectInvalidSyntaxUsageForRetentionSource(thrown);
        classes().that().areNotAnnotatedWith(SourceRetentionAnnotation.class);
    }

    @Test
    public void areAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThat(getOnlyElement(classes)).matches(SimpleClass.class);
    }

    @Test
    public void areMetaAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areMetaAnnotatedWith(SomeAnnotation.class))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void areMetaAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void areMetaAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areMetaAnnotatedWith(hasNamePredicate))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThat(getOnlyElement(classes)).matches(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaClass> classes = filterResultOf(classes().that().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatClasses(classes).matchInAnyOrder(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void implement_type() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);

        classes = filterResultOf(classes().that().implement(Set.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(classes).isEmpty();
    }

    @Test
    public void implement_rejects_non_interface_types() {
        classes().that().implement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().that().implement(AbstractList.class);
    }

    @Test
    public void dontImplement_type() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_type() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotImplement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_rejects_non_interface_types() {
        classes().that().doNotImplement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().that().doNotImplement(AbstractList.class);
    }

    @Test
    public void implement_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);

        classes = filterResultOf(classes().that().implement(AbstractList.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(classes).isEmpty();
    }

    @Test
    public void dontImplement_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotImplement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void implement_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().implement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);

        classes = filterResultOf(classes().that().implement(classWithNameOf(AbstractList.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(classes).isEmpty();
    }

    @Test
    public void dontImplement_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().dontImplement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().doNotImplement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);

        classes = filterResultOf(classes().that().areAssignableTo(AbstractList.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);

        classes = filterResultOf(classes().that().areAssignableTo(AbstractList.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(List.class);

        classes = filterResultOf(classes().that().areAssignableTo(classWithNameOf(AbstractList.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(getOnlyElement(classes)).matches(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Iterable.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class);
    }

    @Test
    public void areInterfaces_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areInterfaces())
                .on(List.class, String.class, Collection.class, Integer.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, Collection.class);
    }

    @Test
    public void areNotInterfaces_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotInterfaces())
                .on(List.class, String.class, Collection.class, Integer.class);

        assertThatClasses(classes).matchInAnyOrder(String.class, Integer.class);
    }

    @Test
    public void areEnums_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areEnums())
                .on(StandardCopyOption.class, StandardOpenOption.class, Collection.class, Integer.class);

        assertThatClasses(classes).matchInAnyOrder(StandardCopyOption.class, StandardOpenOption.class);
    }

    @Test
    public void areNotEnums_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotEnums())
                .on(StandardCopyOption.class, StandardOpenOption.class, Collection.class, Integer.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Integer.class);
    }

    @Test
    public void areTopLevelClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areTopLevelClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(List.class, Map.class);
    }

    @Test
    public void areNotTopLevelClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotTopLevelClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes)
                .matchInAnyOrder(Map.Entry.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNestedClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNestedClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes)
                .matchInAnyOrder(Map.Entry.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotNestedClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotNestedClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(List.class, Map.class);
    }

    @Test
    public void areMemberClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areMemberClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes)
                .matchInAnyOrder(Map.Entry.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class);
    }

    @Test
    public void areNotMemberClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotMemberClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(List.class, Map.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areInnerClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areInnerClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes)
                .matchInAnyOrder(NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotInnerClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotInnerClasses())
                .on(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                        NestedClassWithSomeMoreClasses.StaticNestedClass.class, NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                        NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(List.class, Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.class,
                NestedClassWithSomeMoreClasses.StaticNestedClass.class);
    }

    @Test
    public void areAnonymousClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areAnonymousClasses())
                .on(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(NestedClassWithSomeMoreClasses.getAnonymousClass());
    }

    @Test
    public void areNotAnonymousClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotAnonymousClasses())
                .on(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areLocalClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areLocalClasses())
                .on(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotLocalClasses_predicate() {
        List<JavaClass> classes = filterResultOf(classes().that().areNotLocalClasses())
                .on(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatClasses(classes).matchInAnyOrder(Map.class, Map.Entry.class, NestedClassWithSomeMoreClasses.getAnonymousClass());
    }

    @Test
    public void belongToAnyOf() {
        List<JavaClass> classes = filterResultOf(classes().that().belongToAnyOf(ClassWithInnerClasses.class, String.class))
                .on(ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class, ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                        List.class, String.class, Iterable.class, StringBuilder.class);

        assertThatClasses(classes).matchInAnyOrder(
                ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class, ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                String.class);
    }

    @Test
    public void and_conjunction() {
        List<JavaClass> classes = filterResultOf(
                classes().that().haveNameMatching(".*\\..*i.*")
                        .and(have(nameMatching(".*\\..*(s|S).*")))
                        .and().haveNameMatching(".*\\..*n.*"))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(String.class);
    }

    @Test
    public void or_conjunction() {
        List<JavaClass> classes = filterResultOf(
                classes().that().haveSimpleName(List.class.getSimpleName())
                        .or(have(simpleName(String.class.getSimpleName())))
                        .or().haveSimpleName(Collection.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(List.class, String.class, Collection.class);
    }

    /**
     * We do not support operator precedence, like && and || does, we just aggregate as the predicates come.
     * If someone really needs such precedence, he has to use custom predicates, like a.and(b.or(c)).
     */
    @Test
    public void conjunctions_aggregate_in_sequence_without_special_precedence() {
        List<JavaClass> classes = filterResultOf(
                // (List OR String) AND Collection => empty
                classes().that().haveSimpleName(List.class.getSimpleName())
                        .or(have(simpleName(String.class.getSimpleName())))
                        .and().haveSimpleName(Collection.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThat(classes).isEmpty();

        classes = filterResultOf(
                // (List AND String) OR Collection OR Iterable => [Collection, Iterable]
                classes().that().haveSimpleName(List.class.getSimpleName())
                        .and(have(simpleName(String.class.getSimpleName())))
                        .or().haveSimpleName(Collection.class.getSimpleName())
                        .or().haveSimpleName(Iterable.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatClasses(classes).matchInAnyOrder(Collection.class, Iterable.class);
    }

    private DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private Evaluator<JavaClass> filterResultOf(GivenClassesConjunction givenClasses) {
        return new Evaluator<>(JavaClass.class, givenClasses);
    }

    static class Evaluator<T> {
        private final GivenConjunction<T> givenObjects;

        @SuppressWarnings("unused")
        Evaluator(Class<T> justForTyping, GivenConjunction<T> givenObjects) {
            this.givenObjects = givenObjects;
        }

        public List<T> on(Class<?>... toCheck) {
            final List<T> result = new ArrayList<>();
            JavaClasses classes = importClassesWithContext(toCheck);
            ArchCondition<T> condition = new ArchCondition<T>("ignored") {
                @Override
                public void check(T item, ConditionEvents events) {
                    result.add(item);
                }
            };
            givenObjects.should(condition).check(classes);
            return result;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SomeAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SomeAnnotation
    private @interface MetaAnnotatedAnnotation {
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

    @SomeAnnotation
    private static class AnnotatedClass {
    }

    @MetaAnnotatedAnnotation
    private static class MetaAnnotatedClass {
    }

    private static class ClassWithInnerClasses {
        private static class InnerClass {
            private static class EvenMoreInnerClass {
            }
        }
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
