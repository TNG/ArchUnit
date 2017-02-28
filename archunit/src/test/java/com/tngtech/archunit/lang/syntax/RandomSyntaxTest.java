package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.TestUtils.invoke;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class RandomSyntaxTest {
    private static final Logger LOG = LoggerFactory.getLogger(RandomSyntaxTest.class);
    private static final Random random = new Random();
    private static final int NUMBER_OF_RULES_TO_BUILD = 1000;

    @DataProvider
    public static List<List<?>> random_rules() {
        List<List<?>> result = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_RULES_TO_BUILD; i++) {
            SyntaxSpec spec = new SyntaxSpec();
            result.add(ImmutableList.of(spec.getActualArchRule(), spec.getExpectedDescription()));
        }
        return result;
    }

    @Test
    @UseDataProvider("random_rules")
    public void rule_has_expected_description_and_can_be_evaluated_without_error(DescribedRule describedRule, String expectedDescription) {
        ArchRule archRule = describedRule.archRule;

        assertThat(archRule.getDescription())
                .as("description of constructed ArchRule")
                .isEqualTo(expectedDescription);

        archRule.evaluate(JavaClasses.of(Collections.<JavaClass>emptySet()));
        archRule.check(JavaClasses.of(Collections.<JavaClass>emptySet()));

        ArchRule overriddenText = archRule.as("overridden rule text");
        assertThat(overriddenText.getDescription()).isEqualTo("overridden rule text");
        assertThat(overriddenText.evaluate(JavaClasses.of(Collections.<JavaClass>emptySet())).getFailureReport().toString())
                .contains("overridden rule text");
    }

    private static class SyntaxSpec {
        private static final int MAX_STEPS = 50;

        private final List<String> expectedDescription = new ArrayList<>();
        private final ArchRule actualArchRule;

        SyntaxSpec() {
            Step firstStep = new PartialStep(expectedDescription, GivenClasses.class, initRuleDefinition());
            LOG.debug("Starting from {}", firstStep);
            try {
                LastStep result = firstStep.continueSteps(0, MAX_STEPS);
                actualArchRule = result.getResult();
            } catch (Exception e) {
                throw new AssertionError("Failed while building rule, expected description so far: " + expectedDescription, e);
            }
        }

        private GivenClasses initRuleDefinition() {
            if (random.nextBoolean()) {
                expectedDescription.add("classes");
                return ArchRuleDefinition.classes();
            } else {
                expectedDescription.add("no classes");
                return ArchRuleDefinition.noClasses();
            }
        }

        String getExpectedDescription() {
            return Joiner.on(" ").join(expectedDescription)
                    .replace("dont", "don't");
        }

        DescribedRule getActualArchRule() {
            return new DescribedRule(actualArchRule);
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
        final List<String> expectedDescription;
        final TypedValue currentValue;

        private Step(List<String> expectedDescription, TypedValue currentValue) {
            this.expectedDescription = expectedDescription;
            this.currentValue = currentValue;
        }

        abstract LastStep continueSteps(int currentStepCount, int maxSteps);
    }

    private static class PartialStep extends Step {
        private static final ParameterProvider parameterProvider = new ParameterProvider();

        final Method method;
        final List<Parameter> parameters;

        <T> PartialStep(List<String> expectedDescription, Class<T> type, T currentValue) {
            this(expectedDescription, new TypedValue(type, currentValue), chooseRandomMethod(currentValue.getClass()).get());
        }

        private PartialStep(List<String> expectedDescription, TypedValue currentValue, Method method) {
            this(expectedDescription, currentValue, method, getParametersFor(method));
        }

        private PartialStep(List<String> expectedDescription, TypedValue currentValue, Method method, List<Parameter> parameters) {
            super(expectedDescription, currentValue);
            this.method = method;
            this.parameters = parameters;
            expectedDescription.addAll(getDescription());
        }

        private static List<Parameter> getParametersFor(Method method) {
            ArrayList<Parameter> result = new ArrayList<>();
            for (Type type : method.getGenericParameterTypes()) {
                result.add(parameterProvider.get(method.getName(), TypeToken.of(type)));
            }
            return result;
        }

        private Object[] getParameterValues(List<Parameter> parameters) {
            Object[] params = new Object[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                params[i] = parameters.get(i).value;
            }
            return params;
        }

        @Override
        LastStep continueSteps(int currentStepCount, int maxSteps) {
            if (currentStepCount == maxSteps) {
                throw new IllegalStateException("Creating rule was not finished within " + maxSteps + " steps");
            }

            TypedValue nextValue = new TypedValue(returnType(method, currentValue.value),
                    invoke(method, currentValue.value, getParameterValues(parameters)));
            checkState(nextValue.value != null,
                    "Invoking %s() on %s returned null (%s.java:0)",
                    method.getName(), currentValue.value, currentValue.value.getClass().getSimpleName());

            Optional<Method> method = chooseRandomMethod(nextValue.type);
            Step nextStep = method.isPresent() && shouldNotFinish(nextValue) ?
                    new PartialStep(expectedDescription, nextValue, method.get(), getParametersFor(method.get())) :
                    new LastStep(expectedDescription, nextValue);
            LOG.debug("Next step is {}", nextStep);
            return nextStep.continueSteps(currentStepCount + 1, maxSteps);
        }

        private boolean shouldNotFinish(TypedValue nextValue) {
            return !ArchRule.class.isAssignableFrom(nextValue.type) || random.nextBoolean();
        }

        private Class<?> returnType(Method method, Object value) {
            return TypeToken.of(value.getClass()).resolveType(method.getGenericReturnType()).getRawType();
        }

        private static Optional<Method> chooseRandomMethod(Class<?> clazz) {
            List<Method> methods = getPossibleMethodCandidates(clazz);
            for (Method method : ArchRule.class.getMethods()) {
                methods.remove(method);
            }
            return !methods.isEmpty() ?
                    Optional.of(methods.get(random.nextInt(methods.size()))) :
                    Optional.<Method>absent();
        }

        private static List<Method> getPossibleMethodCandidates(Class<?> clazz) {
            List<Method> result = new ArrayList<>();
            if (clazz.isInterface()) {
                result.addAll(asList(clazz.getDeclaredMethods()));
            }
            for (Class<?> i : clazz.getInterfaces()) {
                result.addAll(getPossibleMethodCandidates(i));
            }
            return result;
        }

        public Collection<String> getDescription() {
            List<String> result = new ArrayList<>();
            result.add(verbalize(method.getName()));
            for (Parameter parameter : parameters) {
                result.add(parameter.description);
            }
            return result;
        }

        private String verbalize(String name) {
            return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name).replace("_", " ");
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
        private LastStep(List<String> expectedDescription, TypedValue currentValue) {
            super(expectedDescription, currentValue);
            checkArgument(ArchRule.class.isAssignableFrom(currentValue.type),
                    "Type %s must be assignable to ArchRule", currentValue.type.getName());
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
        private final Class<?> type;
        private final Object value;

        // NOTE: type != value.getClass(), i.e. it's important what exactly the interface method returned
        private TypedValue(Class<?> type, Object value) {
            this.type = type;
            this.value = value;
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
        private static final Set<SpecificParameterProvider> providers = ImmutableSet.<SpecificParameterProvider>builder()
                .add(new SpecificParameterProvider(String.class) {
                    @Override
                    Parameter get(String methodName, TypeToken<?> type) {
                        if (methodName.toLowerCase().contains("annotat")) {
                            return new Parameter("AnnotationType", "@AnnotationType");
                        } else if (methodName.toLowerCase().contains("assign")) {
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
                        String description = Annotation.class.isAssignableFrom(typeParam.getRawType()) ?
                                "@" + Deprecated.class.getSimpleName() :
                                Deprecated.class.getName();
                        return new Parameter(Deprecated.class, description);
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


        @SuppressWarnings("unchecked")
        Parameter get(String methodName, TypeToken<?> type) {
            for (SpecificParameterProvider provider : providers) {
                if (provider.canHandle(type.getRawType())) {
                    return provider.get(methodName, type);
                }
            }
            throw new RuntimeException("Parameter type " + type + " is not supported yet");
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
}