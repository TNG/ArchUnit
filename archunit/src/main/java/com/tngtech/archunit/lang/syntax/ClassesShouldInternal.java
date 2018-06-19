/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

class ClassesShouldInternal extends ObjectsShouldInternal<JavaClass>
        implements ClassesShould, ClassesShouldConjunction {

    ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
            Priority priority,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, prepareCondition);
    }

    ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
            Priority priority,
            ArchCondition<JavaClass> condition,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, condition, prepareCondition);
    }

    private ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
            Priority priority,
            ConditionAggregator<JavaClass> conditionAggregator,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    public ClassesShouldConjunction be(final Class<?> clazz) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.be(clazz)));
    }

    @Override
    public ClassesShouldConjunction notBe(final Class<?> clazz) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBe(clazz)));
    }

    @Override
    public ClassesShouldConjunction be(String className) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.be(className)));
    }

    @Override
    public ClassesShouldConjunction notBe(String className) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBe(className)));
    }

    @Override
    public ClassesShouldConjunction haveFullyQualifiedName(final String name) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveFullyQualifiedName(name)));
    }

    @Override
    public ClassesShouldConjunction notHaveFullyQualifiedName(String name) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notHaveFullyQualifiedName(name)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleName(String name) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleName(name)));
    }

    @Override
    public ClassesShouldConjunction notHaveSimpleName(String name) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notHaveSimpleName(name)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameStartingWith(String prefix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameStartingWith(prefix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotStartingWith(String prefix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameNotStartingWith(prefix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameContaining(String infix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameContaining(infix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotContaining(String infix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameNotContaining(infix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameEndingWith(String suffix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameEndingWith(suffix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotEndingWith(String suffix) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveSimpleNameNotEndingWith(suffix)));
    }

    @Override
    public ClassesShouldConjunction haveNameMatching(String regex) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveNameMatching(regex)));
    }

    @Override
    public ClassesShouldConjunction haveNameNotMatching(String regex) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveNameNotMatching(regex)));
    }

    @Override
    public ClassesShouldConjunction resideInAPackage(String packageIdentifier) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.resideInAPackage(packageIdentifier)));
    }

    @Override
    public ClassesShouldConjunction resideInAnyPackage(String... packageIdentifiers) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.resideInAnyPackage(packageIdentifiers)));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackage(String packageIdentifier) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.resideOutsideOfPackage(packageIdentifier)));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.resideOutsideOfPackages(packageIdentifiers)));
    }

    @Override
    public ClassesShouldConjunction bePublic() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.bePublic()));
    }

    @Override
    public ClassesShouldConjunction notBePublic() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBePublic()));
    }

    @Override
    public ClassesShouldConjunction beProtected() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beProtected()));
    }

    @Override
    public ClassesShouldConjunction notBeProtected() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeProtected()));
    }

    @Override
    public ClassesShouldConjunction bePackagePrivate() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.bePackagePrivate()));
    }

    @Override
    public ClassesShouldConjunction notBePackagePrivate() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBePackagePrivate()));
    }

    @Override
    public ClassesShouldConjunction bePrivate() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.bePrivate()));
    }

    @Override
    public ClassesShouldConjunction notBePrivate() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBePrivate()));
    }

    @Override
    public ClassesShouldConjunction haveModifier(JavaModifier modifier) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.haveModifier(modifier)));
    }

    @Override
    public ClassesShouldConjunction notHaveModifier(JavaModifier modifier) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notHaveModifier(modifier)));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(Class<? extends Annotation> annotationType) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAnnotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(Class<? extends Annotation> annotationType) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAnnotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(String annotationTypeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAnnotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(String annotationTypeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAnnotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAnnotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAnnotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beMetaAnnotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeMetaAnnotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(String annotationTypeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beMetaAnnotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(String annotationTypeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeMetaAnnotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beMetaAnnotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeMetaAnnotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction implement(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.implement(type)));
    }

    @Override
    public ClassesShouldConjunction notImplement(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notImplement(type)));
    }

    @Override
    public ClassesShouldConjunction implement(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.implement(typeName)));
    }

    @Override
    public ClassesShouldConjunction notImplement(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notImplement(typeName)));
    }

    @Override
    public ClassesShouldConjunction implement(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.implement(predicate)));
    }

    @Override
    public ClassesShouldConjunction notImplement(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notImplement(predicate)));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableTo(type)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableTo(type)));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableTo(typeName)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableTo(typeName)));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableTo(predicate)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableTo(predicate)));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableFrom(type)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(Class<?> type) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableFrom(type)));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableFrom(typeName)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(String typeName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableFrom(typeName)));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beAssignableFrom(predicate)));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeAssignableFrom(predicate)));
    }

    @Override
    public ClassesShouldConjunction accessField(Class<?> owner, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.accessField(owner, fieldName)));
    }

    @Override
    public ClassesShouldConjunction getField(Class<?> owner, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.getField(owner, fieldName)));
    }

    @Override
    public ClassesShouldConjunction setField(Class<?> owner, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.setField(owner, fieldName)));
    }

    @Override
    public ClassesShouldConjunction accessField(String ownerName, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.accessField(ownerName, fieldName)));
    }

    @Override
    public ClassesShouldConjunction getField(String ownerName, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.getField(ownerName, fieldName)));
    }

    @Override
    public ClassesShouldConjunction setField(String ownerName, String fieldName) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.setField(ownerName, fieldName)));
    }

    @Override
    public ClassesShouldConjunction accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.accessFieldWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.getFieldWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.setFieldWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction callMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callMethod(owner, methodName, parameterTypes)));
    }

    @Override
    public ClassesShouldConjunction callMethod(String ownerName, String methodName, String... parameterTypeNames) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callMethod(ownerName, methodName, parameterTypeNames)));
    }

    @Override
    public ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callMethodWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction callConstructor(Class<?> owner, Class<?>[] parameterTypes) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callConstructor(owner, parameterTypes)));
    }

    @Override
    public ClassesShouldConjunction callConstructor(String ownerName, String... parameterTypeNames) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callConstructor(ownerName, parameterTypeNames)));
    }

    @Override
    public ClassesShouldConjunction callConstructorWhere(DescribedPredicate<? super JavaConstructorCall> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callConstructorWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction accessTargetWhere(DescribedPredicate<? super JavaAccess<?>> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.accessTargetWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callCodeUnitWhere(predicate)));
    }

    @Override
    public ClassesShouldThat accessClassesThat() {
        return new ClassesShouldThatInternal(this, new Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>>() {
            @Override
            public ArchCondition<JavaClass> apply(DescribedPredicate<JavaClass> input) {
                return ArchConditions.accessClassesThat(input);
            }
        });
    }

    @Override
    public ClassesShouldConjunction accessClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.accessClassesThat(predicate)));
    }

    @Override
    public OnlyBeAccessedSpecification<ClassesShouldConjunction> onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
    }

    @Override
    public ClassesShouldConjunction beInterfaces() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beInterfaces()));
    }

    @Override
    public ClassesShouldConjunction notBeInterfaces() {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.notBeInterfaces()));
    }

    ClassesShouldInternal copyWithNewCondition(ArchCondition<JavaClass> newCondition) {
        return new ClassesShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }

    ClassesShouldInternal addCondition(ArchCondition<JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator.add(condition));
    }

    @Override
    public ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatANDsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should"))
                .add(condition));
    }

    @Override
    public ClassesShould andShould() {
        return new ClassesShouldInternal(
                classesTransformer,
                priority,
                conditionAggregator.thatANDsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should")),
                prepareCondition);
    }

    @Override
    public ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatORsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should"))
                .add(condition));
    }

    @Override
    public ClassesShould orShould() {
        return new ClassesShouldInternal(
                classesTransformer,
                priority,
                conditionAggregator.thatORsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should")),
                prepareCondition);
    }
}
