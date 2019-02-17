package com.tngtech.archunit.testutil.syntax;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.reflect.TypeToken;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchRule;

import static com.google.common.base.Predicates.or;

public class MethodChoiceStrategy {
    private final Random random = new Random();

    private final Predicate<Method> ignorePredicate;

    private MethodChoiceStrategy() {
        this(Predicates.<Method>alwaysFalse());
    }

    private MethodChoiceStrategy(Predicate<Method> ignorePredicate) {
        this.ignorePredicate = ignorePredicate;
    }

    public static MethodChoiceStrategy chooseAllArchUnitSyntaxMethods() {
        return new MethodChoiceStrategy();
    }

    public MethodChoiceStrategy exceptMethodsWithName(String string) {
        return new MethodChoiceStrategy(or(ignorePredicate, methodWithName(string)));
    }

    private Predicate<Method> methodWithName(final String methodName) {
        return new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                return input.getName().equals(methodName);
            }
        };
    }

    Optional<Method> choose(TypeToken<?> typeToken) {
        List<Method> methods = getPossibleMethodCandidates(typeToken.getRawType());
        return !methods.isEmpty()
                ? Optional.of(methods.get(random.nextInt(methods.size())))
                : Optional.<Method>absent();
    }

    private List<Method> getPossibleMethodCandidates(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (isCandidate(method)) {
                result.add(method);
            }
        }
        return result;
    }

    private boolean isCandidate(Method method) {
        return belongsToArchUnit(method) && isNoArchRuleMethod(method) && !ignorePredicate.apply(method);
    }

    private boolean belongsToArchUnit(Method method) {
        return method.getDeclaringClass().getName().contains(".archunit.") && methodDoesNotBelongTo(Object.class, method);
    }

    private boolean isNoArchRuleMethod(Method method) {
        return methodDoesNotBelongTo(ArchRule.class, method);
    }

    private boolean methodDoesNotBelongTo(Class<?> type, Method method) {
        try {
            type.getMethod(method.getName(), method.getParameterTypes());
            return false;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }
}
