package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.AccessTarget;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.core.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.fieldAccessTarget;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withName;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.callTarget;

public final class ArchConditions {
    private ArchConditions() {
    }

    /**
     * @param packageIdentifier A String identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching the identifier
     */
    public static ArchCondition<JavaClass> accessClassesThatResideIn(String packageIdentifier) {
        return accessClassesThatResideInAnyPackage(packageIdentifier).
                as("access classes that reside in package '%s'", packageIdentifier);
    }

    /**
     * @param packageIdentifiers Strings identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching any of the identifiers
     */
    public static ArchCondition<JavaClass> accessClassesThatResideInAnyPackage(String... packageIdentifiers) {
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
        return getFieldWhere(ownerAndNameAre(clazz, fieldName))
                .as("get field %s.%s", clazz.getSimpleName(), fieldName);
    }

    public static ArchCondition<JavaClass> getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate)
                .as("get field where " + predicate.getDescription());
    }

    public static ArchCondition<JavaClass> setField(final Class<?> clazz, final String fieldName) {
        return setFieldWhere(ownerAndNameAre(clazz, fieldName))
                .as("set field %s.%s", clazz.getSimpleName(), fieldName);
    }

    public static ArchCondition<JavaClass> setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate)
                .as("set field where " + predicate.getDescription());
    }

    public static ArchCondition<JavaClass> accessField(final Class<?> clazz, final String fieldName) {
        return accessFieldWhere(ownerAndNameAre(clazz, fieldName))
                .as("access field %s.%s", clazz.getSimpleName(), fieldName);
    }

    public static ArchCondition<JavaClass> accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate)
                .as("access field where " + predicate.getDescription());
    }

    public static MethodCallConditionCreator callMethod(final String methodName, Class<?>... paramTypes) {
        return new MethodCallConditionCreator(methodName, paramTypes);
    }

    public static ArchCondition<JavaClass> callMethodWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return new ClassCallsMethodCondition(predicate);
    }

    public static ArchCondition<JavaClass> accessClass(final DescribedPredicate<? super JavaClass> predicate) {
        return new ClassAccessesCondition(predicate);
    }

    public static ArchCondition<JavaClass> resideInAPackage(String packageIdentifier) {
        return new ClassResidesInCondition(packageIdentifier);
    }

    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containAnyElementThat(ArchCondition<T> condition) {
        return new ContainAnyCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containOnlyElementsThat(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }

    private static DescribedPredicate<JavaFieldAccess> ownerAndNameAre(final Class<?> target, final String fieldName) {
        return fieldAccessTarget(With.<JavaClass>owner(withName(target.getName())))
                .and(fieldAccessTarget(withName(fieldName)))
                .as(target.getName() + "." + fieldName);
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
                    .isDeclaredIn(assignableTo(type))
                    .hasName(methodName)
                    .hasParameterTypes(params));
        }
    }

    private static class ClassAccessesCondition extends AnyAttributeMatchesCondition<JavaAccess<?>> {
        ClassAccessesCondition(final DescribedPredicate<? super JavaClass> predicate) {
            super(new JavaAccessCondition(accessWithOwnerWith(predicate)));
        }

        private static DescribedPredicate<JavaAccess<?>> accessWithOwnerWith(DescribedPredicate<? super JavaClass> predicate) {
            ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
            return predicate.onResultOf(Get.<JavaClass>owner().after(getTarget))
                    .as("access class " + predicate.getDescription());
        }

        @Override
        Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
            return item.getAccessesFromSelf();
        }
    }
}
