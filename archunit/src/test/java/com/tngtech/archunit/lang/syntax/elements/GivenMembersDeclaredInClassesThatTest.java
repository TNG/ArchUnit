package com.tngtech.archunit.lang.syntax.elements;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.Reference;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.AttributedString;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.ClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.DefaultClassRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.RuntimeRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.SourceRetentionAnnotation;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThatTest.Evaluator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClassTest.expectInvalidSyntaxUsageForClassInsteadOfInterface;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotatedTest.expectInvalidSyntaxUsageForRetentionSource;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatMembers;

public class GivenMembersDeclaredInClassesThatTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void haveFullyQualifiedName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);
    }

    @Test
    public void dontHaveFullyQualifiedName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontHaveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void doNotHaveFullyQualifiedName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotHaveFullyQualifiedName(List.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void haveSimpleName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);
    }

    @Test
    public void dontHaveSimpleName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontHaveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void doNotHaveSimpleName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotHaveSimpleName(List.class.getSimpleName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void haveNameMatching() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveNameMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);
    }

    @Test
    public void haveNameNotMatching() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveNameNotMatching(".*List"))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameStartingWith() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameStartingWith("String"))
                .on(AttributedString.class, String.class, StringBuilder.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, StringBuilder.class);
    }

    @Test
    public void haveSimpleNameNotStartingWith() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameNotStartingWith("String"))
                .on(AttributedString.class, String.class, StringBuilder.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AttributedString.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameContaining() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameContaining("rin"))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class);
    }

    @Test
    public void haveSimpleNameNotContaining() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameNotContaining("rin"))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void haveSimpleNameEndingWith() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("String"))
                .on(String.class, AttributedString.class, StringBuilder.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, AttributedString.class);
    }

    @Test
    public void haveSimpleNameNotEndingWith() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("String"))
                .on(String.class, AttributedString.class, StringBuilder.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(StringBuilder.class);
    }

    @Test
    public void resideInAPackage() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().resideInAPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass());
    }

    @Test
    public void resideOutsideOfPackage() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().resideOutsideOfPackage("..tngtech.."))
                .on(getClass(), String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void resideInAnyPackage() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat()
                .resideInAnyPackage("..tngtech..", "java.lang.ref")
        ).on(getClass(), String.class, Reference.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), Reference.class);
    }

    @Test
    public void resideOutsideOfPackages() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat()
                .resideOutsideOfPackages("..tngtech..", "java.lang.ref")
        ).on(getClass(), String.class, Reference.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class);
    }

    @Test
    public void arePublic() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().arePublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass());
    }

    @Test
    public void areNotPublic() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotPublic())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areProtected() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ProtectedClass.class);
    }

    @Test
    public void areNotProtected() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotProtected())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), PrivateClass.class, PackagePrivateClass.class);
    }

    @Test
    public void arePackagePrivate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().arePackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(PackagePrivateClass.class);
    }

    @Test
    public void areNotPackagePrivate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotPackagePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), PrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void arePrivate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().arePrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(PrivateClass.class);
    }

    @Test
    public void areNotPrivate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotPrivate())
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void haveModifiers() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().haveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(PrivateClass.class);
    }

    @Test
    public void dontHaveModifiers() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontHaveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void doNotHaveModifiers() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotHaveModifier(PRIVATE))
                .on(getClass(), PrivateClass.class, PackagePrivateClass.class, ProtectedClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(getClass(), PackagePrivateClass.class, ProtectedClass.class);
    }

    @Test
    public void areAnnotatedWith_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class);
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
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAnnotatedWith(SomeAnnotation.class))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(SimpleClass.class);
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
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAnnotatedWith(SomeAnnotation.class.getName()))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(SimpleClass.class);
    }

    @Test
    public void areAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class);
    }

    @Test
    public void areNotAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAnnotatedWith(hasNamePredicate))
                .on(AnnotatedClass.class, SimpleClass.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(SimpleClass.class);
    }

    @Test
    public void areMetaAnnotatedWith_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areMetaAnnotatedWith(SomeAnnotation.class))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void areMetaAnnotatedWith_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void areMetaAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areMetaAnnotatedWith(hasNamePredicate))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(MetaAnnotatedClass.class);
    }

    @Test
    public void areNotMetaAnnotatedWith_predicate() {
        DescribedPredicate<HasType> hasNamePredicate = GET_RAW_TYPE.then(GET_NAME).is(equalTo(SomeAnnotation.class.getName()));
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotMetaAnnotatedWith(hasNamePredicate))
                .on(MetaAnnotatedClass.class, AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(AnnotatedClass.class, SimpleClass.class, MetaAnnotatedAnnotation.class);
    }

    @Test
    public void implement_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().implement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().implement(Set.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(members).isEmpty();
    }

    @Test
    public void implement_rejects_non_interface_types() {
        classes().that().implement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().that().implement(AbstractList.class);
    }

    @Test
    public void dontImplement_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontImplement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotImplement(Collection.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_rejects_non_interface_types() {
        classes().that().doNotImplement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        classes().that().doNotImplement(AbstractList.class);
    }

    @Test
    public void implement_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().implement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().implement(AbstractList.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(members).isEmpty();
    }

    @Test
    public void dontImplement_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontImplement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotImplement(Collection.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void implement_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().implement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().implement(classWithNameOf(AbstractList.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThat(members).isEmpty();
    }

    @Test
    public void dontImplement_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().dontImplement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void doNotImplement_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().doNotImplement(classWithNameOf(Collection.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(AbstractList.class))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableTo(Collection.class))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(AbstractList.class.getName()))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableTo(Collection.class.getName()))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void areAssignableTo_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class);

        members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableTo(classWithNameOf(AbstractList.class)))
                .on(ArrayList.class, List.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(ArrayList.class);
    }

    @Test
    public void areNotAssignableTo_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableTo(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Iterable.class);
    }

    @Test
    public void areAssignableFrom_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_type() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableFrom(Collection.class))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_typeName() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableFrom(Collection.class.getName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, String.class);
    }

    @Test
    public void areAssignableFrom_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Iterable.class);
    }

    @Test
    public void areNotAssignableFrom_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAssignableFrom(classWithNameOf(Collection.class)))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, String.class);
    }

    @Test
    public void areInterfaces_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areInterfaces())
                .on(List.class, String.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, Collection.class);
    }

    @Test
    public void areNotInterfaces_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotInterfaces())
                .on(List.class, String.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, Integer.class);
    }

    @Test
    public void areEnums_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areEnums())
                .on(StandardCopyOption.class, StandardOpenOption.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(StandardCopyOption.class, StandardOpenOption.class);
    }

    @Test
    public void areNotEnums_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotEnums())
                .on(StandardCopyOption.class, StandardOpenOption.class, Collection.class, Integer.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Integer.class);
    }

    @Test
    public void areTopLevelClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areTopLevelClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class);
    }

    @Test
    public void areNotTopLevelClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotTopLevelClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNestedClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNestedClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotNestedClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotNestedClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class);
    }

    @Test
    public void areMemberClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areMemberClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class);
    }

    @Test
    public void areNotMemberClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotMemberClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areInnerClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areInnerClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.InnerMemberClass.class,
                NestedClassWithSomeMoreClasses.getAnonymousClass(), NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotInnerClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotInnerClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class,
                        NestedClassWithSomeMoreClasses.InnerMemberClass.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.StaticNestedClass.class);
    }

    @Test
    public void areAnonymousClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areAnonymousClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.getAnonymousClass());
    }

    @Test
    public void areNotAnonymousClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotAnonymousClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areLocalClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areLocalClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members).matchInAnyOrderMembersOf(NestedClassWithSomeMoreClasses.getLocalClass());
    }

    @Test
    public void areNotLocalClasses_predicate() {
        List<JavaMember> members = filterResultOf(members().that().areDeclaredInClassesThat().areNotLocalClasses())
                .on(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getAnonymousClass(),
                        NestedClassWithSomeMoreClasses.getLocalClass());

        assertThatMembers(members)
                .matchInAnyOrderMembersOf(String.class, NestedClassWithSomeMoreClasses.class, NestedClassWithSomeMoreClasses.getAnonymousClass());
    }

    @Test
    public void belongToAnyOf() {
        List<JavaMember> members =
                filterResultOf(members().that().areDeclaredInClassesThat().belongToAnyOf(ClassWithInnerClasses.class, String.class))
                        .on(ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class,
                                ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                                List.class, String.class, Iterable.class, StringBuilder.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(
                ClassWithInnerClasses.class, ClassWithInnerClasses.InnerClass.class, ClassWithInnerClasses.InnerClass.EvenMoreInnerClass.class,
                String.class
        );
    }

    @Test
    public void and_conjunction() {
        List<JavaMember> members = filterResultOf(
                members().that().areDeclaredInClassesThat().haveNameMatching(".*\\..*i.*")
                        .and().areDeclaredInClassesThat(have(nameMatching(".*\\..*(s|S).*")))
                        .and().areDeclaredInClassesThat().haveNameMatching(".*\\..*n.*"))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(String.class);
    }

    @Test
    public void or_conjunction() {
        List<JavaMember> members = filterResultOf(
                members().that().areDeclaredInClassesThat().haveSimpleName(List.class.getSimpleName())
                        .or().areDeclaredInClassesThat(have(simpleName(String.class.getSimpleName())))
                        .or().areDeclaredInClassesThat().haveSimpleName(Collection.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(List.class, String.class, Collection.class);
    }

    /**
     * We do not support operator precedence, like && and || does, we just aggregate as the predicates come.
     * If someone really needs such precedence, he has to use custom predicates, like a.and(b.or(c)).
     */
    @Test
    public void conjunctions_aggregate_in_sequence_without_special_precedence() {
        List<JavaMember> members = filterResultOf(
                // (List OR String) AND Collection => empty
                members().that().areDeclaredInClassesThat().haveSimpleName(List.class.getSimpleName())
                        .or().areDeclaredInClassesThat(have(simpleName(String.class.getSimpleName())))
                        .and().areDeclaredInClassesThat().haveSimpleName(Collection.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThat(members).isEmpty();

        members = filterResultOf(
                // (List AND String) OR Collection OR Iterable => [Collection, Iterable]
                members().that().areDeclaredInClassesThat().haveSimpleName(List.class.getSimpleName())
                        .and().areDeclaredInClassesThat(have(simpleName(String.class.getSimpleName())))
                        .or().areDeclaredInClassesThat().haveSimpleName(Collection.class.getSimpleName())
                        .or().areDeclaredInClassesThat().haveSimpleName(Iterable.class.getSimpleName()))
                .on(List.class, String.class, Collection.class, Iterable.class);

        assertThatMembers(members).matchInAnyOrderMembersOf(Collection.class, Iterable.class);
    }

    private DescribedPredicate<HasName> classWithNameOf(Class<?> type) {
        return GET_NAME.is(equalTo(type.getName()));
    }

    private Evaluator<JavaMember> filterResultOf(GivenMembersConjunction<JavaMember> givenClasses) {
        return new Evaluator<>(JavaMember.class, givenClasses);
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

    // the fields are important for the test to test anything relevant
    @SuppressWarnings("unused")
    private static class ClassWithInnerClasses {
        String member;

        private static class InnerClass {
            String member;

            private static class EvenMoreInnerClass {
                String member;
            }
        }
    }

    @SuppressWarnings("unused")
    private static class NestedClassWithSomeMoreClasses {

        String member;

        static class StaticNestedClass {
            String member;
        }

        @SuppressWarnings("InnerClassMayBeStatic")
        class InnerMemberClass {
            String member;
        }

        static Class<?> getAnonymousClass() {
            return new Serializable() {
                String member;
            }.getClass();
        }

        static Class<?> getLocalClass() {
            class LocalClass {
                String member;
            }
            return LocalClass.class;
        }
    }
}
