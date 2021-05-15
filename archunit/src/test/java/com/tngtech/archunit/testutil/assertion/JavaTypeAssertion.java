package com.tngtech.archunit.testutil.assertion;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.Formatters.ensureCanonicalArrayTypeName;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypeVariable;
import static com.tngtech.archunit.testutil.TestUtils.namesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.propertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.runtimePropertiesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class JavaTypeAssertion extends AbstractObjectAssert<JavaTypeAssertion, JavaType> {

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
                .isEqualTo(ensureCanonicalArrayTypeName(clazz.getSimpleName()));
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
        List<JavaTypeVariable<JavaClass>> typeVariables = actualClass().getTypeParameters();

        Optional<JavaTypeVariable<JavaClass>> variable = FluentIterable.from(typeVariables).firstMatch(toGuava(name(name)));
        assertThat(variable).as("Type variable with name '%s'", name).isPresent();

        return new JavaTypeVariableOfClassAssertion(variable.get());
    }

    public JavaTypeVariableOfClassAssertion hasOnlyTypeParameter(String name) {
        assertThat(actualClass().getTypeParameters()).as("Type parameters").hasSize(1);
        return hasTypeParameter(name);
    }

    public JavaTypeAssertion hasErasure(Class<?> rawType) {
        new JavaTypeAssertion(actual.toErasure()).as("Erasure of %s", actual.getName()).matches(rawType);
        return this;
    }

    public void hasActualTypeArguments(Class<?>... typeArguments) {
        hasActualTypeArguments(ExpectedConcreteClass.wrap(typeArguments));
    }

    public void hasActualTypeArguments(ExpectedConcreteType... typeArguments) {
        assertThat(actual).isInstanceOf(JavaParameterizedType.class);
        JavaParameterizedType parameterizedType = (JavaParameterizedType) this.actual;

        List<JavaType> actualTypeArguments = parameterizedType.getActualTypeArguments();
        DescriptionContext context = new DescriptionContext(actual.getName()).describeTypeParameters().step("actual type arguments");
        assertThat(actualTypeArguments).as(context.toString()).hasSameSizeAs(typeArguments);
        for (int i = 0; i < actualTypeArguments.size(); i++) {
            typeArguments[i].assertMatchWith(actualTypeArguments.get(i), context.describeElement(i, actualTypeArguments.size()));
        }
    }

    private JavaClass actualClass() {
        assertThat(actual).as(describeAssertion(actual.getName())).isInstanceOf(JavaClass.class);
        return (JavaClass) actual;
    }

    public static String getExpectedPackageName(Class<?> clazz) {
        if (!clazz.isArray()) {
            return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        }
        return getExpectedPackageName(clazz.getComponentType());
    }

    public class JavaTypeVariableOfClassAssertion extends AbstractObjectAssert<JavaTypeVariableOfClassAssertion, JavaTypeVariable<JavaClass>> {
        private JavaTypeVariableOfClassAssertion(JavaTypeVariable<JavaClass> actual) {
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
