/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.domain.PackageMatchers;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchCondition.ConditionByPredicate;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.optionalContains;
import static com.tngtech.archunit.base.DescribedPredicate.optionalEmpty;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.Formatters.formatNamesOf;
import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_ACCESSES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_ACCESSES_TO_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CODE_UNIT_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTORS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTOR_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_DIRECT_DEPENDENCIES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_DIRECT_DEPENDENCIES_TO_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELDS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELD_ACCESSES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_METHOD_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_PACKAGE_NAME;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANONYMOUS_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INNER_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.LOCAL_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.MEMBER_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.NESTED_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.RECORDS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.TOP_LEVEL_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaCodeUnit.Functions.Get.GET_CALLS_OF_SELF;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullName;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameContaining;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameEndingWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameStartingWith;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.rawType;
import static java.util.Arrays.asList;

/**
 * A collection of predefined {@link ArchCondition ArchConditions} that can be customized or joined together
 * via {@link ArchCondition#and(ArchCondition)} and {@link ArchCondition#or(ArchCondition)}.
 */
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
    public static ArchCondition<JavaClass> onlyAccessFieldsThat(final DescribedPredicate<? super JavaField> predicate) {
        ChainableFunction<JavaFieldAccess, FieldAccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaFieldAccess> accessPredicate = getTarget.then(FieldAccessTarget.Functions.RESOLVE_MEMBER)
                .is(optionalContains(predicate.<JavaField>forSubtype()).or(optionalEmpty()));
        return new ClassOnlyAccessesCondition<>(accessPredicate, GET_FIELD_ACCESSES_FROM_SELF)
                .as("only access fields that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        return callMethodWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(methodName)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypes))))
                .as("call method %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), methodName, formatNamesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(String ownerName, String methodName, String... parameterTypeNames) {
        return callMethodWhere(JavaCall.Predicates.target(With.owner(name(ownerName)))
                .and(JavaCall.Predicates.target(name(methodName)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypeNames))))
                .as("call method %s", Formatters.formatMethodSimple(
                        ensureSimpleName(ownerName), methodName, asList(parameterTypeNames)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethodWhere(final DescribedPredicate<? super JavaMethodCall> predicate) {
        return new ClassAccessesCondition<>(predicate, GET_METHOD_CALLS_FROM_SELF)
                .as("call method where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyCallMethodsThat(final DescribedPredicate<? super JavaMethod> predicate) {
        ChainableFunction<JavaMethodCall, MethodCallTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaMethodCall> callPredicate = getTarget.then(MethodCallTarget.Functions.RESOLVE_MEMBER)
                .is(optionalContains(predicate.<JavaMethod>forSubtype()).or(optionalEmpty()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_METHOD_CALLS_FROM_SELF)
                .as("only call methods that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(Class<?> owner, Class<?>... parameterTypes) {
        return callConstructorWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(CONSTRUCTOR_NAME)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypes))))
                .as("call constructor %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), CONSTRUCTOR_NAME, formatNamesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(String ownerName, String... parameterTypeNames) {
        return callConstructorWhere(JavaCall.Predicates.target(With.owner(name(ownerName)))
                .and(JavaCall.Predicates.target(name(CONSTRUCTOR_NAME)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypeNames))))
                .as("call constructor %s", Formatters.formatMethodSimple(
                        ensureSimpleName(ownerName), CONSTRUCTOR_NAME, asList(parameterTypeNames)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructorWhere(final DescribedPredicate<? super JavaConstructorCall> predicate) {
        return new ClassAccessesCondition<>(predicate, GET_CONSTRUCTOR_CALLS_FROM_SELF)
                .as("call constructor where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyCallConstructorsThat(final DescribedPredicate<? super JavaConstructor> predicate) {
        ChainableFunction<JavaConstructorCall, ConstructorCallTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaConstructorCall> callPredicate = getTarget.then(ConstructorCallTarget.Functions.RESOLVE_MEMBER)
                .is(optionalContains(predicate.<JavaConstructor>forSubtype()).or(optionalEmpty()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_CONSTRUCTOR_CALLS_FROM_SELF)
                .as("only call constructors that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return new ClassAccessesCondition<>(predicate, GET_CODE_UNIT_CALLS_FROM_SELF)
                .as("call code unit where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyCallCodeUnitsThat(final DescribedPredicate<? super JavaCodeUnit> predicate) {
        ChainableFunction<JavaCall<?>, CodeUnitCallTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaCall<?>> callPredicate = getTarget.then(CodeUnitCallTarget.Functions.RESOLVE_MEMBER)
                .is(optionalContains(predicate.<JavaCodeUnit>forSubtype()).or(optionalEmpty()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_CODE_UNIT_CALLS_FROM_SELF)
                .as("only call code units that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyAccessMembersThat(final DescribedPredicate<? super JavaMember> predicate) {
        ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(AccessTarget.Functions.RESOLVE_MEMBER)
                .is(optionalContains(predicate.<JavaMember>forSubtype()).or(optionalEmpty()));
        return new ClassOnlyAccessesCondition<>(accessPredicate, GET_ACCESSES_FROM_SELF)
                .as("only access members that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessTargetWhere(DescribedPredicate<? super JavaAccess<?>> predicate) {
        return new ClassAccessesCondition<>(predicate, GET_ACCESSES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> accessClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(Get.owner()).is(predicate);
        return new ClassAccessesCondition<>(accessPredicate, GET_ACCESSES_FROM_SELF)
                .as("access classes that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyAccessClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(Get.owner()).is(predicate);
        return new AllAccessesCondition("only access classes that", accessPredicate, GET_ACCESSES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> dependOnClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        return new AnyDependencyCondition(
                "depend on classes that " + predicate.getDescription(),
                GET_TARGET_CLASS.is(predicate),
                GET_DIRECT_DEPENDENCIES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> transitivelyDependOnClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        return new TransitiveDependencyCondition(predicate);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyDependOnClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        return new AllDependenciesCondition(
                "only depend on classes that " + predicate.getDescription(),
                GET_TARGET_CLASS.is(predicate),
                GET_DIRECT_DEPENDENCIES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyBeAccessedByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return new AllAccessesCondition("only be accessed by classes that",
                JavaAccess.Functions.Get.origin().then(Get.owner()).is(predicate), GET_ACCESSES_TO_SELF);
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
        JavaAccessPackagePredicate predicate = JavaAccessPackagePredicate.forAccessTarget().matching(packageIdentifiers);
        return new ClassAccessesCondition<>(predicate, GET_ACCESSES_FROM_SELF)
                .as("access classes that reside in " + predicate);
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching accesses by packages matching any of the identifiers
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyBeAccessedByAnyPackage(String... packageIdentifiers) {
        return new AllAccessesCondition("only be accessed by",
                JavaAccessPackagePredicate.forAccessOrigin().matching(packageIdentifiers), GET_ACCESSES_TO_SELF);
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching {@link JavaClass classes} that have other classes
     * depending on them (e.g. calling methods of this class)
     * with a package matching any of the identifiers
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyHaveDependentsInAnyPackage(String... packageIdentifiers) {
        String description = String.format("only have dependents in any package [%s]", joinSingleQuoted(packageIdentifiers));
        return onlyHaveDependentsWhere(dependencyOrigin(GET_PACKAGE_NAME.is(PackageMatchers.of(packageIdentifiers))))
                .as(description);
    }

    /**
     * @param predicate A predicate specifying allowed dependencies on this class
     * @return A condition satisfied by {@link JavaClass classes} where all classes
     * depending on them (e.g. calling methods of this class) are matched by the predicate
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyHaveDependentClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return onlyHaveDependentsWhere(GET_ORIGIN_CLASS.is(predicate))
                .as("only have dependent classes that " + predicate.getDescription());
    }

    /**
     * @param predicate A predicate specifying allowed dependencies on this class
     * @return A condition satisfied by {@link JavaClass classes} where all {@link Dependency dependencies}
     * on them (e.g. calling methods of this class) are matched by the predicate
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyHaveDependentsWhere(DescribedPredicate<? super Dependency> predicate) {
        String description = "only have dependents where " + predicate.getDescription();
        return new AllDependenciesCondition(description, predicate, GET_DIRECT_DEPENDENCIES_TO_SELF);
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching {@link JavaClass classes} that only depend on
     * other classes (e.g. this class calling methods of other classes)
     * with a package matching any of the identifiers
     */
    @PublicAPI(usage = ACCESS)
    public static AllDependenciesCondition onlyHaveDependenciesInAnyPackage(String... packageIdentifiers) {
        String description = String.format("only have dependencies in any package [%s]", joinSingleQuoted(packageIdentifiers));
        return onlyHaveDependenciesWhere(dependencyTarget(GET_PACKAGE_NAME.is(PackageMatchers.of(packageIdentifiers))))
                .as(description);
    }

    /**
     * @param predicate A predicate identifying relevant dependencies from this class to other classes
     * @return A condition matching {@link JavaClass classes} that depend on
     * other classes (e.g. this class calling methods of other classes)
     * where the respective dependency is matched by the predicate
     */
    @PublicAPI(usage = ACCESS)
    public static AllDependenciesCondition onlyHaveDependenciesWhere(DescribedPredicate<? super Dependency> predicate) {
        String description = "only have dependencies where " + predicate.getDescription();
        return new AllDependenciesCondition(description, predicate, GET_DIRECT_DEPENDENCIES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> ArchCondition<T> and(ArchCondition<T> first, ArchCondition<T> second) {
        return new AndCondition<>(first, second);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> ArchCondition<T> or(ArchCondition<T> first, ArchCondition<T> second) {
        return new OrCondition<>(first, second);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> ArchCondition<T> not(ArchCondition<T> condition) {
        return never(condition).as("not " + condition.getDescription());
    }

    static <T> ArchCondition<Collection<? extends T>> containAnyElementThat(ArchCondition<T> condition) {
        return new ContainAnyCondition<>(condition);
    }

    static <T> ArchCondition<Collection<? extends T>> containOnlyElementsThat(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }

    private static DescribedPredicate<? super JavaFieldAccess> ownerAndNameAre(String ownerName, final String fieldName) {
        return JavaFieldAccess.Predicates.target(With.owner(name(ownerName)))
                .and(JavaFieldAccess.Predicates.target(name(fieldName)))
                .as(ownerName + "." + fieldName);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> be(final Class<?> clazz) {
        return be(clazz.getName());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBe(final Class<?> clazz) {
        return not(be(clazz));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> be(final String className) {
        return be(fullyQualifiedName(className).as(className))
                .describeEventsBy((__, satisfied) -> (satisfied ? "is " : "is not ") + className)
                .forSubtype();
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBe(final String className) {
        return not(be(className));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveName(final String name) {
        return have(name(name));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> notHaveName(String name) {
        return not(haveName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> haveFullName(String fullName) {
        return have(fullName(fullName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> notHaveFullName(String fullName) {
        return not(haveFullName(fullName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveFullyQualifiedName(final String name) {
        return have(fullyQualifiedName(name));
    }

    @Internal
    public static DescribedPredicate<HasName> fullyQualifiedName(String name) {
        DescribedPredicate<HasName> predicate = name(name);
        return predicate.as("fully qualified " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notHaveFullyQualifiedName(String name) {
        return not(haveFullyQualifiedName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleName(final String name) {
        return have(simpleName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notHaveSimpleName(String name) {
        return not(haveSimpleName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameStartingWith(final String prefix) {
        return have(simpleNameStartingWith(prefix));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotStartingWith(String prefix) {
        return not(haveSimpleNameStartingWith(prefix)).as("have simple name not starting with '%s'", prefix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameContaining(final String infix) {
        return have(simpleNameContaining(infix));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotContaining(final String infix) {
        return not(haveSimpleNameContaining(infix)).as("have simple name not containing '%s'", infix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameEndingWith(final String suffix) {
        return have(simpleNameEndingWith(suffix));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotEndingWith(String suffix) {
        return not(haveSimpleNameEndingWith(suffix)).as("have simple name not ending with '%s'", suffix);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameMatching(final String regex) {
        return have(nameMatching(regex));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameNotMatching(String regex) {
        return not(ArchConditions.<HAS_NAME>haveNameMatching(regex)).as("have name not matching '%s'", regex);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> haveFullNameMatching(String regex) {
        return have(fullNameMatching(regex));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_FULL_NAME>
    haveFullNameNotMatching(String regex) {
        return not(ArchConditions.<HAS_FULL_NAME>haveFullNameMatching(regex)).as("have full name not matching '%s'", regex);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameStartingWith(String prefix) {
        return have(nameStartingWith(prefix));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameNotStartingWith(String prefix) {
        return not(haveNameStartingWith(prefix)).<HAS_NAME>forSubtype().as("have name not starting with '%s'", prefix);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameContaining(String infix) {
        return have(nameContaining(infix));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameNotContaining(String infix) {
        return not(haveNameContaining(infix)).<HAS_NAME>forSubtype().as("have name not containing '%s'", infix);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameEndingWith(String suffix) {
        return have(nameEndingWith(suffix));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameNotEndingWith(String suffix) {
        return not(haveNameEndingWith(suffix)).<HAS_NAME>forSubtype().as("have name not ending with '%s'", suffix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAPackage(final String packageIdentifier) {
        return does(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAnyPackage(String... packageIdentifiers) {
        return does(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackage(String packageIdentifier) {
        return does(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackages(String... packageIdentifiers) {
        return does(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> haveModifier(
            final JavaModifier modifier) {
        return have(modifier(modifier));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notHaveModifier(
            final JavaModifier modifier) {
        return not(ArchConditions.haveModifier(modifier));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> bePublic() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PUBLIC).as("be public");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBePublic() {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PUBLIC)).as("not be public");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> beProtected() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PROTECTED).as("be protected");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBeProtected() {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PROTECTED)).as("not be protected");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> bePackagePrivate() {
        return not(ArchConditions.<HAS_MODIFIERS>notBePackagePrivate()).as("be package private");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBePackagePrivate() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PUBLIC)
                .or(haveModifier(JavaModifier.PROTECTED))
                .or(haveModifier(JavaModifier.PRIVATE))
                .as("not be package private");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> bePrivate() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PRIVATE).as("be private");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBePrivate() {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(JavaModifier.PRIVATE)).as("not be private");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> beStatic() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(STATIC).as("be static");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBeStatic() {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(STATIC).as("be static"));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> beFinal() {
        return ArchConditions.<HAS_MODIFIERS>haveModifier(FINAL).as("be final");
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notBeFinal() {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(FINAL).as("be final"));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveOnlyFinalFields() {
        return new HaveOnlyModifiersCondition<>("final fields", FINAL, GET_FIELDS);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveOnlyPrivateConstructors() {
        return new HaveOnlyModifiersCondition<>("private constructors", PRIVATE, GET_CONSTRUCTORS);
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#annotatedWith(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            Class<? extends Annotation> type) {
        return be(annotatedWith(type));
    }

    /**
     * @return negation of {@link #beAnnotatedWith(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            Class<? extends Annotation> type) {
        return not(ArchConditions.beAnnotatedWith(type));
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#annotatedWith(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            String typeName) {
        return be(annotatedWith(typeName));
    }

    /**
     * @return negation of {@link #beAnnotatedWith(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            String typeName) {
        return not(ArchConditions.beAnnotatedWith(typeName));
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#annotatedWith(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return be(annotatedWith(predicate));
    }

    /**
     * @return negation of {@link #beAnnotatedWith(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return not(ArchConditions.beAnnotatedWith(predicate));
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#metaAnnotatedWith(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            Class<? extends Annotation> type) {
        return be(metaAnnotatedWith(type));
    }

    /**
     * @return negation of {@link #beMetaAnnotatedWith(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            Class<? extends Annotation> type) {
        return not(ArchConditions.beMetaAnnotatedWith(type));
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#metaAnnotatedWith(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            String typeName) {
        return be(metaAnnotatedWith(typeName));
    }

    /**
     * @return negation of {@link #beMetaAnnotatedWith(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            String typeName) {
        return not(ArchConditions.beMetaAnnotatedWith(typeName));
    }

    /**
     * @return a condition matching elements analogously to {@link CanBeAnnotated.Predicates#metaAnnotatedWith(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return be(metaAnnotatedWith(predicate));
    }

    /**
     * @return negation of {@link #beMetaAnnotatedWith(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return not(ArchConditions.beMetaAnnotatedWith(predicate));
    }

    /**
     * @return a condition matching classes analogously to {@link JavaClass.Predicates#implement(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(Class<?> interfaceType) {
        return does(JavaClass.Predicates.implement(interfaceType));
    }

    /**
     * @return negation of {@link #implement(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(Class<?> interfaceType) {
        return not(implement(interfaceType));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#implement(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(String interfaceTypeName) {
        return does(JavaClass.Predicates.implement(interfaceTypeName));
    }

    /**
     * @return negation of {@link #implement(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(String interfaceTypeName) {
        return not(implement(interfaceTypeName));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#implement(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(DescribedPredicate<? super JavaClass> predicate) {
        return does(JavaClass.Predicates.implement(predicate));
    }

    /**
     * @return negation of {@link #implement(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(DescribedPredicate<? super JavaClass> predicate) {
        return not(implement(predicate));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableTo(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(Class<?> type) {
        return be(assignableTo(type));
    }

    /**
     * @return negation of {@link #beAssignableTo(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(Class<?> type) {
        return not(beAssignableTo(type));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableTo(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(String typeName) {
        return be(assignableTo(typeName));
    }

    /**
     * @return negation of {@link #beAssignableTo(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(String typeName) {
        return not(beAssignableTo(typeName));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableTo(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return be(assignableTo(predicate));
    }

    /**
     * @return negation of {@link #beAssignableTo(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableTo(predicate));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableFrom(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(Class<?> type) {
        return be(assignableFrom(type));
    }

    /**
     * @return negation of {@link #beAssignableFrom(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(Class<?> type) {
        return not(beAssignableFrom(type));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableFrom(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(String typeName) {
        return be(assignableFrom(typeName));
    }

    /**
     * @return negation of {@link #beAssignableFrom(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(String typeName) {
        return not(beAssignableFrom(typeName));
    }

    /**
     * @return A condition matching classes analogously to {@link JavaClass.Predicates#assignableFrom(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return be(assignableFrom(predicate));
    }

    /**
     * @return negation of {@link #beAssignableFrom(DescribedPredicate)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableFrom(predicate));
    }

    /**
     * @return A condition matching interfaces as defined in {@link JavaClass#isInterface()}
     * @see #notBeInterfaces()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beInterfaces() {
        return be(INTERFACES).describeEventsBy((__, satisfied) -> (satisfied ? "is an" : "is no") + " interface");
    }

    /**
     * @return negation of {@link #beInterfaces()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeInterfaces() {
        return not(beInterfaces());
    }

    /**
     * @return A condition matching classes that are enums as defined in {@link JavaClass#isEnum()}
     * @see #notBeEnums()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beEnums() {
        return be(ENUMS).describeEventsBy((__, satisfied) -> (satisfied ? "is an" : "is no") + " enum");
    }

    /**
     * @return negation of {@link #beEnums()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeEnums() {
        return not(beEnums());
    }

    /**
     * @return A condition matching classes that are records according to the Java Language Specification
     * as defined in {@link JavaClass#isRecord()}
     * @see #notBeRecords()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beRecords() {
        return be(RECORDS).describeEventsBy((__, satisfied) -> (satisfied ? "is a" : "is no") + " record");
    }

    /**
     * @return negation of {@link #beRecords()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeRecords() {
        return not(beRecords());
    }

    /**
     * @return A condition matching classes that are top level classes (not nested classes)
     * as defined in {@link JavaClass#isTopLevelClass()}
     * @see #notBeTopLevelClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beTopLevelClasses() {
        return BE_TOP_LEVEL_CLASSES;
    }

    /**
     * @return negation of {@link #beTopLevelClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeTopLevelClasses() {
        return not(BE_TOP_LEVEL_CLASSES);
    }

    /**
     * @return A condition matching classes that are nested classes as defined in {@link JavaClass#isNestedClass()}
     * @see #notBeNestedClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beNestedClasses() {
        return BE_NESTED_CLASSES;
    }

    /**
     * @return negation of {@link #beNestedClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeNestedClasses() {
        return not(BE_NESTED_CLASSES);
    }

    /**
     * @return A condition matching classes that are member classes as defined in {@link JavaClass#isMemberClass()}
     * @see #notBeMemberClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beMemberClasses() {
        return BE_MEMBER_CLASSES;
    }

    /**
     * @return negation of {@link #beMemberClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeMemberClasses() {
        return not(BE_MEMBER_CLASSES);
    }

    /**
     * @return A condition matching classes that are inner classes as defined in {@link JavaClass#isInnerClass()}
     * @see #notBeInnerClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beInnerClasses() {
        return BE_INNER_CLASSES;
    }

    /**
     * @return negation of {@link #beInnerClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeInnerClasses() {
        return not(BE_INNER_CLASSES);
    }

    /**
     * @return A condition matching classes that are anonymous as defined in {@link JavaClass#isAnonymousClass()}
     * @see #notBeAnonymousClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAnonymousClasses() {
        return BE_ANONYMOUS_CLASSES;
    }

    /**
     * @return negation of {@link #beAnonymousClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAnonymousClasses() {
        return not(BE_ANONYMOUS_CLASSES);
    }

    /**
     * @return A condition matching classes that are local as defined in {@link JavaClass#isLocalClass()}
     * @see #notBeLocalClasses()
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beLocalClasses() {
        return BE_LOCAL_CLASSES;
    }

    /**
     * @return negation of {@link #beLocalClasses()}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeLocalClasses() {
        return not(BE_LOCAL_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static <T extends HasName.AndFullName> ArchCondition<T> containNumberOfElements(DescribedPredicate<? super Integer> predicate) {
        return new NumberOfElementsCondition<>(predicate);
    }

    /**
     * @return A condition matching members that are declared in the specified class
     * @see #notBeDeclaredIn(Class)
     * @see #beDeclaredIn(String)
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredIn(Class<?> owner) {
        return be(declaredIn(owner));
    }

    /**
     * @return negation of {@link #beDeclaredIn(Class)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> notBeDeclaredIn(Class<?> owner) {
        return not(beDeclaredIn(owner));
    }

    /**
     * @return like {@link #beDeclaredIn(Class)} but the class is specified by the fully qualified class name
     * @see #notBeDeclaredIn(String)
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredIn(String ownerTypeName) {
        return be(declaredIn(ownerTypeName));
    }

    /**
     * @return negation of {@link #beDeclaredIn(String)}
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> notBeDeclaredIn(String ownerTypeName) {
        return not(beDeclaredIn(ownerTypeName));
    }

    /**
     * @return like {@link #beDeclaredIn(Class)} but matching classes are defined by a predicate
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        DescribedPredicate<JavaMember> declaredIn = declaredIn(
                predicate.as("classes that " + predicate.getDescription()));
        return be(declaredIn);
    }

    /**
     * @return A condition matching fields that have a specific raw type
     * @see #haveRawType(String)
     * @see #haveRawType(DescribedPredicate)
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(Class<?> type) {
        return have(rawType(type));
    }

    /**
     * @return like {@link #haveRawType(Class)} but the class is specified by the fully qualified class name
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(String typeName) {
        return have(rawType(typeName));
    }

    /**
     * @return like {@link #haveRawType(Class)} but matching classes are defined by a predicate
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return have(rawType(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(Class<?>... parameterTypes) {
        return have(rawParameterTypes(parameterTypes));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(String... parameterTypeNames) {
        return have(rawParameterTypes(parameterTypeNames));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return have(rawParameterTypes(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(Class<?> type) {
        return have(rawReturnType(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(String typeName) {
        return have(rawReturnType(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return have(rawReturnType(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> declareThrowableOfType(Class<? extends Throwable> type) {
        return declareThrowableOfType(equivalentTo(type).as(type.getName()));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> declareThrowableOfType(String typeName) {
        return declareThrowableOfType(name(typeName).as(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> declareThrowableOfType(DescribedPredicate<? super JavaClass> predicate) {
        DescribedPredicate<HasThrowsClause<?>> declareThrowableOfType = throwsClauseContainingType(predicate)
                .as("declare throwable of type " + predicate.getDescription());
        return does(declareThrowableOfType);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> onlyBeCalledByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        ChainableFunction<JavaAccess<?>, JavaCodeUnit> origin = JavaAccess.Functions.Get.origin();
        ChainableFunction<HasOwner<JavaClass>, JavaClass> owner = Get.owner();
        return new CodeUnitOnlyCallsCondition<>("only be called by classes that " + predicate.getDescription(),
                origin.then(owner).is(predicate), GET_CALLS_OF_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> onlyBeCalledByCodeUnitsThat(DescribedPredicate<? super JavaCodeUnit> predicate) {
        ChainableFunction<JavaAccess<?>, ? extends JavaCodeUnit> origin = JavaAccess.Functions.Get.origin();
        return new CodeUnitOnlyCallsCondition<>("only be called by code units that " + predicate.getDescription(),
                origin.is(predicate), GET_CALLS_OF_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> onlyBeCalledByMethodsThat(DescribedPredicate<? super JavaMethod> predicate) {
        ChainableFunction<JavaAccess<?>, ? extends JavaCodeUnit> origin = JavaAccess.Functions.Get.origin();
        return new CodeUnitOnlyCallsCondition<>("only be called by methods that " + predicate.getDescription(),
                origin.is(matching(JavaMethod.class, predicate)), GET_CALLS_OF_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> onlyBeCalledByConstructorsThat(DescribedPredicate<? super JavaConstructor> predicate) {
        ChainableFunction<JavaAccess<?>, ? extends JavaCodeUnit> origin = JavaAccess.Functions.Get.origin();
        return new CodeUnitOnlyCallsCondition<>("only be called by constructors that " + predicate.getDescription(),
                origin.is(matching(JavaConstructor.class, predicate)), GET_CALLS_OF_SELF);
    }

    /**
     * Derives an {@link ArchCondition} from a {@link DescribedPredicate}. Similar to {@link ArchCondition#from(DescribedPredicate)},
     * but more conveniently creates a message to be used within a 'have'-sentence.
     * <br>
     * Take e.g. {@code have(simpleName("Demo"))}, then the condition description would be {@code "have simple name 'Demo'"},
     * each satisfied event would be described as {@code "Class <some.Example> has simple name 'Demo' in (Example.java:0)"}
     * and each violated one as {@code "Class <some.Example> does not have simple name 'Demo' in (Example.java:0)"}.
     *
     * @param predicate The predicate determining which objects satisfy/violate the condition
     * @return A {@link ConditionByPredicate ConditionByPredicate} derived from the predicate and with 'have'-descriptions
     * @param <T> The type of object the condition will test, e.g. a {@link JavaClass}
     */
    @PublicAPI(usage = ACCESS)
    public static <T extends HasDescription & HasSourceCodeLocation> ConditionByPredicate<T> have(DescribedPredicate<? super T> predicate) {
        return ArchCondition.from(predicate.<T>forSubtype())
                .as(ArchPredicates.have(predicate).getDescription())
                .describeEventsBy((predicateDescription, satisfied) -> (satisfied ? "has " : "does not have ") + predicateDescription);
    }

    /**
     * Derives an {@link ArchCondition} from a {@link DescribedPredicate}. Similar to {@link ArchCondition#from(DescribedPredicate)},
     * but more conveniently creates a message to be used within a 'be'-sentence.
     * <br>
     * Take e.g. {@code be(assignableTo(Demo.class))}, then the condition description would be {@code "be assignable to com.example.Demo"},
     * each satisfied event would be described as {@code "Class <some.Example> is assignable to com.example.Demo in (Example.java:0)"}
     * and each violated one as {@code "Class <some.Example> is not assignable to com.example.Demo in (Example.java:0)"}.
     *
     * @param predicate The predicate determining which objects satisfy/violate the condition
     * @return A {@link ConditionByPredicate ConditionByPredicate} derived from the predicate and with 'be'-descriptions
     * @param <T> The type of object the condition will test, e.g. a {@link JavaClass}
     */
    @PublicAPI(usage = ACCESS)
    public static <T extends HasDescription & HasSourceCodeLocation> ConditionByPredicate<T> be(DescribedPredicate<? super T> predicate) {
        return ArchCondition.from(predicate.<T>forSubtype())
                .as(ArchPredicates.be(predicate).getDescription())
                .describeEventsBy((predicateDescription, satisfied) -> (satisfied ? "is " : "is not ") + predicateDescription);
    }

    @SuppressWarnings("unchecked") // cast compatibility is explicitly checked
    private static <U, T> DescribedPredicate<T> matching(Class<U> type, DescribedPredicate<? super U> predicate) {
        return DescribedPredicate.describe(
                String.format("matching %s %s", type.getSimpleName(), predicate.getDescription()),
                input -> type.isInstance(input) && predicate.test((U) input));
    }

    private static final ArchCondition<JavaClass> BE_TOP_LEVEL_CLASSES =
            be(TOP_LEVEL_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is a" : "is no") + " top level class");
    private static final ArchCondition<JavaClass> BE_NESTED_CLASSES =
            be(NESTED_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is a" : "is no") + " nested class");
    private static final ArchCondition<JavaClass> BE_MEMBER_CLASSES =
            be(MEMBER_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is a" : "is no") + " member class");
    private static final ArchCondition<JavaClass> BE_INNER_CLASSES =
            be(INNER_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is an" : "is no") + " inner class");
    private static final ArchCondition<JavaClass> BE_ANONYMOUS_CLASSES =
            be(ANONYMOUS_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is an" : "is no") + " anonymous class");
    private static final ArchCondition<JavaClass> BE_LOCAL_CLASSES =
            be(LOCAL_CLASSES).describeEventsBy((__, satisfied) -> (satisfied ? "is a" : "is no") + " local class");

    private static class HaveOnlyModifiersCondition<T extends HasModifiers & HasDescription & HasSourceCodeLocation>
            extends AllAttributesMatchCondition<T, JavaClass> {

        private final Function<JavaClass, ? extends Collection<T>> getHasModifiers;

        HaveOnlyModifiersCondition(String description, final JavaModifier modifier, Function<JavaClass, ? extends Collection<T>> getHasModifiers) {
            super("have only " + description, be(modifier(modifier).as(modifier.toString().toLowerCase())));
            this.getHasModifiers = getHasModifiers;
        }

        @Override
        Collection<T> relevantAttributes(JavaClass javaClass) {
            return getHasModifiers.apply(javaClass);
        }
    }

    private static class NumberOfElementsCondition<T extends HasName.AndFullName> extends ArchCondition<T> {
        private final DescribedPredicate<Integer> predicate;
        private final SortedSet<String> allElementNames = new TreeSet<>();

        NumberOfElementsCondition(DescribedPredicate<? super Integer> predicate) {
            super("contain number of elements " + predicate.getDescription());
            this.predicate = predicate.forSubtype();
        }

        @Override
        public void check(T item, ConditionEvents events) {
            allElementNames.add(item.getFullName());
        }

        @Override
        public void finish(ConditionEvents events) {
            int size = allElementNames.size();
            boolean conditionSatisfied = predicate.test(size);
            String message = String.format("there is/are %d element(s) in %s", size, join(allElementNames));
            events.add(new SimpleConditionEvent(size, conditionSatisfied, message));
        }

        private String join(SortedSet<String> strings) {
            return "[" + Joiner.on(", ").join(strings) + "]";
        }
    }

    private static <T extends HasDescription & HasSourceCodeLocation> ArchCondition<T> does(DescribedPredicate<? super T> predicate) {
        return ArchCondition.from(predicate.<T>forSubtype())
                .describeEventsBy((predicateDescription, satisfied) -> (satisfied ? "does " : "does not ") + predicateDescription);
    }
}
