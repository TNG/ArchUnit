package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.NATIVE;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNCHRONIZED;
import static com.tngtech.archunit.core.domain.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.domain.JavaModifier.VOLATILE;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporterTestUtils.isLambdaMethodName;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporterTestUtils.isSyntheticAccessMethodName;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.propertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.runtimePropertiesOf;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

public class JavaMembersAssertion extends AbstractObjectAssert<JavaMembersAssertion, List<JavaMember>> {
    public JavaMembersAssertion(Iterable<? extends JavaMember> javaMembers) {
        super(sort(javaMembers), JavaMembersAssertion.class);
    }

    private static List<JavaMember> sort(Iterable<? extends JavaMember> actual) {
        return stream(actual.spliterator(), false)
                .sorted(comparing(HasName.AndFullName::getFullName))
                .collect(toList());
    }

    public <M extends Member & AnnotatedElement> void matchInAnyOrder(Collection<M> expectedMembers) {
        assertThat(actual).as("Actual JavaMembers").hasSameSizeAs(expectedMembers);
        List<M> sorted = sort(expectedMembers);
        for (int i = 0; i < sorted.size(); i++) {
            assertThat(actual.get(i).getName()).as("Sorted member number " + i).isEqualTo(JavaMemberAssertion.getExpectedNameOf(sorted.get(i)));
            assertThat(actual.get(i)).isEquivalentTo(sorted.get(i));
        }
    }

    private <M extends Member & AnnotatedElement> List<M> sort(Collection<M> expectedMembers) {
        return expectedMembers.stream()
                .sorted(comparing(JavaMemberAssertion::getExpectedFullNameOf))
                .collect(toList());
    }

    public void matchInAnyOrderMembersOf(Class<?>... classes) {
        Set<Member> members = new HashSet<>();
        for (Class<?> clazz : classes) {
            members.addAll(ImmutableSet.copyOf(clazz.getDeclaredFields()));
            members.addAll(
                    Arrays.stream(clazz.getDeclaredMethods())
                            .filter(m -> !isLambdaMethodName(m.getName()) && !isSyntheticAccessMethodName(m.getName()))
                            .collect(toSet()));
            members.addAll(ImmutableSet.copyOf(clazz.getDeclaredConstructors()));
        }

        matchCasted(members);
    }

    // We know this is compatible since every Member also implements AnnotatedElement
    // Unfortunately the Java type system does not make it easy here
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void matchCasted(Set<Member> members) {
        matchInAnyOrder((Set) members);
    }

    static <T extends Member & AnnotatedElement> void assertEquivalent(JavaMember javaMember, T member) {
        assertThat(javaMember.getOwner().reflect()).isEqualTo(member.getDeclaringClass());
        assertModifiersMatch(javaMember, member);
        assertThat(runtimePropertiesOf(javaMember.getAnnotations()))
                .isEqualTo(propertiesOf(member.getAnnotations()));
    }

    private static <T extends Member> void assertModifiersMatch(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(ABSTRACT))
                .as("member %s is abstract", javaMember.getFullName())
                .isEqualTo(Modifier.isAbstract(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(FINAL))
                .as("member %s is final", javaMember.getFullName())
                .isEqualTo(Modifier.isFinal(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PRIVATE))
                .as("member %s is private", javaMember.getFullName())
                .isEqualTo(Modifier.isPrivate(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PROTECTED))
                .as("member %s is protected", javaMember.getFullName())
                .isEqualTo(Modifier.isProtected(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(PUBLIC))
                .as("member %s is public", javaMember.getFullName())
                .isEqualTo(Modifier.isPublic(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(STATIC))
                .as("member %s is static", javaMember.getFullName())
                .isEqualTo(Modifier.isStatic(member.getModifiers()));

        if (javaMember instanceof JavaCodeUnit) {
            assertCodeUnitModifiers(javaMember, member);
        }

        if (javaMember instanceof JavaField) {
            assertFieldModifiers(javaMember, member);
        }
    }

    private static <T extends Member> void assertCodeUnitModifiers(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(NATIVE))
                .as("member %s is native", javaMember.getFullName())
                .isEqualTo(Modifier.isNative(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(SYNCHRONIZED))
                .as("member %s is synchronized", javaMember.getFullName())
                .isEqualTo(Modifier.isSynchronized(member.getModifiers()));
    }

    private static <T extends Member> void assertFieldModifiers(JavaMember javaMember, T member) {
        assertThat(javaMember.getModifiers().contains(TRANSIENT))
                .as("member %s is transient", javaMember.getFullName())
                .isEqualTo(Modifier.isTransient(member.getModifiers()));
        assertThat(javaMember.getModifiers().contains(VOLATILE))
                .as("member %s is volatile", javaMember.getFullName())
                .isEqualTo(Modifier.isVolatile(member.getModifiers()));
    }
}
