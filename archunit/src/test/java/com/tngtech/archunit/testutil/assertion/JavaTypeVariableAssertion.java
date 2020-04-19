package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Assert;

import static com.google.common.collect.Iterables.cycle;
import static com.google.common.collect.Iterables.limit;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.JavaTypeVariableAssertion.ExpectedConcreteWildcardType.wildcardType;

public class JavaTypeVariableAssertion extends AbstractObjectAssert<JavaTypeVariableAssertion, JavaTypeVariable> {
    public JavaTypeVariableAssertion(JavaTypeVariable actual) {
        super(actual, JavaTypeVariableAssertion.class);
    }

    public void hasBoundsMatching(Class<?>... bounds) {
        hasBoundsMatching(ExpectedConcreteParameterizedType.wrap(bounds));
    }

    public void hasBoundsMatching(ExpectedConcreteType... bounds) {
        DescriptionContext context = new DescriptionContext(actual.getName()).step("bounds").describeUpperBounds();
        assertConcreteTypesMatch(context, actual.getBounds(), ImmutableList.copyOf(bounds));
    }

    private static void assertConcreteTypesMatch(DescriptionContext context, List<? extends JavaType> actual, List<ExpectedConcreteType> expected) {
        assertThat(actual).as(context.describeElements(actual.size()).toString()).hasSize(expected.size());
        for (int i = 0; i < actual.size(); i++) {
            DescriptionContext elementContext = context.describeElement(i, actual.size());
            expected.get(i).assertMatchWith(actual.get(i), elementContext);
        }
    }

    public static ExpectedConcreteParameterizedType parameterizedType(Class<?> expectedType) {
        return new ExpectedConcreteParameterizedType(expectedType);
    }

    public interface ExpectedConcreteType {
        void assertMatchWith(JavaType actual, DescriptionContext context);
    }

    public static class ExpectedConcreteParameterizedType implements ExpectedConcreteType {
        private Type type;
        private final List<ExpectedConcreteType> typeParameters = new ArrayList<>();

        private ExpectedConcreteParameterizedType(Type type) {
            this.type = type;
        }

        public ExpectedConcreteType withTypeArguments(Type... type) {
            return withTypeArguments(ExpectedConcreteParameterizedType.wrap(type));
        }

        public ExpectedConcreteType withTypeArguments(ExpectedConcreteType... type) {
            typeParameters.addAll(ImmutableList.copyOf(type));
            return this;
        }

        public ExpectedConcreteType withWildcardTypeParameter() {
            return withTypeArguments(new ExpectedConcreteWildcardType());
        }

        public ExpectedConcreteType withWildcardTypeParameterWithUpperBound(Class<?> bound) {
            return withWildcardTypeParameterWithUpperBound(new ExpectedConcreteParameterizedType(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithUpperBound(ExpectedConcreteType bound) {
            return withTypeArguments(wildcardType().withUpperBound(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithLowerBound(Class<?> bound) {
            return withWildcardTypeParameterWithLowerBound(new ExpectedConcreteParameterizedType(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameterWithLowerBound(ExpectedConcreteType bound) {
            return withTypeArguments(wildcardType().withLowerBound(bound));
        }

        public ExpectedConcreteType withWildcardTypeParameters(ExpectedConcreteWildcardType... wildcardTypes) {
            return withTypeArguments(wildcardTypes);
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            DescriptionContext newContext = context.describe(ensureSimpleName(actual.getName()));
            assertThatType(actual).as(newContext.toString()).matches(type);
            assertTypeParametersMatch(actual, newContext);
        }

        private void assertTypeParametersMatch(JavaType actual, DescriptionContext context) {
            DescriptionContext parameterContext = context.step("type parameters").describeTypeParameters();
            if (!typeParameters.isEmpty() && !(actual instanceof JavaParameterizedType)) {
                Assert.fail(String.format("%s: Not parameterized, but expected to have type parameters %s", parameterContext, typeParameters));
            }
            List<JavaType> actualTypeParameters = ((JavaParameterizedType) actual).getActualTypeArguments();
            assertConcreteTypesMatch(parameterContext, actualTypeParameters, typeParameters);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + type + formatTypeParameters() + '}';
        }

        private String formatTypeParameters() {
            return !typeParameters.isEmpty() ? "<" + Joiner.on(", ").join(typeParameters) + ">" : "";
        }

        static ExpectedConcreteType[] wrap(Type... types) {
            ExpectedConcreteType[] result = new ExpectedConcreteType[types.length];
            for (int i = 0; i < types.length; i++) {
                result[i] = new ExpectedConcreteParameterizedType(types[i]);
            }
            return result;
        }
    }

    public static class ExpectedConcreteWildcardType implements ExpectedConcreteType {
        private final List<ExpectedConcreteType> upperBounds = new ArrayList<>();
        private final List<ExpectedConcreteType> lowerBounds = new ArrayList<>();

        private ExpectedConcreteWildcardType() {
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            context = context.describe(actual.getName());

            assertThat(actual).as(context.toString()).isInstanceOf(JavaWildcardType.class);
            JavaWildcardType wildcardType = (JavaWildcardType) actual;
            assertThat(wildcardType.getName()).as(context.toString()).isEqualTo("?");

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
            return withUpperBound(new ExpectedConcreteParameterizedType(bound));
        }

        public ExpectedConcreteWildcardType withUpperBound(ExpectedConcreteType bound) {
            upperBounds.add(bound);
            return this;
        }

        public ExpectedConcreteWildcardType withLowerBound(Class<?> bound) {
            return withLowerBound(new ExpectedConcreteParameterizedType(bound));
        }

        public ExpectedConcreteWildcardType withLowerBound(ExpectedConcreteType bound) {
            lowerBounds.add(bound);
            return this;
        }

        public static ExpectedConcreteWildcardType wildcardType() {
            return new ExpectedConcreteWildcardType();
        }
    }

    public static class ExpectedConcreteTypeVariable implements ExpectedConcreteType {
        private final String name;
        private List<ExpectedConcreteType> upperBounds;

        private ExpectedConcreteTypeVariable(String name) {
            this.name = name;
        }

        public ExpectedConcreteTypeVariable withUpperBounds(Class<?>... bounds) {
            upperBounds = ImmutableList.copyOf(ExpectedConcreteParameterizedType.wrap(bounds));
            return this;
        }

        @Override
        public void assertMatchWith(JavaType actual, DescriptionContext context) {
            assertThat(actual).as(context.step("JavaType").toString()).isInstanceOf(JavaTypeVariable.class);
            JavaTypeVariable actualTypeVariable = (JavaTypeVariable) actual;
            assertThat(actualTypeVariable.getName()).as(context.step("type variable name").toString()).isEqualTo(name);

            if (upperBounds != null) {
                DescriptionContext newContext = context.describe(actual.getName()).step("bounds").metaInfo().describeUpperBounds();
                assertConcreteTypesMatch(newContext, actualTypeVariable.getUpperBounds(), upperBounds);
            }
        }

        public static ExpectedConcreteTypeVariable typeVariable(String name) {
            return new ExpectedConcreteTypeVariable(name);
        }
    }

    private static class DescriptionContext {
        private static final String MARKER = "##MARKER##";
        private static final String PLACEHOLDER = "_";

        private final String context;
        private final String description;
        private final String currentElement;
        private final String joinString;

        DescriptionContext(String context) {
            this(context + MARKER, "assertion", "", ", ");
        }

        private DescriptionContext(String context, String description, String currentElement, String joinString) {
            this.context = context;
            this.description = description;
            this.currentElement = currentElement;
            this.joinString = joinString;
        }

        public DescriptionContext describe(String part) {
            return new DescriptionContext(context.replace(MARKER, part + MARKER), description, part, joinString);
        }

        public DescriptionContext describeUpperBounds() {
            String newContext = context.replace(MARKER, " extends " + MARKER);
            return new DescriptionContext(newContext, description, currentElement, " & ");
        }

        public DescriptionContext describeLowerBounds() {
            String newContext = context.replace(MARKER, " super " + MARKER);
            return new DescriptionContext(newContext, description, currentElement, " & ");
        }

        public DescriptionContext describeElements(int number) {
            String elementsPlaceHolder = number > 0 ? joinedPlaceHolders(number) : "[]";
            return new DescriptionContext(context.replace(MARKER, elementsPlaceHolder), description, currentElement, joinString);
        }

        public DescriptionContext describeElement(int index, int totalSize) {
            int maxIndex = totalSize - 1;
            String prefix = index > 0 ? joinedPlaceHolders(index) + joinString : "";
            String suffix = index < maxIndex ? joinString + joinedPlaceHolders(maxIndex - index) : "";
            String newContext = context.replace(MARKER, prefix + MARKER + suffix);
            String newCurrentElement = this.currentElement + "[" + index + "]";
            return new DescriptionContext(newContext, description, newCurrentElement, joinString);
        }

        private String joinedPlaceHolders(int number) {
            return FluentIterable.from(limit(cycle(PLACEHOLDER), number)).join(Joiner.on(joinString));
        }

        public DescriptionContext step(String description) {
            return new DescriptionContext(context, description, currentElement, joinString);
        }

        public DescriptionContext metaInfo() {
            String newContext = context.replace(MARKER, "{" + MARKER + "}");
            return new DescriptionContext(newContext, description, currentElement, joinString);
        }

        public DescriptionContext describeTypeParameters() {
            String newContext = context.replace(MARKER, "<" + MARKER + ">");
            String newJoinString = ", ";
            return new DescriptionContext(newContext, description, currentElement, newJoinString);
        }

        @Override
        public String toString() {
            String currentElementInfix = currentElement.isEmpty() ? "" : "[" + currentElement + "]";
            return "\"" + description + "\"" + currentElementInfix + " -> " + context.replace(MARKER, "");
        }
    }
}
