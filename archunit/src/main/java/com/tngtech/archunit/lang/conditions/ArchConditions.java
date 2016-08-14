package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.theHierarchyOf;

public final class ArchConditions {
    private ArchConditions() {
    }

    /**
     * @param packageIdentifier A String identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching the identifier
     */
    public static ArchCondition<JavaClass> accessClassesIn(String packageIdentifier) {
        return accessClassesInAnyPackage(packageIdentifier).
                as(String.format("access classes that reside in '%s'", packageIdentifier));
    }

    /**
     * @param packageIdentifiers Strings identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching any of the identifiers
     */
    public static ArchCondition<JavaClass> accessClassesInAnyPackage(String... packageIdentifiers) {
        return new ClassAccessesAnyPackageCondition(packageIdentifiers);
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching accesses by packages matching any of the identifiers
     */
    public static ArchCondition<JavaClass> onlyBeAccessedByAnyPackage(String... packageIdentifiers) {
        return new ClassIsOnlyAccessedByAnyPackageCondition(packageIdentifiers);
    }

    public static ArchCondition<JavaClass> getField(final Class<?> clazz, final String fieldName) {
        return getFieldWhere(ownerAndNameAre(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> getFieldWhere(DescribedPredicate<JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate);
    }

    public static ArchCondition<JavaClass> setField(final Class<?> clazz, final String fieldName) {
        return setFieldWhere(ownerAndNameAre(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> setFieldWhere(DescribedPredicate<JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate);
    }

    public static ArchCondition<JavaClass> accessField(final Class<?> clazz, final String fieldName) {
        return accessFieldWhere(ownerAndNameAre(clazz, fieldName));
    }

    public static ArchCondition<JavaClass> accessFieldWhere(DescribedPredicate<JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate);
    }

    public static MethodCallConditionCreator callMethod(final String methodName, Class<?>... paramTypes) {
        return new MethodCallConditionCreator(methodName, paramTypes);
    }

    public static ArchCondition<JavaClass> callMethodWhere(DescribedPredicate<JavaCall<?>> predicate) {
        return new ClassCallsMethodCondition(predicate);
    }

    public static ArchCondition<JavaClass> resideInAPackage(String packageIdentifier) {
        return new ClassResidesInCondition(packageIdentifier);
    }

    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containAnyElementThat(ArchCondition<T> condition) {
        return new ContainsAnyCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containOnlyElementsThat(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }

    public static class MethodCallConditionCreator {
        private final String methodName;
        private final Class<?>[] params;

        private MethodCallConditionCreator(String methodName, Class<?>[] params) {
            this.methodName = methodName;
            this.params = params;
        }

        public ArchCondition<JavaClass> in(Class<?> clazz) {
            return callMethodWhere(callTarget().is(clazz, methodName, params));
        }

        public ArchCondition<JavaClass> inHierarchyOf(Class<?> type) {
            return callMethodWhere(callTarget()
                    .isDeclaredIn(theHierarchyOf(type))
                    .hasName(methodName)
                    .hasParameters(params));
        }
    }
}
