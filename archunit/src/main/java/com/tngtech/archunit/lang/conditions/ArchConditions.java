package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.ArchCondition;
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
    public static ArchCondition<JavaClass> classAccessesPackage(String packageIdentifier) {
        return new ClassAccessesPackageCondition(packageIdentifier);
    }

    public static ArchCondition<JavaClass> classGetsField(final Class<?> clazz, final String fieldName) {
        return classGetsFieldWith(ownerAndName(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> classGetsFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate);
    }

    public static ArchCondition<JavaClass> classSetsField(final Class<?> clazz, final String fieldName) {
        return classSetsFieldWith(ownerAndName(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> classSetsFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate);
    }

    public static ArchCondition<JavaClass> classAccessesField(final Class<?> clazz, final String fieldName) {
        return classAccessesFieldWith(ownerAndName(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> classAccessesFieldWith(Predicate<JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate);
    }

    public static ArchCondition<JavaClass> classCallsMethod(final Class<?> clazz, final String methodName, Class<?>... paramTypes) {
        return classCallsMethodWhere(targetIs(clazz, methodName, asList(paramTypes)));
    }

    public static ArchCondition<JavaClass> classCallsMethodWhere(Predicate<JavaCall<?>> predicate) {
        return new ClassCallsMethodCondition(predicate);
    }

    public static ArchCondition<JavaClass> classResidesIn(String packageIdentifier) {
        return new ClassResidesInCondition(packageIdentifier);
    }

    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containsAny(ArchCondition<T> condition) {
        return new ContainsAnyCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containsOnly(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }
}
