package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndName;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetIs;
import static java.util.Arrays.asList;

public final class ArchConditions {
    private ArchConditions() {
    }

    /**
     * @param packageIdentifier A String identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching the identifier
     */
    public static AbstractArchCondition<JavaClass> classAccessesPackage(String packageIdentifier) {
        return new ClassAccessesPackageCondition(packageIdentifier);
    }

    public static AbstractArchCondition<JavaClass> classGetsField(final Class<?> clazz, final String fieldName) {
        return classGetsFieldWith(ownerAndName(clazz, fieldName));
    }

    public static AbstractArchCondition<JavaClass> classGetsFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate);
    }

    public static AbstractArchCondition<JavaClass> classSetsField(final Class<?> clazz, final String fieldName) {
        return classSetsFieldWith(ownerAndName(clazz, fieldName));
    }

    public static AbstractArchCondition<JavaClass> classSetsFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate);
    }

    public static AbstractArchCondition<JavaClass> classAccessesField(final Class<?> clazz, final String fieldName) {
        return classAccessesFieldWith(ownerAndName(clazz, fieldName));
    }

    public static AbstractArchCondition<JavaClass> classAccessesFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate);
    }

    public static AbstractArchCondition<JavaClass> classCallsMethod(final Class<?> clazz, final String methodName, Class<?>... paramTypes) {
        return classCallsMethodWhere(targetIs(clazz, methodName, asList(paramTypes)));
    }

    public static AbstractArchCondition<JavaClass> classCallsMethodWhere(Predicate<JavaCall<?>> predicate) {
        return new ClassCallsMethodCondition(predicate);
    }

    public static AbstractArchCondition<JavaClass> classResidesIn(String packageIdentifier) {
        return new ClassResidesInCondition(packageIdentifier);
    }

    public static <T> AbstractArchCondition<T> never(AbstractArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    public static <T> AbstractArchCondition<Collection<? extends T>> containsAny(AbstractArchCondition<T> condition) {
        return new ContainsAnyCondition<>(condition);
    }

    public static <T> AbstractArchCondition<Collection<? extends T>> containsOnly(AbstractArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }
}
