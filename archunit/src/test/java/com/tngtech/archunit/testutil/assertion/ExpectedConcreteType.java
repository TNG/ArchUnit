package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import org.junit.Assert;

import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public interface ExpectedConcreteType {
    void assertMatchWith(JavaType actual, DescriptionContext context);

    class ExpectedConcreteClass implements ExpectedConcreteType {
        private final Class<?> clazz;

        private ExpectedConcreteClass(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            DescriptionContext newContext = context.describe(ensureSimpleName(actual.getName()));
            assertThat(actual).as(newContext.toString()).isInstanceOf(JavaClass.class);
            assertThatType(actual).as(newContext.toString()).matches(clazz);
        }

        static ExpectedConcreteType[] wrap(Class<?>[] classes) {
            ExpectedConcreteType[] result = new ExpectedConcreteType[classes.length];
            for (int i = 0; i < classes.length; i++) {
                result[i] = new ExpectedConcreteClass(classes[i]);
            }
            return result;
        }

        public static ExpectedConcreteClass concreteClass(Class<?> expectedType) {
            return new ExpectedConcreteClass(expectedType);
        }
    }

    class ExpectedConcreteParameterizedType implements ExpectedConcreteType {
        private final Type type;
        private final List<ExpectedConcreteType> typeParameters = new ArrayList<>();

        private ExpectedConcreteParameterizedType(Type type) {
            this.type = type;
        }

        public static ExpectedConcreteParameterizedType parameterizedType(Class<?> expectedType) {
            return new ExpectedConcreteParameterizedType(expectedType);
        }

        public ExpectedConcreteType withTypeArguments(Class<?>... type) {
            return withTypeArguments(ExpectedConcreteClass.wrap(type));
        }

        public ExpectedConcreteType withTypeArguments(ExpectedConcreteType... type) {
            typeParameters.addAll(ImmutableList.copyOf(type));
            return this;
        }

        public ExpectedConcreteType withWildcardTypeParameter() {
            return withTypeArguments(new ExpectedConcreteWildcardType());
        }

        public ExpectedConcreteType withWildcardTypeParameterWithUpperBound(Class<?> bound) {
            return withWildcardTypeParameterWithUpperBound(new ExpectedConcreteClass(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithUpperBound(ExpectedConcreteType bound) {
            return withTypeArguments(ExpectedConcreteWildcardType.wildcardType().withUpperBound(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithLowerBound(Class<?> bound) {
            return withWildcardTypeParameterWithLowerBound(new ExpectedConcreteClass(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithLowerBound(ExpectedConcreteType bound) {
            return withTypeArguments(ExpectedConcreteWildcardType.wildcardType().withLowerBound(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameters(ExpectedConcreteWildcardType... wildcardTypes) {
            return withTypeArguments(wildcardTypes);
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            DescriptionContext newContext = context.describe(ensureSimpleName(actual.getName()));
            assertThatType(actual.toErasure()).as(newContext.toString()).matches(type);
            assertTypeParametersMatch(actual, newContext);
        }

        private void assertTypeParametersMatch(JavaType actual, DescriptionContext context) {
            DescriptionContext parameterContext = context.step("type parameters").describeTypeParameters();
            if (!(actual instanceof JavaParameterizedType)) {
                Assert.fail(String.format(
                        "%s: Actual type is not parameterized, but expected to be parameterized with type parameters %s", parameterContext, typeParameters));
            }
            List<JavaType> actualTypeParameters = ((JavaParameterizedType) actual).getActualTypeArguments();
            assertThatTypes(actualTypeParameters).matchExactly(typeParameters, parameterContext);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + type + formatTypeParameters() + '}';
        }

        private String formatTypeParameters() {
            return !typeParameters.isEmpty() ? "<" + Joiner.on(", ").join(typeParameters) + ">" : "";
        }
    }

    class ExpectedConcreteWildcardType implements ExpectedConcreteType {
        private final List<ExpectedConcreteType> upperBounds = new ArrayList<>();
        private final List<ExpectedConcreteType> lowerBounds = new ArrayList<>();

        private ExpectedConcreteWildcardType() {
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            context = context.describe(actual.getName());

            assertThat(actual).as(context.toString()).isInstanceOf(JavaWildcardType.class);
            JavaWildcardType wildcardType = (JavaWildcardType) actual;
            assertThat(wildcardType.getName()).as(context.toString()).startsWith("?");

            assertUpperBoundsMatch(wildcardType, context);
            assertLowerBoundMatch(wildcardType, context);
        }

        private void assertUpperBoundsMatch(JavaWildcardType actual, DescriptionContext context) {
            context = context.step("upper bounds");
            assertThat(actual.getUpperBounds()).as(context.toString()).hasSameSizeAs(upperBounds);
            context = context.describeUpperBounds();
            assertBoundsMatch(actual.getUpperBounds(), upperBounds, context);
        }

        private void assertLowerBoundMatch(JavaWildcardType actual, DescriptionContext context) {
            context = context.step("lower bounds");
            assertThat(actual.getLowerBounds()).as(context.toString()).hasSameSizeAs(lowerBounds);
            context = context.describeLowerBounds();
            assertBoundsMatch(actual.getLowerBounds(), lowerBounds, context);
        }

        private void assertBoundsMatch(List<JavaType> actualBounds, List<ExpectedConcreteType> expectedBounds, DescriptionContext context) {
            for (int i = 0; i < expectedBounds.size(); i++) {
                expectedBounds.get(i).assertMatchWith(actualBounds.get(i), context);
            }
        }

        public ExpectedConcreteWildcardType withUpperBound(Class<?> bound) {
            return withUpperBound(new ExpectedConcreteClass(bound));
        }

        public ExpectedConcreteWildcardType withUpperBound(ExpectedConcreteType bound) {
            upperBounds.add(bound);
            return this;
        }

        public ExpectedConcreteWildcardType withLowerBound(Class<?> bound) {
            return withLowerBound(new ExpectedConcreteClass(bound));
        }

        public ExpectedConcreteWildcardType withLowerBound(ExpectedConcreteType bound) {
            lowerBounds.add(bound);
            return this;
        }

        public static ExpectedConcreteWildcardType wildcardType() {
            return new ExpectedConcreteWildcardType();
        }
    }

    class ExpectedConcreteTypeVariable implements ExpectedConcreteType {
        private final String name;
        private List<ExpectedConcreteType> upperBounds;

        private ExpectedConcreteTypeVariable(String name) {
            this.name = name;
        }

        public ExpectedConcreteTypeVariable withUpperBounds(Class<?>... bounds) {
            return withUpperBounds(ExpectedConcreteClass.wrap(bounds));
        }

        public ExpectedConcreteTypeVariable withUpperBounds(ExpectedConcreteType... bounds) {
            upperBounds = ImmutableList.copyOf(bounds);
            return this;
        }

        public ExpectedConcreteTypeVariable withoutUpperBounds() {
            upperBounds = emptyList();
            return this;
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            assertThat(actual).as(context.step("JavaType").toString()).isInstanceOf(JavaTypeVariable.class);
            JavaTypeVariable<?> actualTypeVariable = (JavaTypeVariable<?>) actual;
            assertThat(actualTypeVariable.getName()).as(context.step("type variable name").toString()).isEqualTo(name);

            if (upperBounds != null) {
                DescriptionContext newContext = context.describe(actual.getName()).step("bounds").metaInfo().describeUpperBounds();
                assertThatTypes(actualTypeVariable.getUpperBounds()).matchExactly(upperBounds, newContext);
            }
        }

        public static ExpectedConcreteTypeVariable typeVariable(String name) {
            return new ExpectedConcreteTypeVariable(name);
        }
    }

    class ExpectedConcreteTypeVariableArray implements ExpectedConcreteType {
        private final String name;
        private ExpectedConcreteType componentType;

        private ExpectedConcreteTypeVariableArray(String name) {
            this.name = name;
        }

        public ExpectedConcreteTypeVariableArray withComponentType(ExpectedConcreteType componentType) {
            this.componentType = componentType;
            return this;
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            assertThat(actual).as(context.step("JavaType").toString()).isInstanceOf(JavaGenericArrayType.class);
            JavaGenericArrayType actualArrayType = (JavaGenericArrayType) actual;
            assertThat(actualArrayType.getName()).as(context.step("type variable name").toString()).isEqualTo(name);

            if (componentType != null) {
                DescriptionContext newContext = context.describe(actual.getName()).step("component type").metaInfo();
                componentType.assertMatchWith(actualArrayType.getComponentType(), newContext);
            }
        }

        public static ExpectedConcreteTypeVariableArray typeVariableArray(String typeVariableArrayString) {
            return new ExpectedConcreteTypeVariableArray(typeVariableArrayString);
        }
    }
}
