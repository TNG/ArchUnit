package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.ExpectedConcreteType;
import org.assertj.core.api.AbstractObjectAssert;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypeVariable;
import static com.tngtech.archunit.testutil.TestUtils.namesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.propertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.runtimePropertiesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class JavaTypeAssertion extends AbstractObjectAssert<JavaTypeAssertion, JavaType> {
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(\\[+)(.*)");

    public JavaTypeAssertion(JavaType javaType) {
        super(javaType, JavaTypeAssertion.class);
    }

    public void matches(java.lang.reflect.Type type) {
        checkArgument(type instanceof Class<?>, "Only %s implemented so far, please extend", Class.class.getName());
        matches((Class<?>) type);
    }

    public void matches(Class<?> clazz) {
        JavaClass javaClass = actualClass();

        assertThat(javaClass.getName()).as(describeAssertion("Name of " + javaClass))
                .isEqualTo(clazz.getName());
        assertThat(javaClass.getSimpleName()).as(describeAssertion("Simple name of " + javaClass))
                .isEqualTo(ensureArrayName(clazz.getSimpleName()));
        assertThat(javaClass.getPackage().getName()).as(describeAssertion("Package of " + javaClass))
                .isEqualTo(getExpectedPackageName(clazz));
        assertThat(javaClass.getPackageName()).as(describeAssertion("Package name of " + javaClass))
                .isEqualTo(getExpectedPackageName(clazz));
        assertThat(javaClass.getModifiers()).as(describeAssertion("Modifiers of " + javaClass))
                .isEqualTo(JavaModifier.getModifiersForClass(clazz.getModifiers()));
        assertThat(javaClass.isArray()).as(describeAssertion(javaClass + " is array")).isEqualTo(clazz.isArray());
        assertThat(runtimePropertiesOf(javaClass.getAnnotations())).as(describeAssertion("Annotations of " + javaClass))
                .isEqualTo(propertiesOf(clazz.getAnnotations()));

        if (clazz.isArray()) {
            new JavaTypeAssertion(javaClass.getComponentType())
                    .as(describeAssertion(String.format("Component type of %s: ", javaClass.getSimpleName())))
                    .matches(clazz.getComponentType());
        }
    }

    private String describeAssertion(String partialAssertionDescription) {
        return isNullOrEmpty(descriptionText())
                ? partialAssertionDescription
                : descriptionText() + ": " + partialAssertionDescription;
    }

    public JavaTypeAssertion hasTypeParameters(String... names) {
        assertThat(namesOf(actualClass().getTypeParameters())).as("names of type parameters").containsExactly(names);
        return this;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // checked via AssertJ
    public JavaTypeVariableOfClassAssertion hasTypeParameter(String name) {
        List<JavaTypeVariable> typeVariables = actualClass().getTypeParameters();

        Optional<JavaTypeVariable> variable = FluentIterable.from(typeVariables).firstMatch(toGuava(name(name)));
        assertThat(variable).as("Type variable with name '%s'", name).isPresent();

        return new JavaTypeVariableOfClassAssertion(variable.get());
    }

    public JavaTypeVariableOfClassAssertion hasOnlyTypeParameter(String name) {
        assertThat(actualClass().getTypeParameters()).as("Type parameters").hasSize(1);
        return hasTypeParameter(name);
    }

    private JavaClass actualClass() {
        return actual instanceof JavaClass ? (JavaClass) actual : actual.toErasure();
    }

    private String ensureArrayName(String name) {
        String suffix = "";
        Matcher matcher = ARRAY_PATTERN.matcher(name);
        if (matcher.matches()) {
            name = Type.getType(matcher.group(2)).getClassName();
            suffix = Strings.repeat("[]", matcher.group(1).length());
        }
        return name + suffix;
    }

    public static String getExpectedPackageName(Class<?> clazz) {
        if (!clazz.isArray()) {
            return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        }
        return getExpectedPackageName(clazz.getComponentType());
    }

    public class JavaTypeVariableOfClassAssertion extends AbstractObjectAssert<JavaTypeVariableOfClassAssertion, JavaTypeVariable> {
        private JavaTypeVariableOfClassAssertion(JavaTypeVariable actual) {
            super(actual, JavaTypeVariableOfClassAssertion.class);
        }

        public JavaTypeAssertion withBoundsMatching(Class<?>... bounds) {
            assertThatTypeVariable(actual).hasBoundsMatching(bounds);
            return JavaTypeAssertion.this;
        }

        public JavaTypeAssertion withBoundsMatching(ExpectedConcreteType... bounds) {
            assertThatTypeVariable(actual).hasBoundsMatching(bounds);
            return JavaTypeAssertion.this;
        }
    }
}
