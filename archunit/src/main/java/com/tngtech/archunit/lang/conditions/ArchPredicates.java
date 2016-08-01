package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;

import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.core.Formatters.formatMethod;
import static com.tngtech.archunit.core.JavaClass.REFLECT;

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
        return new DescribedPredicate<JavaClass>(String.format("reside in '%s'", packageIdentifier)) {
            private final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

            @Override
            public boolean apply(JavaClass input) {
                return packageMatcher.matches(input.getPackage());
            }
        };
    }

    public static DescribedPredicate<JavaClass> annotatedWith(final Class<? extends Annotation> annotationType) {
        return new DescribedPredicate<JavaClass>("annotated with @") {
            @Override
            public boolean apply(JavaClass input) {
                return input.reflect().getAnnotation(annotationType) != null;
            }
        };
    }

    /**
     * Predicate for matching of simple class names against a regular expression.
     *
     * @param classNameRegex A regex to match against class names
     * @return A predicate for classes with matching name
     */
    public static DescribedPredicate<JavaClass> named(final String classNameRegex) {
        final Pattern pattern = Pattern.compile(classNameRegex);
        return new DescribedPredicate<JavaClass>(String.format("named '%s'", classNameRegex)) {
            @Override
            public boolean apply(JavaClass input) {
                return pattern.matcher(input.getSimpleName()).matches();
            }
        };
    }

    public static DescribedPredicate<JavaClass> inTheHierarchyOfAClassThat(final DescribedPredicate<JavaClass> predicate) {
        return new DescribedPredicate<JavaClass>("in the hierarchy of a class that " + predicate.getDescription()) {
            @Override
            public boolean apply(JavaClass input) {
                JavaClass current = input;
                while (current.getSuperClass().isPresent() && !predicate.apply(current)) {
                    current = current.getSuperClass().get();
                }
                return predicate.apply(current);
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> ownerAndNameAre(final Class<?> target, final String fieldName) {
        return new DescribedPredicate<JavaFieldAccess>(
                String.format("owner is %s and name is %s", target.getName(), fieldName)) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return owner(target).apply(input) &&
                        fieldName.equals(input.getTarget().getName());
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> owner(final Class<?> target) {
        return new DescribedPredicate<JavaFieldAccess>("owner " + target.getName()) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                return input.getTarget().getOwner().reflect() == target;
            }
        };
    }

    public static DescribedPredicate<JavaFieldAccess> accessType(final AccessType accessType) {
        return new DescribedPredicate<JavaFieldAccess>("access type " + accessType) {
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
     * @return A {@link com.tngtech.archunit.core.DescribedPredicate} returning true iff the package of the
     * tested {@link JavaClass} matches the identifier
     */
    public static DescribedPredicate<JavaFieldAccess> targetTypeResidesIn(String packageIdentifier) {
        final PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);

        return new DescribedPredicate<JavaFieldAccess>("target type resides in '%s'", packageIdentifier) {
            @Override
            public boolean apply(JavaFieldAccess input) {
                Class<?> fieldType = input.getTarget().reflect().getType();

                return !fieldType.isPrimitive() &&
                        packageMatcher.matches(fieldType.getPackage().getName());
            }
        };
    }

    public static DescribedPredicate<JavaCall<?>> targetIs(
            final Class<?> targetClass, final String methodName, final List<Class<?>> paramTypes) {
        return new DescribedPredicate<JavaCall<?>>("target is %s", formatMethod(targetClass.getName(), methodName, paramTypes)) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return targetHasName(targetClass, methodName).apply(input)
                        && input.getTarget().getParameters().equals(paramTypes);
            }
        };
    }

    public static DescribedPredicate<JavaCall<?>> targetHasName(final Class<?> targetClass, final String methodName) {
        return targetIs(DescribedPredicate.<Class<?>>equalTo(targetClass).onResultOf(REFLECT), methodName);
    }

    public static DescribedPredicate<JavaCall<?>> targetIs(final DescribedPredicate<JavaClass> targetSelector, final String methodName) {
        return new DescribedPredicate<JavaCall<?>>("target is %s and has name '%s'", targetSelector.getDescription(), methodName) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return targetOwnerIs(targetSelector).apply(input)
                        && input.getTarget().getName().equals(methodName);
            }
        };
    }

    public static DescribedPredicate<JavaCall<?>> targetOwnerIs(final Class<?> targetClass) {
        return targetOwnerIs(DescribedPredicate.<Class<?>>equalTo(targetClass).onResultOf(REFLECT));
    }

    public static DescribedPredicate<JavaCall<?>> targetOwnerIs(final DescribedPredicate<JavaClass> selector) {
        return new DescribedPredicate<JavaCall<?>>("target is " + selector) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return selector.apply(input.getTarget().getOwner());
            }
        };
    }

    public static DescribedPredicate<JavaCall<?>> originClassIs(Class<?> originClass) {
        return originClassIs(DescribedPredicate.<Class<?>>equalTo(originClass).onResultOf(REFLECT)
                .as("equal to %s.class", originClass.getSimpleName()));
    }

    public static DescribedPredicate<JavaCall<?>> originClassIsNot(Class<?> originClass) {
        return not(originClassIs(originClass)).as("origin class is not %s.class", originClass.getSimpleName());
    }

    public static DescribedPredicate<JavaCall<?>> originClassIs(final DescribedPredicate<JavaClass> selector) {
        return new DescribedPredicate<JavaCall<?>>("origin class is " + selector.getDescription()) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return selector.apply(input.getOriginOwner());
            }
        };
    }
}
