package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Annotation;
import java.util.Collection;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.parameterTypes;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.be;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static java.util.Arrays.asList;

public final class ArchConditions {
    private ArchConditions() {
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> getField(final Class<?> owner, final String fieldName) {
        return getField(owner.getName(), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> getField(final String ownerName, final String fieldName) {
        return getFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("get field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate)
                .as("get field where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> setField(final Class<?> owner, final String fieldName) {
        return setField(owner.getName(), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> setField(final String ownerName, final String fieldName) {
        return setFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("set field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate)
                .as("set field where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessField(final Class<?> owner, final String fieldName) {
        return accessField(owner.getName(), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessField(final String ownerName, final String fieldName) {
        return accessFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("access field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate)
                .as("access field where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        return callMethodWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(methodName)))
                .and(JavaCall.Predicates.target(parameterTypes(parameterTypes))))
                .as("call method %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), methodName, namesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(String ownerName, String methodName, String... parameterTypeNames) {
        return callMethodWhere(JavaCall.Predicates.target(With.<JavaClass>owner(name(ownerName)))
                .and(JavaCall.Predicates.target(name(methodName)))
                .and(JavaCall.Predicates.target(parameterTypes(parameterTypeNames))))
                .as("call method %s", Formatters.formatMethodSimple(
                        ensureSimpleName(ownerName), methodName, asList(parameterTypeNames)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethodWhere(final DescribedPredicate<? super JavaMethodCall> predicate) {
        return new ClassCallsCodeUnitCondition(new DescribedPredicate<JavaCall<?>>("") {
            @Override
            public boolean apply(JavaCall<?> input) {
                return input instanceof JavaMethodCall && predicate.apply((JavaMethodCall) input);
            }
        }).as("call method where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(Class<?> owner, Class<?>... parameterTypes) {
        return callConstructorWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(CONSTRUCTOR_NAME)))
                .and(JavaCall.Predicates.target(parameterTypes(parameterTypes))))
                .as("call constructor %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), CONSTRUCTOR_NAME, namesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(String ownerName, String... parameterTypeNames) {
        return callConstructorWhere(JavaCall.Predicates.target(With.<JavaClass>owner(name(ownerName)))
                .and(JavaCall.Predicates.target(name(CONSTRUCTOR_NAME)))
                .and(JavaCall.Predicates.target(parameterTypes(parameterTypeNames))))
                .as("call constructor %s", Formatters.formatMethodSimple(
                        ensureSimpleName(ownerName), CONSTRUCTOR_NAME, asList(parameterTypeNames)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructorWhere(final DescribedPredicate<? super JavaConstructorCall> predicate) {
        return new ClassCallsCodeUnitCondition(new DescribedPredicate<JavaCall<?>>("") {
            @Override
            public boolean apply(JavaCall<?> input) {
                return input instanceof JavaConstructorCall && predicate.apply((JavaConstructorCall) input);
            }
        }).as("call constructor where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return new ClassCallsCodeUnitCondition(predicate);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessTargetWhere(DescribedPredicate<? super JavaAccess<?>> predicate) {
        return new AnyAccessFromClassCondition("access target where", predicate);
    }

    // FIXME: Is inheritance of an interface an access??
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        @SuppressWarnings({"RedundantTypeArguments", "unchecked"})
        ChainableFunction<JavaAccess, AccessTarget> getTarget =
                JavaAccess.Functions.Get.<JavaAccess, AccessTarget>target(); // This seems to be a compiler nightmare...
        return new AnyAccessFromClassCondition("access classes that",
                getTarget.then(Get.<JavaClass>owner()).is(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyBeAccessedByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return new AllAccessesToClassCondition("only be accessed by classes that",
                JavaAccess.Functions.Get.origin().then(Get.<JavaClass>owner()).is(predicate));
    }

    /**
     * @param packageIdentifier A String identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching the identifier
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessClassesThatResideIn(String packageIdentifier) {
        return accessClassesThatResideInAnyPackage(packageIdentifier).
                as("access classes that reside in package '%s'", packageIdentifier);
    }

    /**
     * @param packageIdentifiers Strings identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching any of the identifiers
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessClassesThatResideInAnyPackage(String... packageIdentifiers) {
        return new AnyAccessFromClassCondition("access classes that reside in",
                JavaAccessPackagePredicate.forAccessTarget().matching(packageIdentifiers));
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching accesses by packages matching any of the identifiers
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyBeAccessedByAnyPackage(String... packageIdentifiers) {
        return new AllAccessesToClassCondition("only be accessed by",
                JavaAccessPackagePredicate.forAccessOrigin().matching(packageIdentifiers));
    }


    @PublicAPI(usage = ACCESS)
    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    private static <T> ArchCondition<T> not(ArchCondition<T> condition) {
        return never(condition).as("not " + condition.getDescription());
    }

    static <T> ArchCondition<Collection<? extends T>> containAnyElementThat(ArchCondition<T> condition) {
        return new ContainAnyCondition<>(condition);
    }

    static <T> ArchCondition<Collection<? extends T>> containOnlyElementsThat(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }

    private static DescribedPredicate<? super JavaFieldAccess> ownerAndNameAre(String ownerName, final String fieldName) {
        return JavaFieldAccess.Predicates.target(With.<JavaClass>owner(name(ownerName)))
                .and(JavaFieldAccess.Predicates.target(name(fieldName)))
                .as(ownerName + "." + fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beNamed(final String name) {
        final DescribedPredicate<HasName> beNamed = name(name).as("be named '%s'", name);
        return new ArchCondition<JavaClass>(beNamed.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = beNamed.apply(item);
                String message = String.format("class %s is %snamed '%s'",
                        item.getName(), satisfied ? "" : "not ", name);
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeNamed(String name) {
        return not(beNamed(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleName(final String name) {
        final DescribedPredicate<JavaClass> haveSimpleName = have(simpleName(name));
        return new ArchCondition<JavaClass>(haveSimpleName.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = haveSimpleName.apply(item);
                String message = String.format("class %s %s simple name '%s'",
                        item.getName(), satisfied ? "has" : "doesn't have", name);
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notHaveSimpleName(String name) {
        return not(haveSimpleName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveNameMatching(final String regex) {
        final DescribedPredicate<HasName> haveNameMatching = have(nameMatching(regex));
        return new ArchCondition<JavaClass>(haveNameMatching.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = haveNameMatching.apply(item);
                String infix = satisfied ? "matches" : "doesn't match";
                String message = String.format("class %s %s '%s'", item.getName(), infix, regex);
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveNameNotMatching(String regex) {
        return not(haveNameMatching(regex)).as("have name not matching '%s'", regex);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAPackage(final String packageIdentifier) {
        return residesConditionForPredicate(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAnyPackage(String... packageIdentifiers) {
        return residesConditionForPredicate(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackage(String packageIdentifier) {
        return residesConditionForPredicate(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackages(String... packageIdentifiers) {
        return residesConditionForPredicate(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    private static ArchCondition<JavaClass> residesConditionForPredicate(final DescribedPredicate<JavaClass> resideInAPackage) {
        return new ArchCondition<JavaClass>(resideInAPackage.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = resideInAPackage.apply(item);
                String message = String.format("Class %s %s %s",
                        item.getName(), satisfied ? "does" : "doesn't", resideInAPackage.getDescription());
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveModifier(final JavaModifier modifier) {
        final DescribedPredicate<HasModifiers> haveModifier = have(modifier(modifier));
        return new ArchCondition<JavaClass>(haveModifier.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = haveModifier.apply(item);
                String message = String.format("class %s %s modifier %s",
                        item.getName(), satisfied ? "has" : "doesn't have", modifier);
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notHaveModifier(final JavaModifier modifier) {
        return not(haveModifier(modifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> bePublic() {
        return haveModifier(JavaModifier.PUBLIC).as("be public");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBePublic() {
        return not(haveModifier(JavaModifier.PUBLIC)).as("not be public");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beProtected() {
        return haveModifier(JavaModifier.PROTECTED).as("be protected");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeProtected() {
        return not(haveModifier(JavaModifier.PROTECTED)).as("not be protected");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> bePackagePrivate() {
        return not(notBePackagePrivate()).as("be package private");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBePackagePrivate() {
        return haveModifier(JavaModifier.PUBLIC)
                .or(haveModifier(JavaModifier.PROTECTED))
                .or(haveModifier(JavaModifier.PRIVATE))
                .as("not be package private");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> bePrivate() {
        return haveModifier(JavaModifier.PRIVATE).as("be private");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBePrivate() {
        return not(haveModifier(JavaModifier.PRIVATE)).as("not be private");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAnnotatedWith(Class<? extends Annotation> type) {
        return createAnnotatedCondition(HasAnnotations.Predicates.annotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAnnotatedWith(Class<? extends Annotation> type) {
        return not(beAnnotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAnnotatedWith(String typeName) {
        return createAnnotatedCondition(HasAnnotations.Predicates.annotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAnnotatedWith(String typeName) {
        return not(beAnnotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAnnotatedWith(final DescribedPredicate<? super JavaAnnotation> predicate) {
        return createAnnotatedCondition(HasAnnotations.Predicates.annotatedWith(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return not(beAnnotatedWith(predicate));
    }

    private static ArchCondition<JavaClass> createAnnotatedCondition(final DescribedPredicate<CanBeAnnotated> annotatedWith) {
        return new ArchCondition<JavaClass>(be(annotatedWith).getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = annotatedWith.apply(item);
                String message = String.format("class %s is %s%s",
                        item.getName(), satisfied ? "" : "not ", annotatedWith.getDescription());
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(Class<?> interfaceType) {
        return createImplementsCondition(implementPredicate(assignableTo(interfaceType)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(Class<?> interfaceType) {
        return not(implement(interfaceType));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(String interfaceTypeName) {
        return createImplementsCondition(implementPredicate(assignableTo(interfaceTypeName)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(String interfaceTypeName) {
        return not(implement(interfaceTypeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(DescribedPredicate<? super JavaClass> predicate) {
        return createImplementsCondition(implementPredicate(assignableTo(predicate)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(DescribedPredicate<? super JavaClass> predicate) {
        return not(implement(predicate));
    }

    // Conscious copy to keep visibility reduced -> ClassesThatPredicates
    private static DescribedPredicate<JavaClass> implementPredicate(DescribedPredicate<JavaClass> assignablePredicate) {
        return DescribedPredicate.not(INTERFACES).and(assignablePredicate)
                .as(assignablePredicate.getDescription().replace("assignable to", "implement"));
    }

    private static ArchCondition<JavaClass> createImplementsCondition(final DescribedPredicate<? super JavaClass> implement) {
        return new ArchCondition<JavaClass>(implement.getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = implement.apply(item);
                String description = satisfied
                        ? implement.getDescription().replace("implement", "implements")
                        : implement.getDescription().replace("implement", "doesn't implement");
                String message = String.format("class %s %s", item.getName(), description);
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(Class<?> type) {
        return createAssignableCondition(JavaClass.Predicates.assignableTo(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(Class<?> type) {
        return not(beAssignableTo(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(String typeName) {
        return createAssignableCondition(JavaClass.Predicates.assignableTo(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(String typeName) {
        return not(beAssignableTo(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return createAssignableCondition(JavaClass.Predicates.assignableTo(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableTo(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(Class<?> type) {
        return createAssignableCondition(JavaClass.Predicates.assignableFrom(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(Class<?> type) {
        return not(beAssignableFrom(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(String typeName) {
        return createAssignableCondition(JavaClass.Predicates.assignableFrom(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(String typeName) {
        return not(beAssignableFrom(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return createAssignableCondition(JavaClass.Predicates.assignableFrom(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableFrom(predicate));
    }

    private static ArchCondition<JavaClass> createAssignableCondition(final DescribedPredicate<JavaClass> assignable) {
        return new ArchCondition<JavaClass>(be(assignable).getDescription()) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = assignable.apply(item);
                String message = String.format("class %s is %s%s",
                        item.getName(), satisfied ? "" : "not ", assignable.getDescription());
                events.add(new SimpleConditionEvent<>(item, satisfied, message));
            }
        };
    }
}
