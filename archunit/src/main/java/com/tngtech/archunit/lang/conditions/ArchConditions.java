/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import com.google.common.base.Joiner;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.base.PackageMatchers;
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
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static com.tngtech.archunit.core.domain.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_ACCESSES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_ACCESSES_TO_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTORS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTOR_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_DIRECT_DEPENDENCIES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_DIRECT_DEPENDENCIES_TO_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELDS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELD_ACCESSES_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_METHOD_CALLS_FROM_SELF;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_PACKAGE_NAME;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
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
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.archunit.core.domain.properties.HasType.Predicates.rawType;
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
    public static ArchCondition<JavaClass> onlyAccessFieldsThat(final DescribedPredicate<? super JavaField> predicate) {
        ChainableFunction<JavaFieldAccess, FieldAccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaFieldAccess> accessPredicate = getTarget.then(FieldAccessTarget.Functions.RESOLVE)
                .is(anyElementThat(predicate.<JavaField>forSubType()));
        return new ClassOnlyAccessesCondition<>(accessPredicate, GET_FIELD_ACCESSES_FROM_SELF)
                .as("only access fields that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        return callMethodWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(methodName)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypes))))
                .as("call method %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), methodName, namesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callMethod(String ownerName, String methodName, String... parameterTypeNames) {
        return callMethodWhere(JavaCall.Predicates.target(With.<JavaClass>owner(name(ownerName)))
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
        DescribedPredicate<JavaMethodCall> callPredicate = getTarget.then(MethodCallTarget.Functions.RESOLVE)
                .is(anyElementThat(predicate.<JavaMethod>forSubType()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_METHOD_CALLS_FROM_SELF)
                .as("only call methods that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(Class<?> owner, Class<?>... parameterTypes) {
        return callConstructorWhere(JavaCall.Predicates.target(owner(type(owner)))
                .and(JavaCall.Predicates.target(name(CONSTRUCTOR_NAME)))
                .and(JavaCall.Predicates.target(rawParameterTypes(parameterTypes))))
                .as("call constructor %s", Formatters.formatMethodSimple(
                        owner.getSimpleName(), CONSTRUCTOR_NAME, namesOf(parameterTypes)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callConstructor(String ownerName, String... parameterTypeNames) {
        return callConstructorWhere(JavaCall.Predicates.target(With.<JavaClass>owner(name(ownerName)))
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
        DescribedPredicate<JavaConstructorCall> callPredicate = getTarget.then(ConstructorCallTarget.Functions.RESOLVE)
                .is(anyElementThat(predicate.<JavaConstructor>forSubType()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_CONSTRUCTOR_CALLS_FROM_SELF)
                .as("only call constructors that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return new ClassAccessesCondition<>(predicate, GET_CALLS_FROM_SELF)
                .as("call code unit where " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyCallCodeUnitsThat(final DescribedPredicate<? super JavaCodeUnit> predicate) {
        ChainableFunction<JavaCall<?>, CodeUnitCallTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaCall<?>> callPredicate = getTarget.then(CodeUnitCallTarget.Functions.RESOLVE)
                .is(anyElementThat(predicate.<JavaCodeUnit>forSubType()));
        return new ClassOnlyAccessesCondition<>(callPredicate, GET_CALLS_FROM_SELF)
                .as("only call code units that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyAccessMembersThat(final DescribedPredicate<? super JavaMember> predicate) {
        ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(AccessTarget.Functions.RESOLVE)
                .is(anyElementThat(predicate.<JavaMember>forSubType()));
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
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(Get.<JavaClass>owner()).is(predicate);
        return new ClassAccessesCondition<>(accessPredicate, GET_ACCESSES_FROM_SELF)
                .as("access classes that " + predicate.getDescription());
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyAccessClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
        DescribedPredicate<JavaAccess<?>> accessPredicate = getTarget.then(Get.<JavaClass>owner()).is(predicate);
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
    public static ArchCondition<JavaClass> onlyDependOnClassesThat(final DescribedPredicate<? super JavaClass> predicate) {
        return new AllDependenciesCondition(
                "only depend on classes that " + predicate.getDescription(),
                GET_TARGET_CLASS.is(predicate),
                GET_DIRECT_DEPENDENCIES_FROM_SELF);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> onlyBeAccessedByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return new AllAccessesCondition("only be accessed by classes that",
                JavaAccess.Functions.Get.origin().then(Get.<JavaClass>owner()).is(predicate), GET_ACCESSES_TO_SELF);
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
        String description = String.format("only have dependents in any package ['%s']",
                Joiner.on("', '").join(packageIdentifiers));
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
        String description = String.format("only have dependencies in any package ['%s']",
                Joiner.on("', '").join(packageIdentifiers));
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
        return JavaFieldAccess.Predicates.target(With.<JavaClass>owner(name(ownerName)))
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
        return new BeClassCondition(className);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBe(final String className) {
        return not(be(className));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveName(final String name) {
        return new HaveConditionByPredicate<>(name(name));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> notHaveName(String name) {
        return not(ArchConditions.<HAS_NAME>haveName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> haveFullName(String fullName) {
        return new HaveConditionByPredicate<>(fullName(fullName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> notHaveFullName(String fullName) {
        return not(new HaveConditionByPredicate<HAS_FULL_NAME>(fullName(fullName)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveFullyQualifiedName(final String name) {
        return new HaveConditionByPredicate<>(fullyQualifiedName(name));
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
        final DescribedPredicate<JavaClass> haveSimpleName = have(simpleName(name));
        return new SimpleNameCondition(haveSimpleName, name);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notHaveSimpleName(String name) {
        return not(haveSimpleName(name));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameStartingWith(final String prefix) {
        final DescribedPredicate<JavaClass> predicate = have(simpleNameStartingWith(prefix));

        return new SimpleNameStartingWithCondition(predicate, prefix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotStartingWith(String prefix) {
        return not(haveSimpleNameStartingWith(prefix)).as("have simple name not starting with '%s'", prefix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameContaining(final String infix) {
        final DescribedPredicate<JavaClass> predicate = have(simpleNameContaining(infix));

        return new SimpleNameContainingCondition(predicate, infix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotContaining(final String infix) {
        return not(haveSimpleNameContaining(infix)).as("have simple name not containing '%s'", infix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameEndingWith(final String suffix) {
        final DescribedPredicate<JavaClass> predicate = have(simpleNameEndingWith(suffix));

        return new SimpleNameEndingWithCondition(predicate, suffix);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> haveSimpleNameNotEndingWith(String suffix) {
        return not(haveSimpleNameEndingWith(suffix)).as("have simple name not ending with '%s'", suffix);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameMatching(final String regex) {
        final DescribedPredicate<HAS_NAME> haveNameMatching = have(nameMatching(regex)).forSubType();
        return new MatchingCondition<>(haveNameMatching, regex);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_NAME extends HasName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_NAME> haveNameNotMatching(String regex) {
        return not(ArchConditions.<HAS_NAME>haveNameMatching(regex)).as("have name not matching '%s'", regex);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation>
    ArchCondition<HAS_FULL_NAME> haveFullNameMatching(String regex) {
        final DescribedPredicate<HAS_FULL_NAME> haveFullNameMatching = have(fullNameMatching(regex)).forSubType();
        return new MatchingCondition<>(haveFullNameMatching, regex);
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_FULL_NAME extends HasName.AndFullName & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_FULL_NAME>
    haveFullNameNotMatching(String regex) {
        return not(ArchConditions.<HAS_FULL_NAME>haveFullNameMatching(regex)).as("have full name not matching '%s'", regex);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAPackage(final String packageIdentifier) {
        return new DoesConditionByPredicate<>(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideInAnyPackage(String... packageIdentifiers) {
        return new DoesConditionByPredicate<>(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackage(String packageIdentifier) {
        return new DoesConditionByPredicate<>(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> resideOutsideOfPackages(String... packageIdentifiers) {
        return new DoesConditionByPredicate<>(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> haveModifier(
            final JavaModifier modifier) {
        return new HaveConditionByPredicate<>(modifier(modifier));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_MODIFIERS extends HasModifiers & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_MODIFIERS> notHaveModifier(
            final JavaModifier modifier) {
        return not(ArchConditions.<HAS_MODIFIERS>haveModifier(modifier));
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

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            Class<? extends Annotation> type) {
        return new IsConditionByPredicate<>(annotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            Class<? extends Annotation> type) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beAnnotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            String typeName) {
        return new IsConditionByPredicate<>(annotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            String typeName) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beAnnotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beAnnotatedWith(
            final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return new IsConditionByPredicate<>(annotatedWith(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeAnnotatedWith(
            DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beAnnotatedWith(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            Class<? extends Annotation> type) {
        return new IsConditionByPredicate<>(metaAnnotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            Class<? extends Annotation> type) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beMetaAnnotatedWith(type));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            String typeName) {
        return new IsConditionByPredicate<>(metaAnnotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            String typeName) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beMetaAnnotatedWith(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> beMetaAnnotatedWith(
            final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return new IsConditionByPredicate<>(metaAnnotatedWith(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static <HAS_ANNOTATIONS extends HasAnnotations<?> & HasDescription & HasSourceCodeLocation> ArchCondition<HAS_ANNOTATIONS> notBeMetaAnnotatedWith(
            DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return not(ArchConditions.<HAS_ANNOTATIONS>beMetaAnnotatedWith(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(Class<?> interfaceType) {
        return new ImplementsCondition(JavaClass.Predicates.implement(interfaceType));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(Class<?> interfaceType) {
        return not(implement(interfaceType));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(String interfaceTypeName) {
        return new ImplementsCondition(JavaClass.Predicates.implement(interfaceTypeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(String interfaceTypeName) {
        return not(implement(interfaceTypeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> implement(DescribedPredicate<? super JavaClass> predicate) {
        return new ImplementsCondition(JavaClass.Predicates.implement(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notImplement(DescribedPredicate<? super JavaClass> predicate) {
        return not(implement(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(Class<?> type) {
        return new IsConditionByPredicate<>(assignableTo(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(Class<?> type) {
        return not(beAssignableTo(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(String typeName) {
        return new IsConditionByPredicate<>(assignableTo(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(String typeName) {
        return not(beAssignableTo(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return new IsConditionByPredicate<>(assignableTo(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableTo(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(Class<?> type) {
        return new IsConditionByPredicate<>(assignableFrom(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(Class<?> type) {
        return not(beAssignableFrom(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(String typeName) {
        return new IsConditionByPredicate<>(assignableFrom(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(String typeName) {
        return not(beAssignableFrom(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return new IsConditionByPredicate<>(assignableFrom(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return not(beAssignableFrom(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beInterfaces() {
        return InterfacesCondition.BE_INTERFACES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeInterfaces() {
        return not(InterfacesCondition.BE_INTERFACES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beEnums() {
        return EnumsCondition.BE_ENUMS;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeEnums() {
        return not(EnumsCondition.BE_ENUMS);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beTopLevelClasses() {
        return BE_TOP_LEVEL_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeTopLevelClasses() {
        return not(BE_TOP_LEVEL_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beNestedClasses() {
        return BE_NESTED_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeNestedClasses() {
        return not(BE_NESTED_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beMemberClasses() {
        return BE_MEMBER_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeMemberClasses() {
        return not(BE_MEMBER_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beInnerClasses() {
        return BE_INNER_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeInnerClasses() {
        return not(BE_INNER_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beAnonymousClasses() {
        return BE_ANONYMOUS_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeAnonymousClasses() {
        return not(BE_ANONYMOUS_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> beLocalClasses() {
        return BE_LOCAL_CLASSES;
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> notBeLocalClasses() {
        return not(BE_LOCAL_CLASSES);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> containNumberOfElements(DescribedPredicate<? super Integer> predicate) {
        return new NumberOfElementsCondition(predicate);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredIn(Class<?> owner) {
        return new IsConditionByPredicate<>(declaredIn(owner));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> notBeDeclaredIn(Class<?> owner) {
        return not(beDeclaredIn(owner));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredIn(String ownerTypeName) {
        return new IsConditionByPredicate<>(declaredIn(ownerTypeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> notBeDeclaredIn(String ownerTypeName) {
        return not(beDeclaredIn(ownerTypeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaMember> beDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        DescribedPredicate<JavaMember> declaredIn = declaredIn(
                predicate.as("classes that " + predicate.getDescription()));
        return new IsConditionByPredicate<>(declaredIn);
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(Class<?> type) {
        return new HaveConditionByPredicate<>(rawType(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(String typeName) {
        return new HaveConditionByPredicate<>(rawType(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaField> haveRawType(DescribedPredicate<? super JavaClass> predicate) {
        return new HaveConditionByPredicate<>(rawType(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(Class<?>... parameterTypes) {
        return new HaveConditionByPredicate<>(rawParameterTypes(parameterTypes));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(String... parameterTypeNames) {
        return new HaveConditionByPredicate<>(rawParameterTypes(parameterTypeNames));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate) {
        return new HaveConditionByPredicate<>(rawParameterTypes(predicate));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(Class<?> type) {
        return new HaveConditionByPredicate<>(rawReturnType(type));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(String typeName) {
        return new HaveConditionByPredicate<>(rawReturnType(typeName));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaCodeUnit> haveRawReturnType(DescribedPredicate<? super JavaClass> predicate) {
        return new HaveConditionByPredicate<>(rawReturnType(predicate));
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
        return new DoesConditionByPredicate<>(declareThrowableOfType);
    }

    private static <T extends HasDescription & HasSourceCodeLocation> String createMessage(T object, String message) {
        return object.getDescription() + " " + message + " in " + object.getSourceCodeLocation();
    }

    private static final IsConditionByPredicate<JavaClass> BE_TOP_LEVEL_CLASSES =
            new IsConditionByPredicate<>("a top level class", JavaClass.Predicates.TOP_LEVEL_CLASSES);
    private static final IsConditionByPredicate<JavaClass> BE_NESTED_CLASSES =
            new IsConditionByPredicate<>("a nested class", JavaClass.Predicates.NESTED_CLASSES);
    private static final IsConditionByPredicate<JavaClass> BE_MEMBER_CLASSES =
            new IsConditionByPredicate<>("a member class", JavaClass.Predicates.MEMBER_CLASSES);
    private static final IsConditionByPredicate<JavaClass> BE_INNER_CLASSES =
            new IsConditionByPredicate<>("an inner class", JavaClass.Predicates.INNER_CLASSES);
    private static final IsConditionByPredicate<JavaClass> BE_ANONYMOUS_CLASSES =
            new IsConditionByPredicate<>("an anonymous class", JavaClass.Predicates.ANONYMOUS_CLASSES);
    private static final IsConditionByPredicate<JavaClass> BE_LOCAL_CLASSES =
            new IsConditionByPredicate<>("a local class", JavaClass.Predicates.LOCAL_CLASSES);

    private static class HaveOnlyModifiersCondition<T extends HasModifiers & HasDescription & HasSourceCodeLocation>
            extends AllAttributesMatchCondition<T> {

        private final Function<JavaClass, ? extends Collection<T>> getHasModifiers;

        HaveOnlyModifiersCondition(String description, final JavaModifier modifier, Function<JavaClass, ? extends Collection<T>> getHasModifiers) {
            super("have only " + description, new ModifierCondition<T>(modifier));
            this.getHasModifiers = getHasModifiers;
        }

        @Override
        Collection<T> relevantAttributes(JavaClass javaClass) {
            return getHasModifiers.apply(javaClass);
        }
    }

    private static class ModifierCondition<T extends HasModifiers & HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
        private final JavaModifier modifier;

        ModifierCondition(JavaModifier modifier) {
            super("modifier " + modifier);
            this.modifier = modifier;
        }

        @Override
        public void check(T hasModifiers, ConditionEvents events) {
            boolean satisfied = hasModifiers.getModifiers().contains(modifier);
            String infix = (satisfied ? "is " : "is not ") + modifier.toString().toLowerCase();
            events.add(new SimpleConditionEvent(hasModifiers, satisfied, createMessage(hasModifiers, infix)));
        }
    }

    private static class ImplementsCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<? super JavaClass> implement;

        ImplementsCondition(DescribedPredicate<? super JavaClass> implement) {
            super(implement.getDescription());
            this.implement = implement;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean satisfied = implement.apply(javaClass);
            String description = satisfied
                    ? implement.getDescription().replace("implement", "implements")
                    : implement.getDescription().replace("implement", "does not implement");
            String message = createMessage(javaClass, description);
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    }

    private static class InterfacesCondition extends ArchCondition<JavaClass> {
        private static final InterfacesCondition BE_INTERFACES = new InterfacesCondition();

        InterfacesCondition() {
            super("be interfaces");
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean isInterface = javaClass.isInterface();
            String message = createMessage(javaClass,
                    (isInterface ? "is an" : "is not an") + " interface");
            events.add(new SimpleConditionEvent(javaClass, isInterface, message));
        }
    }

    private static class EnumsCondition extends ArchCondition<JavaClass> {
        private static final EnumsCondition BE_ENUMS = new EnumsCondition();

        EnumsCondition() {
            super("be enums");
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean isEnum = javaClass.isEnum();
            String message = createMessage(javaClass,
                    (isEnum ? "is an" : "is not an") + " enum");
            events.add(new SimpleConditionEvent(javaClass, isEnum, message));
        }
    }

    private static class NumberOfElementsCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<Integer> predicate;
        private SortedSet<String> allClassNames;

        NumberOfElementsCondition(DescribedPredicate<? super Integer> predicate) {
            super("contain number of elements " + predicate.getDescription());
            this.predicate = predicate.forSubType();
            allClassNames = new TreeSet<>();
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            allClassNames.add(item.getName());
        }

        @Override
        public void finish(ConditionEvents events) {
            int size = allClassNames.size();
            boolean conditionSatisfied = predicate.apply(size);
            String message = String.format("there is/are %d element(s) in classes %s", size, join(allClassNames));
            events.add(new SimpleConditionEvent(size, conditionSatisfied, message));
        }

        private String join(SortedSet<String> strings) {
            return "[" + Joiner.on(", ").join(strings) + "]";
        }
    }

    private static class BeClassCondition extends ArchCondition<JavaClass> {
        private final String className;

        BeClassCondition(String className) {
            super("be " + className);
            this.className = className;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean itemEquivalentToClazz = javaClass.getName().equals(className);
            String message = createMessage(javaClass,
                    (itemEquivalentToClazz ? "is " : "is not ") + className);
            events.add(new SimpleConditionEvent(javaClass, itemEquivalentToClazz, message));
        }
    }

    private static class SimpleNameCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<JavaClass> haveSimpleName;
        private final String name;

        SimpleNameCondition(DescribedPredicate<JavaClass> haveSimpleName, String name) {
            super(haveSimpleName.getDescription());
            this.haveSimpleName = haveSimpleName;
            this.name = name;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean satisfied = haveSimpleName.apply(javaClass);
            String message = createMessage(javaClass,
                    String.format("%s simple name '%s'", satisfied ? "has" : "does not have", name));
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    }

    private static class SimpleNameStartingWithCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<JavaClass> predicate;
        private final String prefix;

        SimpleNameStartingWithCondition(DescribedPredicate<JavaClass> predicate, String prefix) {
            super(predicate.getDescription());
            this.predicate = predicate;
            this.prefix = prefix;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean satisfied = predicate.apply(javaClass);
            String message = String.format("simple name of %s %s with '%s' in %s",
                    javaClass.getName(),
                    satisfied ? "starts" : "does not start",
                    prefix,
                    javaClass.getSourceCodeLocation());
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    }

    private static class SimpleNameContainingCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<JavaClass> predicate;
        private final String infix;

        SimpleNameContainingCondition(DescribedPredicate<JavaClass> predicate, String infix) {
            super(predicate.getDescription());
            this.predicate = predicate;
            this.infix = infix;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean satisfied = predicate.apply(javaClass);
            String message = String.format("simple name of %s %s '%s' in %s",
                    javaClass.getName(),
                    satisfied ? "contains" : "does not contain",
                    infix,
                    javaClass.getSourceCodeLocation());
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    }

    private static class SimpleNameEndingWithCondition extends ArchCondition<JavaClass> {
        private final DescribedPredicate<JavaClass> predicate;
        private final String suffix;

        SimpleNameEndingWithCondition(DescribedPredicate<JavaClass> predicate, String suffix) {
            super(predicate.getDescription());
            this.predicate = predicate;
            this.suffix = suffix;
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean satisfied = predicate.apply(javaClass);
            String message = String.format("simple name of %s %s with '%s' in %s",
                    javaClass.getName(),
                    satisfied ? "ends" : "does not end",
                    suffix,
                    javaClass.getSourceCodeLocation());
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    }

    private static class MatchingCondition<T extends HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
        private final DescribedPredicate<T> matcher;
        private final String regex;

        MatchingCondition(DescribedPredicate<T> matcher, String regex) {
            super(matcher.getDescription());
            this.matcher = matcher;
            this.regex = regex;
        }

        @Override
        public void check(T item, ConditionEvents events) {
            boolean satisfied = matcher.apply(item);
            String message = createMessage(item,
                    String.format("%s '%s'", satisfied ? "matches" : "does not match", regex));
            events.add(new SimpleConditionEvent(item, satisfied, message));
        }
    }

    private static class DoesConditionByPredicate<T extends HasDescription & HasSourceCodeLocation>
            extends ArchCondition<T> {
        private final DescribedPredicate<? super T> predicate;

        DoesConditionByPredicate(DescribedPredicate<? super T> predicate) {
            super(predicate.getDescription());
            this.predicate = predicate;
        }

        @Override
        public void check(T item, ConditionEvents events) {
            boolean satisfied = predicate.apply(item);
            String message = createMessage(item,
                    (satisfied ? "does " : "does not ") + predicate.getDescription());
            events.add(new SimpleConditionEvent(item, satisfied, message));
        }
    }

    private static class IsConditionByPredicate<T extends HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
        private final String eventDescription;
        private final DescribedPredicate<T> predicate;

        IsConditionByPredicate(DescribedPredicate<? super T> predicate) {
            this(predicate.getDescription(), predicate);
        }

        IsConditionByPredicate(String eventDescription, DescribedPredicate<? super T> predicate) {
            super(ArchPredicates.be(predicate).getDescription());
            this.eventDescription = eventDescription;
            this.predicate = predicate.forSubType();
        }

        @Override
        public void check(T member, ConditionEvents events) {
            boolean satisfied = predicate.apply(member);
            String message = createMessage(member,
                    (satisfied ? "is " : "is not ") + eventDescription);
            events.add(new SimpleConditionEvent(member, satisfied, message));
        }
    }

    private static class HaveConditionByPredicate<T extends HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
        private final DescribedPredicate<T> rawType;

        HaveConditionByPredicate(DescribedPredicate<? super T> rawType) {
            super(ArchPredicates.have(rawType).getDescription());
            this.rawType = rawType.forSubType();
        }

        @Override
        public void check(T object, ConditionEvents events) {
            boolean satisfied = rawType.apply(object);
            String message = createMessage(object, (satisfied ? "has " : "does not have ") + rawType.getDescription());
            events.add(new SimpleConditionEvent(object, satisfied, message));
        }
    }
}
