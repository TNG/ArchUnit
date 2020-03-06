package com.tngtech.archunit.testutil.assertion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Assert;

import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;

public class JavaTypeVariableAssertion extends AbstractObjectAssert<JavaTypeVariableAssertion, JavaTypeVariable> {
    public JavaTypeVariableAssertion(JavaTypeVariable actual) {
        super(actual, JavaTypeVariableAssertion.class);
    }

    public void hasBoundsMatching(Class<?>... bounds) {
        hasBoundsMatching(ExpectedConcreteType.wrap(bounds));
    }

    public void hasBoundsMatching(ExpectedConcreteType... bounds) {
        assertConcreteTypesMatch(actual.getName(), actual.getBounds(), bounds);
    }

    private void assertConcreteTypesMatch(String name, List<? extends JavaType> actual, ExpectedConcreteType[] expected) {
        assertConcreteTypesMatch(name, actual, ImmutableList.copyOf(expected));
    }

    private void assertConcreteTypesMatch(String context, List<? extends JavaType> actual, List<ExpectedConcreteType> expected) {
        assertThat(actual).as("Concrete type variable bounds of " + context).hasSize(expected.size());
        for (int i = 0; i < actual.size(); i++) {
            assertConcreteTypeMatches(context, actual.get(i), expected.get(i));
        }
    }

    private void assertConcreteTypeMatches(String context, JavaType actual, ExpectedConcreteType expected) {
        String name = ensureSimpleName(actual.getName());
        String newContext = context + "->" + name;
        assertThatType(actual).as("Concrete type " + newContext).matches(expected.type);

        assertTypeParametersMatch(actual, expected, newContext);
    }

    private void assertTypeParametersMatch(JavaType actual, ExpectedConcreteType expected, String newContext) {
        if (!expected.typeParameters.isEmpty() && !(actual instanceof JavaParameterizedType)) {
            Assert.fail(String.format("Type %s is not parameterized, but expected to have type parameters %s", actual.getName(), expected.typeParameters));
        }
        List<JavaType> actualTypeParameters = ((JavaParameterizedType) actual).getActualTypeArguments();
        assertConcreteTypesMatch(newContext, actualTypeParameters, expected.typeParameters);
    }

    public static ExpectedConcreteType typeVariable(Class<?> expectedType) {
        return new ExpectedConcreteType(expectedType);
    }

    public static class ExpectedConcreteType {
        private final Type type;
        private final List<ExpectedConcreteType> typeParameters = new ArrayList<>();

        private ExpectedConcreteType(Type type) {
            this.type = type;
        }

        public ExpectedConcreteType withTypeArguments(Type... type) {
            for (Type t : type) {
                typeParameters.add(new ExpectedConcreteType(t));
            }
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + type + formatTypeParameters() + '}';
        }

        private String formatTypeParameters() {
            return !typeParameters.isEmpty() ? "<" + Joiner.on(", ").join(typeParameters) + ">" : "";
        }

        static ExpectedConcreteType[] wrap(Class<?>... types) {
            ExpectedConcreteType[] result = new ExpectedConcreteType[types.length];
            for (int i = 0; i < types.length; i++) {
                result[i] = new ExpectedConcreteType(types[i]);
            }
            return result;
        }
    }
}
