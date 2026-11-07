package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass;
import org.assertj.core.api.AbstractObjectAssert;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.tngtech.archunit.core.domain.Formatters.ensureCanonicalArrayTypeName;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypeVariable;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.propertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.runtimePropertiesOf;
import static com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.getTypeVariableWithName;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaTypeAssertion extends AbstractObjectAssert<JavaTypeAssertion, JavaType> {

    public JavaTypeAssertion(JavaType javaType) {
        super(javaType, JavaTypeAssertion.class);
    }

    public void matches(Type type) {
        matchesType(actual, type);
    }

    public void matches(Class<?> clazz) {
        matchesClass(actualClass(), clazz);
    }

    private void matchesType(JavaType javaType, Type type) {
        if (type instanceof Class) {
            Class<?> reflectionClass = (Class<?>) type;
            assertThat(javaType).isInstanceOfSatisfying(JavaClass.class, clazz -> matchesClass(clazz, reflectionClass));
        } else if (type instanceof ParameterizedType) {
            ParameterizedType reflectionParameterizedType = (ParameterizedType) type;
            Class<?> rawReflectedType = (Class<?>) reflectionParameterizedType.getRawType();
            Optional<Type> enclosingType = Optional.ofNullable(reflectionParameterizedType.getOwnerType());
            assertThat(javaType)
                    .as(describeAssertion("representation of " + type.getTypeName()))
                    .isInstanceOfSatisfying(JavaParameterizedType.class, parameterizedType -> {
                        matchesType(parameterizedType.toErasure(), rawReflectedType);
                        assertThat(parameterizedType.getActualTypeArguments()).hasSameSizeAs(reflectionParameterizedType.getActualTypeArguments())
                                .as(describeAssertion("all type arguments"))
                                .zipSatisfy(Arrays.asList(reflectionParameterizedType.getActualTypeArguments()), this::matchesType);
                        assertThat(parameterizedType.getEnclosingType().isPresent()).as("has enclosingType").isEqualTo(enclosingType.isPresent());
                        if (parameterizedType.getEnclosingType().isPresent() && enclosingType.isPresent()) {
                            matchesType(parameterizedType.getEnclosingType().get(), enclosingType.get());
                        }
                    });
            assertThat(javaType.toErasure().isEquivalentTo(rawReflectedType)).as(describeAssertion(String.format("raw class %s is equivalent", javaType.toErasure()))).isTrue();
        } else if (type instanceof TypeVariable) {
            assertThat(javaType).as(describeAssertion("type variable")).isInstanceOfSatisfying(JavaTypeVariable.class, typeVariable ->
                    assertThat(typeVariable.getName()).as(describeAssertion("type variable name")).isEqualTo(((TypeVariable<?>) type).getName()));
        } else if (type instanceof GenericArrayType) {
            assertThat(javaType).as(describeAssertion("generic array")).isInstanceOfSatisfying(JavaGenericArrayType.class, genericArrayType ->
                    matchesType(genericArrayType.getComponentType(), ((GenericArrayType) type).getGenericComponentType()));
        } else if (type instanceof WildcardType) {
            WildcardType reflectionWildCardType = (WildcardType) type;
            assertThat(javaType).as(describeAssertion("wildcard")).isInstanceOfSatisfying(JavaWildcardType.class, wildCardType -> {
                        Type[] upperBounds = reflectionWildCardType.getUpperBounds();
                        // wildcard with implicit and explicit upper bound to Object look different in ArchUnit, but not in reflection API
                        if (upperBounds.length == 1 && upperBounds[0] == Object.class && wildCardType.getUpperBounds().isEmpty()) {
                            upperBounds = new Type[]{};
                        }
                        assertThat(wildCardType.getUpperBounds()).hasSameSizeAs(upperBounds)
                                .as(describeAssertion("all upper bounds"))
                                .zipSatisfy(Arrays.asList(upperBounds), this::matchesType);
                        assertThat(wildCardType.getLowerBounds()).hasSameSizeAs(reflectionWildCardType.getLowerBounds())
                                .as(describeAssertion("all upper bounds"))
                                .zipSatisfy(Arrays.asList(reflectionWildCardType.getLowerBounds()), this::matchesType);
                    }
            );
        } else {
            throw new IllegalArgumentException("unknown type variant: " + type.getClass());
        }
    }

    private void matchesClass(JavaClass javaClass, Class<?> clazz) {

        assertThat(javaClass.getName()).as(describeAssertion("Name of " + javaClass))
                .isEqualTo(clazz.getName());
        assertThat(javaClass.getSimpleName()).as(describeAssertion("Simple name of " + javaClass))
                .isEqualTo(ensureCanonicalArrayTypeName(clazz.getSimpleName()));
        assertThat(javaClass.getPackage().getName()).as(describeAssertion("Package of " + javaClass))
                .isEqualTo(getExpectedPackageName(clazz));
        assertThat(javaClass.getPackageName()).as(describeAssertion("Package name of " + javaClass))
                .isEqualTo(getExpectedPackageName(clazz));
        assertThat(javaClass.getModifiers()).as(describeAssertion("Modifiers of " + javaClass))
                .isEqualTo(getExpectedModifiersForClass(clazz));
        assertThat(javaClass.isArray()).as(describeAssertion(javaClass + " is array")).isEqualTo(clazz.isArray());
        assertThat(runtimePropertiesOf(javaClass.getAnnotations())).as(describeAssertion("Annotations of " + javaClass))
                .isEqualTo(propertiesOf(clazz.getAnnotations()));

        if (clazz.isArray()) {
            new JavaTypeAssertion(javaClass.getComponentType())
                    .as(describeAssertion(String.format("Component type of %s: ", javaClass.getSimpleName())))
                    .matches(clazz.getComponentType());
        }
    }

    public void matches(ExpectedConcreteType type) {
        type.assertMatchWith(actual, new DescriptionContext(""));
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

    public JavaTypeVariableOfClassAssertion hasTypeParameter(String name) {
        JavaTypeVariable<JavaClass> typeVariable = getTypeVariableWithName(name, actualClass().getTypeParameters());
        return new JavaTypeVariableOfClassAssertion(typeVariable);
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
        if (clazz.isArray()) {
            return getExpectedPackageName(clazz.getComponentType());
        }
        if (clazz.isPrimitive()) {
            return "java.lang";
        }
        return clazz.getPackage() != null ? clazz.getPackage().getName() : "";
    }

    private Set<JavaModifier> getExpectedModifiersForClass(Class<?> clazz) {
        Set<JavaModifier> result = new HashSet<>(JavaModifier.getModifiersForClass(clazz.getModifiers()));
        if (clazz.isAnonymousClass() && runsOnPreJava9Vm()) {
            // anonymous classes have been a shady section of the JLS. At some point they are defined as "implicitly final"
            // in a later JLS as "never final", and the bytecode contains something different than the Reflection API returns
            // (possibly FINAL if declared in non-public classes, but not STATIC, while the Reflection API returns STATIC but not FINAL).
            // We will ignore this and stick to the bytecode, since it's a corner case and being consistent with the bytecode is the easiest
            result.remove(JavaModifier.STATIC);
            if (clazz.getEnclosingMethod() == null || (clazz.getEnclosingClass() != null && isPrivate(clazz.getEnclosingClass().getModifiers()))) {
                result.add(JavaModifier.FINAL);
            }
        }
        return result;
    }

    private boolean runsOnPreJava9Vm() {
        // Note that what we really would have to check is that the class was compiled with a JDK 7 or 8.
        // But unfortunately not the bytecode compatibility, but really if an old compiler from JDK 7 or 8 was used.
        // Since this seems very hard to find out, we just use the fact that we only compile with an old JDK if
        // we also run the test with an old JRE, so if the running JRE is newer the code was compiled with a newer JDK
        // (even if source and target compatibility are Java 7 / Java 8)
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("1.7.") || javaVersion.startsWith("1.8.");
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
