package com.tngtech.archunit.testutil.syntax;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.tngtech.archunit.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.testutil.TestUtils.invoke;

class MethodCallChain {
    private final MethodChoiceStrategy methodChoiceStrategy;
    private Optional<Method> nextMethodCandidate;
    private TypedValue currentValue;

    MethodCallChain(MethodChoiceStrategy methodChoiceStrategy, TypedValue typedValue) {
        this.methodChoiceStrategy = checkNotNull(methodChoiceStrategy);
        currentValue = checkNotNull(typedValue);
        nextMethodCandidate = methodChoiceStrategy.choose(typedValue.getType());
    }

    TypedValue getCurrentValue() {
        return checkNotNull(currentValue);
    }

    Method getNextMethodCandidate() {
        return nextMethodCandidate.get();
    }

    boolean hasAnotherMethodCandidate() {
        return nextMethodCandidate.isPresent();
    }

    void invokeNextMethodCandidate(Parameters parameters) {
        PropagatedType nextType = currentValue.resolveType(nextMethodCandidate.get().getGenericReturnType());
        Object nextValue = invoke(nextMethodCandidate.get(), currentValue.getValue(), parameters.getValues());
        currentValue = validate(new TypedValue(nextType, nextValue));
        nextMethodCandidate = methodChoiceStrategy.choose(currentValue.getType());
    }

    private TypedValue validate(TypedValue value) {
        checkArgument(Modifier.isPublic(value.getRawType().getModifiers()),
                "Chosen type %s is not public", value.getRawType().getName());

        checkArgument(!value.getRawType().equals(Object.class),
                "Type of value got too generic: %s", value.getValue().getClass());

        checkState(currentValue.getValue() != null,
                "Invoking %s() on %s returned null (%s.java:0)",
                nextMethodCandidate.get().getName(), value, value.getValue().getClass().getSimpleName());

        return value;
    }
}
