package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Annotation;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.FluentPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;

public class ArchPredicates {
    private ArchPredicates() {
    }

    /**
     * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
     * characters, '..' stands for any sequence of packages.
     * For further details see {@link com.tngtech.archunit.lang.conditions.PackageMatcher}.
     *
     * @param packageIdentifier A string representing the identifier to match packages against
     * @return A {@link com.tngtech.archunit.core.DescribedPredicate} returning true iff the package of the
     * tested {@link com.tngtech.archunit.core.JavaClass} matches the identifier
     */
    public static DescribedPredicate<JavaClass> resideIn(final String packageIdentifier) {
        return new DescribedPredicate<JavaClass>(String.format("classes that reside in '%s'", packageIdentifier)) {
            private final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

            @Override
            public boolean apply(JavaClass input) {
                return packageMatcher.matches(input.getPackage());
            }
        };
    }

    public static DescribedPredicate<JavaClass> annotatedWith(final Class<? extends Annotation> annotationType) {
        return new DescribedPredicate<JavaClass>() {
            @Override
            public boolean apply(JavaClass input) {
                return input.reflect().getAnnotation(annotationType) != null;
            }
        };
    }

    /**
     * Predicate for simple class name matching where the only wildcard is the asterisk '*' meaning arbitrary many symbols
     *
     * @param classNameIdentifier A string identifying class names
     * @return A predicate for classes with matching name
     */
    public static DescribedPredicate<JavaClass> named(final String classNameIdentifier) {
        return new DescribedPredicate<JavaClass>() {
            @Override
            public boolean apply(JavaClass input) {
                return ClassNameMatcher.of(classNameIdentifier).matches(input.getSimpleName());
            }
        };
    }

    public static FluentPredicate<JavaFieldAccess> ownerAndName(final Class<?> target, final String fieldName) {
        return new FluentPredicate<JavaFieldAccess>() {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return owner(target).apply(input) &&
                        fieldName.equals(input.getTarget().getName());
            }
        };
    }

    public static FluentPredicate<JavaFieldAccess> owner(final Class<?> target) {
        return new FluentPredicate<JavaFieldAccess>() {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return input.getTarget().getOwner().reflect() == target;
            }
        };
    }

    public static FluentPredicate<JavaFieldAccess> hasAccessType(final JavaFieldAccess.AccessType accessType) {
        return new FluentPredicate<JavaFieldAccess>() {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return accessType == input.getAccessType();
            }
        };
    }

    /**
     * Offers a syntax to identify packages similar to AspectJ. In particular '*' stands for any sequence of
     * characters, '..' stands for any sequence of packages.
     * For further details see {@link PackageMatcher}.
     *
     * @param packageIdentifier A string representing the identifier to match packages against
     * @return A {@link com.tngtech.archunit.core.FluentPredicate} returning true iff the package of the
     * tested {@link JavaClass} matches the identifier
     */
    public static FluentPredicate<JavaFieldAccess> fieldTypeIn(String packageIdentifier) {
        final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

        return new FluentPredicate<JavaFieldAccess>() {
            @Override
            public boolean apply(JavaFieldAccess input) {
                Class<?> fieldType = input.getTarget().reflect().getType();

                return !fieldType.isPrimitive() &&
                        packageMatcher.matches(fieldType.getPackage().getName());
            }
        };
    }

    public static FluentPredicate<JavaCall<?>> targetIs(
            final Class<?> targetClass, final String methodName, final List<Class<?>> paramTypes) {
        return new FluentPredicate<JavaCall<?>>() {
            @Override
            public boolean apply(JavaCall<?> input) {
                return targetHasName(targetClass, methodName).apply(input)
                        && input.getTarget().getParameters().equals(paramTypes);
            }
        };
    }

    public static FluentPredicate<JavaCall<?>> targetHasName(final Class<?> targetClass, final String methodName) {
        return targetIs(FluentPredicate.of(Predicates.<Class<?>>equalTo(targetClass)), methodName);
    }

    public static FluentPredicate<JavaCall<?>> targetIs(final Predicate<Class<?>> targetSelector, final String methodName) {
        return new FluentPredicate<JavaCall<?>>() {
            @Override
            public boolean apply(JavaCall<?> input) {
                return targetClassIs(targetSelector).apply(input)
                        && input.getTarget().getName().equals(methodName);
            }
        };
    }

    public static FluentPredicate<JavaCall<?>> targetClassIs(final Class<?> targetClass) {
        return targetClassIs(Predicates.<Class<?>>equalTo(targetClass));
    }

    public static FluentPredicate<JavaCall<?>> targetClassIs(final Predicate<Class<?>> selector) {
        return new FluentPredicate<JavaCall<?>>() {
            @Override
            public boolean apply(JavaCall<?> input) {
                return selector.apply(input.getTarget().getOwner().reflect());
            }
        };
    }

    public static FluentPredicate<JavaCall<?>> originClassIs(Class<?> originClass) {
        return originClassIs(Predicates.<Class<?>>equalTo(originClass));
    }

    public static FluentPredicate<JavaCall<?>> originClassIs(final Predicate<Class<?>> selector) {
        return new FluentPredicate<JavaCall<?>>() {
            @Override
            public boolean apply(JavaCall<?> input) {
                return selector.apply(input.getOriginOwner().reflect());
            }
        };
    }
}
