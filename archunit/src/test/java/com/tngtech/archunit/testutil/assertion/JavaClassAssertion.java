package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static org.assertj.core.util.Strings.isNullOrEmpty;

public class JavaClassAssertion extends AbstractObjectAssert<JavaClassAssertion, JavaClass> {
    public JavaClassAssertion(JavaClass actual) {
        super(actual, JavaClassAssertion.class);
        describedAs(actual.getSimpleName());
    }

    public JavaClassAssertion matches(Class<?> clazz) {
        assertThatType(actual).as(descriptionText()).matches(clazz);
        return this;
    }

    public JavaClassAssertion hasName(String expectedName) {
        assertThat(actual.getName()).as(describeAssertion("full name")).isEqualTo(expectedName);
        return this;
    }

    public JavaClassAssertion hasSimpleName(String expectedSimpleName) {
        assertThat(actual.getSimpleName()).as(describeAssertion("simple name")).isEqualTo(expectedSimpleName);
        return this;
    }

    public JavaClassAssertion hasPackageName(String expectedPackageName) {
        assertThat(actual.getPackageName()).as(describeAssertion("package name")).isEqualTo(expectedPackageName);
        return this;
    }

    public JavaClassAssertion hasOnlyModifiers(JavaModifier... expectedModifiers) {
        assertThat(actual.getModifiers()).as(describeAssertion("modifiers")).containsOnly(expectedModifiers);
        return this;
    }

    public JavaClassAssertion hasNoSuperclass() {
        assertThat(actual.getRawSuperclass()).as(describeAssertion("raw superclass")).isAbsent();
        assertThat(actual.getSuperclass()).as(describeAssertion("superclass")).isAbsent();
        return this;
    }

    public JavaClassAssertion hasRawSuperclassMatching(Class<?> expectedSuperclass) {
        assertThat(actual.getRawSuperclass()).as(describeAssertion("super class")).isPresent();
        assertThatType(actual.getRawSuperclass().get()).as(describeAssertion("super class")).matches(expectedSuperclass);
        return this;
    }

    public JavaClassAssertion hasNoInterfaces() {
        assertThat(actual.getRawInterfaces()).as(describeAssertion("interfaces")).isEmpty();
        return this;
    }

    public JavaClassAssertion hasInterfacesMatchingInAnyOrder(Class<?>... expectedInterfaces) {
        assertThatTypes(actual.getRawInterfaces()).as(describeAssertion("interfaces")).matchInAnyOrder(expectedInterfaces);
        return this;
    }

    public JavaClassAssertion hasAllInterfacesMatchingInAnyOrder(Class<?>... expectedAllInterfaces) {
        assertThatTypes(actual.getAllRawInterfaces()).as(describeAssertion("all interfaces")).matchInAnyOrder(expectedAllInterfaces);
        return this;
    }

    public JavaClassAssertion isInterface(boolean expectedIsInterface) {
        assertThat(actual.isInterface()).as(describeAssertion("is interface")).isEqualTo(expectedIsInterface);
        return this;
    }

    public JavaClassAssertion isEnum(boolean expectedIsEnum) {
        assertThat(actual.isEnum()).as(describeAssertion("is enum")).isEqualTo(expectedIsEnum);
        return this;
    }

    public JavaClassAssertion isAnnotation(boolean expectedIsAnnotation) {
        assertThat(actual.isAnnotation()).as(describeAssertion("is annotation")).isEqualTo(expectedIsAnnotation);
        return this;
    }

    public JavaClassAssertion isRecord(boolean expectedIsRecord) {
        assertThat(actual.isRecord()).as(describeAssertion("is record")).isEqualTo(expectedIsRecord);
        return this;
    }

    public JavaClassAssertion isFullyImported(boolean expectedIsFullyImported) {
        assertThat(actual.isFullyImported()).as(describeAssertion("is fully imported")).isEqualTo(expectedIsFullyImported);
        return this;
    }

    public JavaClassAssertion hasNoEnclosingClass() {
        assertThat(actual.getEnclosingClass()).as(describeAssertion("enclosing class")).isAbsent();
        return this;
    }

    public JavaClassAssertion isTopLevelClass(boolean expectedIsTopLevelClass) {
        assertThat(actual.isTopLevelClass()).as(describeAssertion("is top level class")).isEqualTo(expectedIsTopLevelClass);
        return this;
    }

    public JavaClassAssertion isNestedClass(boolean expectedIsNestedClass) {
        assertThat(actual.isNestedClass()).as(describeAssertion("is nested class")).isEqualTo(expectedIsNestedClass);
        return this;
    }

    public JavaClassAssertion isMemberClass(boolean expectedIsMemberClass) {
        assertThat(actual.isMemberClass()).as(describeAssertion("is member class")).isEqualTo(expectedIsMemberClass);
        return this;
    }

    public JavaClassAssertion isInnerClass(boolean expectedIsInnerClass) {
        assertThat(actual.isInnerClass()).as(describeAssertion("is inner class")).isEqualTo(expectedIsInnerClass);
        return this;
    }

    public JavaClassAssertion isLocalClass(boolean expectedIsLocalClass) {
        assertThat(actual.isLocalClass()).as(describeAssertion("is local class")).isEqualTo(expectedIsLocalClass);
        return this;
    }

    public JavaClassAssertion isAnonymousClass(boolean expectedIsAnonymousClass) {
        assertThat(actual.isAnonymousClass()).as(describeAssertion("is anonymous class")).isEqualTo(expectedIsAnonymousClass);
        return this;
    }

    private String describeAssertion(String partialAssertionDescription) {
        return isNullOrEmpty(descriptionText())
                ? partialAssertionDescription
                : descriptionText() + " " + partialAssertionDescription;
    }
}
