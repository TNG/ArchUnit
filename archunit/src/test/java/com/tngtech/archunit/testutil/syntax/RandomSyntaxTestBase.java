package com.tngtech.archunit.testutil.syntax;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.testutil.TestUtils.invoke;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public abstract class RandomSyntaxTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(RandomSyntaxTestBase.class);
    protected static final Random random = new Random();
    private static final int NUMBER_OF_RULES_TO_BUILD = 1000;

    public static List<List<?>> createRandomRules(RandomSyntaxSeed<?> seed,
            DescriptionReplacement... replacements) {
        return createRandomRules(seed, MethodChoiceStrategy.chooseAllArchUnitSyntaxMethods(), replacements);
    }

    public static List<List<?>> createRandomRules(RandomSyntaxSeed<?> seed,
            MethodChoiceStrategy methodChoiceStrategy,
            DescriptionReplacement... replacements) {
        List<List<?>> result = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_RULES_TO_BUILD; i++) {
            SyntaxSpec<?> spec = new SyntaxSpec<>(seed, methodChoiceStrategy, ExpectedDescription.from(seed, replacements));
            result.add(ImmutableList.of(spec.getActualArchRule(), spec.getExpectedDescription()));
        }
        return result;
    }

    @Test
    @UseDataProvider("random_rules")
    public void rule_has_expected_description_and_can_be_evaluated_without_error(
            DescribedRule describedRule, String expectedDescription) {
        ArchRule archRule = describedRule.archRule;

        assertThat(archRule.getDescription()).as("description of constructed ArchRule").isEqualTo(expectedDescription);

        archRule.evaluate(importClassesWithContext());
        assertCheckEitherPassesOrThrowsAssertionError(archRule);

        ArchRule overriddenText = archRule.as("overridden rule text");
        assertThat(overriddenText.getDescription()).isEqualTo("overridden rule text");
        assertThat(overriddenText.evaluate(
                importClassesWithContext()).getFailureReport().toString()).contains(
                "overridden rule text");
    }

    private void assertCheckEitherPassesOrThrowsAssertionError(ArchRule archRule) {
        try {
            archRule.check(importClassesWithContext());
            // it is okay if this passes
        } catch (AssertionError e) {
            // it is also okay, if this throws an AssertionError, but no other exception must be thrown
        }
    }

    private static class SyntaxSpec<T> {
        private static final int MAX_STEPS = 50;

        private final ExpectedDescription expectedDescription;
        private final ArchRule actualArchRule;

        SyntaxSpec(RandomSyntaxSeed<T> seed, MethodChoiceStrategy methodChoiceStrategy, ExpectedDescription expectedDescription) {
            this.expectedDescription = expectedDescription;
            Step firstStep = new PartialStep(methodChoiceStrategy, expectedDescription, seed.getType(), seed.getValue());
            LOG.debug("Starting from {}", firstStep);
            try {
                LastStep result = firstStep.continueSteps(0, MAX_STEPS);
                actualArchRule = result.getResult();
            } catch (Exception e) {
                throw new AssertionError(
                        "Failed while building rule, expected description so far: " + expectedDescription, e);
            }
        }

        String getExpectedDescription() {
            return expectedDescription.toString();
        }

        DescribedRule getActualArchRule() {
            return new DescribedRule(actualArchRule);
        }
    }

    protected interface DescriptionReplacement {
        /**
         * Can modify the list of tokens composing the current description in any way.
         *
         * @param currentToken       The description token to process
         * @param currentDescription All collected description tokens so far
         * @return true, if the token was already handled and should thus not be added to the description
         */
        boolean applyTo(String currentToken, List<String> currentDescription);
    }

    private static class ExpectedDescription {
        private final List<DescriptionReplacement> descriptionReplacements;
        private final List<String> description = new ArrayList<>();

        private ExpectedDescription(DescriptionReplacement[] descriptionReplacements) {
            this.descriptionReplacements = ImmutableList.copyOf(descriptionReplacements);
        }

        public static ExpectedDescription from(RandomSyntaxSeed<?> seed, DescriptionReplacement[] patternsToExclude) {
            return new ExpectedDescription(patternsToExclude).add(seed.getDescription());
        }

        private ExpectedDescription add(String expected) {
            boolean handled = false;
            for (DescriptionReplacement replacement : descriptionReplacements) {
                LOG.debug("Applying {}, token is \"{}\" and description so far {}",
                        replacement, expected, description);

                handled = handled || replacement.applyTo(expected, description);

                LOG.debug("Applied {}, token is \"{}\" and description so far {}",
                        replacement, expected, description);
            }
            if (!handled) {
                description.add(expected);
            }
            LOG.debug("Expected description is now {}", description);
            return this;
        }

        @Override
        public String toString() {
            return Joiner.on(" ").join(description)
                    .replace("dont", "don't");
        }
    }

    private static class DescribedRule {
        private final ArchRule archRule;

        private DescribedRule(ArchRule archRule) {
            this.archRule = archRule;
        }

        @Override
        public String toString() {
            return "RULE[" + archRule.getDescription() + "]";
        }
    }

    private abstract static class Step {
        final ExpectedDescription expectedDescription;
        final TypedValue currentValue;

        private Step(ExpectedDescription expectedDescription, TypedValue currentValue) {
            this.expectedDescription = expectedDescription;
            this.currentValue = currentValue;
        }

        abstract LastStep continueSteps(int currentStepCount, int maxSteps);
    }

    private static class PartialStep extends Step {
        private static final ParameterProvider parameterProvider = new ParameterProvider();

        private final MethodChoiceStrategy methodChoiceStrategy;
        final Method method;
        final Parameters parameters;

        <T> PartialStep(MethodChoiceStrategy methodChoiceStrategy, ExpectedDescription expectedDescription, TypeToken<T> type, T currentValue) {
            this(methodChoiceStrategy, expectedDescription, new TypedValue(type, currentValue),
                    methodChoiceStrategy.choose(type).get());
        }

        private PartialStep(MethodChoiceStrategy methodChoiceStrategy, ExpectedDescription expectedDescription, TypedValue currentValue,
                Method method) {
            this(methodChoiceStrategy, expectedDescription, currentValue, method, getParametersFor(method));
        }

        private PartialStep(
                MethodChoiceStrategy methodChoiceStrategy,
                ExpectedDescription expectedDescription,
                TypedValue currentValue,
                Method method,
                Parameters parameters) {

            super(expectedDescription, currentValue);
            this.methodChoiceStrategy = methodChoiceStrategy;
            this.method = method;
            this.parameters = parameters;
            expectedDescription.add(getDescription());
        }

        private static Parameters getParametersFor(Method method) {
            List<TypeToken<?>> tokens = new ArrayList<>();
            for (Type type : method.getGenericParameterTypes()) {
                tokens.add(TypeToken.of(type));
            }
            return parameterProvider.get(method.getName(), tokens);
        }

        @Override
        LastStep continueSteps(int currentStepCount, int maxSteps) {
            if (currentStepCount == maxSteps) {
                throw new IllegalStateException("Creating rule was not finished within " + maxSteps + " steps");
            }

            TypedValue nextValue = new TypedValue(returnType(method, currentValue),
                    invoke(method, currentValue.value, parameters.getValues()));
            checkState(nextValue.value != null,
                    "Invoking %s() on %s returned null (%s.java:0)",
                    method.getName(), currentValue.value, currentValue.value.getClass().getSimpleName());

            Optional<Method> method = methodChoiceStrategy.choose(nextValue.type);
            Step nextStep = method.isPresent() && shouldNotFinish(nextValue)
                    ? new PartialStep(methodChoiceStrategy, expectedDescription, nextValue, method.get(), getParametersFor(method.get()))
                    : new LastStep(expectedDescription, nextValue);
            LOG.debug("Next step is {}", nextStep);
            return nextStep.continueSteps(currentStepCount + 1, maxSteps);
        }

        private boolean shouldNotFinish(TypedValue nextValue) {
            return !ArchRule.class.isAssignableFrom(nextValue.getRawType()) || random.nextBoolean();
        }

        private TypeToken<?> returnType(Method method, TypedValue value) {
            return value.type.resolveType(method.getGenericReturnType());
        }

        public String getDescription() {
            return parameters.getDescription();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("object", currentValue)
                    .add("method", method)
                    .toString();
        }
    }

    private static class LastStep extends Step {
        private LastStep(ExpectedDescription expectedDescription, TypedValue currentValue) {
            super(expectedDescription, currentValue);
            checkArgument(ArchRule.class.isAssignableFrom(currentValue.getRawType()),
                    "Type %s must be assignable to ArchRule", currentValue.getRawType().getName());
        }

        @Override
        LastStep continueSteps(int currentStepCount, int maxSteps) {
            return this;
        }

        ArchRule getResult() {
            return (ArchRule) currentValue.value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("currentValue", currentValue)
                    .toString();
        }
    }

    private static class TypedValue {
        private final TypeToken<?> type;
        private final Object value;

        // NOTE: type != value.getClass(), i.e. it's important what exactly the interface method returned
        private TypedValue(TypeToken<?> type, Object value) {
            this.type = type;
            this.value = value;
        }

        Class<?> getRawType() {
            return type.getRawType();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("type", type)
                    .add("value", value)
                    .toString();
        }
    }

    private static class ParameterProvider {
        private static final Set<SpecificParameterProvider> singleParameterProviders = ImmutableSet.<SpecificParameterProvider>builder()
                .add(new SpecificParameterProvider(String.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        if (methodName.toLowerCase().contains("annotat")) {
                            return new Parameter("AnnotationType", "@AnnotationType");
                        } else if (methodName.toLowerCase().contains("assign")
                                || methodName.toLowerCase().contains("implement")
                                || methodName.toLowerCase().contains("declared")
                                || methodName.toLowerCase().contains("type")) {
                            return new Parameter("some.Type", "some.Type");
                        } else if (methodName.equals("be") || methodName.equals("notBe")) {
                            return new Parameter("some.Type", "some.Type");
                        } else {
                            return new Parameter("string", "'string'");
                        }
                    }
                })
                .add(new SpecificParameterProvider(String[].class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        return new Parameter(new String[]{"one", "two"}, "['one', 'two']");
                    }
                })
                .add(new SpecificParameterProvider(Class.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        TypeToken<?> typeParam = type.resolveType(Class.class.getTypeParameters()[0]);
                        String description = Annotation.class.isAssignableFrom(typeParam.getRawType())
                                ? "@" + Deprecated.class.getSimpleName()
                                : Deprecated.class.getName();
                        return new Parameter(Deprecated.class, description);
                    }
                })
                .add(new SpecificParameterProvider(Class[].class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        Class<?>[] value = {String.class, Serializable.class};
                        return new Parameter(value, "[" + value[0].getName() + ", " + value[1].getName() + "]");
                    }
                })
                .add(new SpecificParameterProvider(Enum.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        Object constant = type.getRawType().getEnumConstants()[0];
                        return new Parameter(constant, String.valueOf(constant));
                    }
                })
                .add(new SpecificParameterProvider(DescribedPredicate.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        return new Parameter(DescribedPredicate.alwaysTrue().as("custom predicate"), "custom predicate");
                    }
                })
                .add(new SpecificParameterProvider(ArchCondition.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        return new Parameter(new ArchCondition<Object>("overrideMe") {
                            @Override
                            public void check(Object item, ConditionEvents events) {
                            }
                        }.as("custom condition"), "custom condition");
                    }
                })
                .build();

        private final List<SpecificParametersProvider> parametersProvider = ImmutableList.of(
                new FieldMethodParametersProvider(),
                new CallMethodClassParametersProvider(),
                new CallMethodStringParametersProvider(),
                new CallConstructorClassParametersProvider(),
                new CallConstructorStringParametersProvider(),
                new SingleParametersProvider());

        Parameters get(String methodName, List<TypeToken<?>> types) {
            for (SpecificParametersProvider provider : parametersProvider) {
                if (provider.canHandle(methodName, types)) {
                    return provider.get(methodName, types);
                }
            }
            throw new IllegalStateException(String.format(
                    "No ParametersProvider found for %s with parameterTypes %s", methodName, types));
        }

        Parameter get(String methodName, TypeToken<?> type) {
            for (SpecificParameterProvider provider : singleParameterProviders) {
                if (provider.canHandle(type.getRawType())) {
                    return provider.get(methodName, type);
                }
            }
            throw new RuntimeException("Parameter type " + type + " is not supported yet");
        }

        private abstract static class SpecificParametersProvider {
            abstract boolean canHandle(String methodName, List<TypeToken<?>> parameterTypes);

            abstract Parameters get(String methodName, List<TypeToken<?>> parameterTypes);
        }

        private abstract static class SpecificParameterProvider {
            private final Class<?> supportedType;

            SpecificParameterProvider(Class<?> supportedType) {
                this.supportedType = supportedType;
            }

            boolean canHandle(Class<?> type) {
                return supportedType.isAssignableFrom(type);
            }

            abstract Parameter get(String methodName, TypeToken<?> type);
        }

        private class FieldMethodParametersProvider extends SpecificParametersProvider {
            @Override
            boolean canHandle(String methodName, List<TypeToken<?>> parameterTypes) {
                return methodName.toLowerCase().contains("field");
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                Parameters parameters = new SingleParametersProvider().get(methodName, parameterTypes);
                if (parameterTypes.size() == 2) {
                    return specificHandlingOfTwoParameterMethods(methodName, parameterTypes, parameters);
                }
                return parameters;
            }

            private Parameters specificHandlingOfTwoParameterMethods(String methodName, List<TypeToken<?>> parameterTypes, Parameters parameters) {
                checkKnownCase(parameterTypes);
                String first = simpleNameFrom(parameters.parameters.get(0).value);
                String params = first + "." + ParameterProvider.this.get(methodName, TypeToken.of(String.class)).value;
                return parameters.withDescription(verbalize(methodName) + " " + params);
            }

            private void checkKnownCase(List<TypeToken<?>> parameterTypes) {
                boolean firstParameterInvalid = !Class.class.isAssignableFrom(parameterTypes.get(0).getRawType())
                        && !String.class.isAssignableFrom(parameterTypes.get(0).getRawType());

                boolean secondParameterInvalid = !String.class.isAssignableFrom(parameterTypes.get(1).getRawType());

                if (firstParameterInvalid || secondParameterInvalid) {
                    throw new UnsupportedOperationException(String.format(
                            "Up to now all methods with two parameters "
                                    + "dealing with fields have either %s or %s as their first parameter type and"
                                    + "%s as their second parameter type. "
                                    + "If this doesn't hold anymore, please replace this with something more sophisticated",
                            Class.class.getName(), String.class.getName(), String.class.getName()));
                }
            }
        }

        private String simpleNameFrom(Object classOrString) {
            return Class.class.isAssignableFrom(classOrString.getClass())
                    ? ((Class<?>) classOrString).getSimpleName()
                    : ensureSimpleName((String) classOrString);
        }

        private static class CanHandlePredicate {
            private final String methodNamePart;
            private final int numberOfParameters;
            private final Class<?> typeOfFirstParameter;

            private CanHandlePredicate(String methodNamePart, int numberOfParameters, Class<?> typeOfFirstParameter) {
                this.methodNamePart = methodNamePart;
                this.numberOfParameters = numberOfParameters;
                this.typeOfFirstParameter = typeOfFirstParameter;
            }

            public boolean apply(String methodName, List<TypeToken<?>> parameterTypes) {
                return nameMatches(methodName) && numberOfParamsMatches(parameterTypes)
                        && firstParameterTypeMatches(parameterTypes);
            }

            private boolean nameMatches(String methodName) {
                return methodName.toLowerCase().contains(methodNamePart);
            }

            private boolean numberOfParamsMatches(List<TypeToken<?>> parameterTypes) {
                return parameterTypes.size() == numberOfParameters;
            }

            private boolean firstParameterTypeMatches(List<TypeToken<?>> parameterTypes) {
                return typeOfFirstParameter.isAssignableFrom(parameterTypes.get(0).getRawType());
            }
        }

        private abstract class CallCodeUnitParametersProvider extends SpecificParametersProvider {
            private final CanHandlePredicate predicate;

            CallCodeUnitParametersProvider(CanHandlePredicate predicate) {
                this.predicate = predicate;
            }

            @Override
            boolean canHandle(String methodName, List<TypeToken<?>> parameterTypes) {
                return predicate.apply(methodName, parameterTypes);
            }
        }

        private class CallMethodClassParametersProvider extends CallCodeUnitParametersProvider {
            CallMethodClassParametersProvider() {
                super(new CanHandlePredicate("method", 3, Class.class));
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                Parameters parameters = new SingleParametersProvider().get(methodName, parameterTypes);
                String params = createCallDetailsForClassArrayAtIndex(2, parameters.parameters.get(1).value, parameters);
                return parameters.withDescription(verbalize(methodName) + " " + params);
            }
        }

        private class CallMethodStringParametersProvider extends CallCodeUnitParametersProvider {
            CallMethodStringParametersProvider() {
                super(new CanHandlePredicate("method", 3, String.class));
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                Parameters parameters = new SingleParametersProvider().get(methodName, parameterTypes);
                String params = createCallDetailsForStringArrayAtIndex(2, parameters.parameters.get(1).value, parameters);
                return parameters.withDescription(verbalize(methodName) + " " + params);
            }
        }

        private class CallConstructorClassParametersProvider extends CallCodeUnitParametersProvider {
            CallConstructorClassParametersProvider() {
                super(new CanHandlePredicate("constructor", 2, Class.class));
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                Parameters parameters = new SingleParametersProvider().get(methodName, parameterTypes);
                String params = createCallDetailsForClassArrayAtIndex(1, CONSTRUCTOR_NAME, parameters);
                return parameters.withDescription(verbalize(methodName) + " " + params);
            }
        }

        private class CallConstructorStringParametersProvider extends CallCodeUnitParametersProvider {
            CallConstructorStringParametersProvider() {
                super(new CanHandlePredicate("constructor", 2, String.class));
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                Parameters parameters = new SingleParametersProvider().get(methodName, parameterTypes);
                String params = createCallDetailsForStringArrayAtIndex(1, CONSTRUCTOR_NAME, parameters);
                return parameters.withDescription(verbalize(methodName) + " " + params);
            }
        }

        private String createCallDetailsForClassArrayAtIndex(int index, Object callTargetName, Parameters parameters) {
            List<String> simpleParamTypeNames = new ArrayList<>();
            for (Class<?> param : (Class<?>[]) parameters.parameters.get(index).value) {
                simpleParamTypeNames.add(param.getSimpleName());
            }
            return createCallDetails(callTargetName, simpleParamTypeNames, parameters);
        }

        private String createCallDetailsForStringArrayAtIndex(int index, Object callTargetName, Parameters parameters) {
            // NOTE: For now strings are always simple word-like values, i.e. suitable simple names, adjust this when/if necessary
            List<String> simpleParamTypeNames = asList((String[]) parameters.parameters.get(index).value);
            return createCallDetails(callTargetName, simpleParamTypeNames, parameters);
        }

        private String createCallDetails(Object callTargetName, List<String> simpleParamTypeNames, Parameters parameters) {
            String first = simpleNameFrom(parameters.parameters.get(0).value);
            return String.format("%s.%s(%s)", first, callTargetName,
                    Joiner.on(", ").join(simpleParamTypeNames));
        }

        private class SingleParametersProvider extends SpecificParametersProvider {
            @Override
            boolean canHandle(String methodName, List<TypeToken<?>> parameterTypes) {
                return true;
            }

            @Override
            Parameters get(String methodName, List<TypeToken<?>> parameterTypes) {
                List<Parameter> params = new ArrayList<>();
                for (TypeToken<?> type : parameterTypes) {
                    params.add(ParameterProvider.this.get(methodName, type));
                }
                return new Parameters(methodName, params);
            }
        }
    }

    private static String verbalize(String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name).replace("_", " ");
    }

    private static class Parameters {
        private final List<Parameter> parameters;
        private final String description;

        private Parameters(List<Parameter> parameters, String description) {
            this.parameters = parameters;
            this.description = description;
        }

        private Parameters(String methodName, List<Parameter> parameters) {
            this.parameters = parameters;
            description = getDescription(methodName);
        }

        private String getDescription(String methodName) {
            List<String> result = new ArrayList<>();
            result.add(verbalize(methodName));
            for (Parameter parameter : parameters) {
                result.add(parameter.description);
            }
            return Joiner.on(" ").join(result);
        }

        Object[] getValues() {
            Object[] params = new Object[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                params[i] = parameters.get(i).value;
            }
            return params;
        }

        public String getDescription() {
            return description;
        }

        Parameters withDescription(String description) {
            return new Parameters(parameters, description);
        }
    }

    private static class Parameter {
        private final Object value;
        private final String description;

        Parameter(Object value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("value", value)
                    .add("description", description)
                    .toString();
        }
    }

    protected static class SingleStringReplacement implements DescriptionReplacement {

        private final String search;

        private final String replacement;

        public SingleStringReplacement(String search, String replacement) {
            this.search = search;
            this.replacement = replacement;
        }

        @Override
        public boolean applyTo(String currentToken, List<String> currentDescription) {
            if (currentToken.contains(search)) {
                currentDescription.add(currentToken.replace(search, replacement));
                return true;
            }

            return false;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{/" + search + "/" + replacement + "/}";
        }
    }
}