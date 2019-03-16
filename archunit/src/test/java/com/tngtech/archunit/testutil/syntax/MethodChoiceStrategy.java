package com.tngtech.archunit.testutil.syntax;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchRule;

import static com.google.common.base.MoreObjects.toStringHelper;
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

    Optional<Method> choose(PropagatedType type) {
        List<Method> methods = getPossibleMethodCandidates(type.getRawType());
        return !methods.isEmpty()
                ? Optional.of(methods.get(random.nextInt(methods.size())))
                : Optional.<Method>absent();
    }

    private List<Method> getPossibleMethodCandidates(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        for (Method method : getInvocableMethods(clazz)) {
            if (isCandidate(method)) {
                result.add(method);
            }
        }
        return result;
    }

    private Collection<Method> getInvocableMethods(Class<?> clazz) {
        Map<MethodKey, Method> result = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            MethodKey key = MethodKey.of(method);
            Method invocableCandidate = result.containsKey(key)
                    ? resolveMethodInMoreSpecificType(method, result.get(key))
                    : method;
            result.put(key, invocableCandidate);
        }
        return result.values();
    }

    private Method resolveMethodInMoreSpecificType(Method first, Method second) {
        return second.getDeclaringClass().isAssignableFrom(first.getDeclaringClass()) ? first : second;
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

    private static class MethodKey {
        private final String name;
        private final List<Class<?>> parameterTypes;

        private MethodKey(Method method) {
            name = method.getName();
            parameterTypes = ImmutableList.copyOf(method.getParameterTypes());
        }

        static MethodKey of(Method method) {
            return new MethodKey(method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, parameterTypes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodKey other = (MethodKey) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.parameterTypes, other.parameterTypes);
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("name", name)
                    .add("parameterTypes", parameterTypes)
                    .toString();
        }
    }
}
