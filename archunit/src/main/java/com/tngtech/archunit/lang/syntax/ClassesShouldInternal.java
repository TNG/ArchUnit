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
package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
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
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
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
        return addCondition(ArchConditions.be(clazz));
    }

    @Override
    public ClassesShouldConjunction notBe(final Class<?> clazz) {
        return addCondition(ArchConditions.notBe(clazz));
    }

    @Override
    public ClassesShouldConjunction be(String className) {
        return addCondition(ArchConditions.be(className));
    }

    @Override
    public ClassesShouldConjunction notBe(String className) {
        return addCondition(ArchConditions.notBe(className));
    }

    @Override
    public ClassesShouldConjunction haveFullyQualifiedName(final String name) {
        return addCondition(ArchConditions.haveFullyQualifiedName(name));
    }

    @Override
    public ClassesShouldConjunction notHaveFullyQualifiedName(String name) {
        return addCondition(ArchConditions.notHaveFullyQualifiedName(name));
    }

    @Override
    public ClassesShouldConjunction haveSimpleName(String name) {
        return addCondition(ArchConditions.haveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction notHaveSimpleName(String name) {
        return addCondition(ArchConditions.notHaveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameStartingWith(String prefix) {
        return addCondition(ArchConditions.haveSimpleNameStartingWith(prefix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotStartingWith(String prefix) {
        return addCondition(ArchConditions.haveSimpleNameNotStartingWith(prefix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameContaining(String infix) {
        return addCondition(ArchConditions.haveSimpleNameContaining(infix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotContaining(String infix) {
        return addCondition(ArchConditions.haveSimpleNameNotContaining(infix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameEndingWith(String suffix) {
        return addCondition(ArchConditions.haveSimpleNameEndingWith(suffix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotEndingWith(String suffix) {
        return addCondition(ArchConditions.haveSimpleNameNotEndingWith(suffix));
    }

    @Override
    public ClassesShouldConjunction haveNameMatching(String regex) {
        return addCondition(ArchConditions.haveNameMatching(regex));
    }

    @Override
    public ClassesShouldConjunction haveNameNotMatching(String regex) {
        return addCondition(ArchConditions.haveNameNotMatching(regex));
    }

    @Override
    public ClassesShouldConjunction resideInAPackage(String packageIdentifier) {
        return addCondition(ArchConditions.resideInAPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideInAnyPackage(String... packageIdentifiers) {
        return addCondition(ArchConditions.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackage(String packageIdentifier) {
        return addCondition(ArchConditions.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return addCondition(ArchConditions.resideOutsideOfPackages(packageIdentifiers));
    }

    @Override
    public ClassesShouldConjunction bePublic() {
        return addCondition(ArchConditions.bePublic());
    }

    @Override
    public ClassesShouldConjunction notBePublic() {
        return addCondition(ArchConditions.notBePublic());
    }

    @Override
    public ClassesShouldConjunction beProtected() {
        return addCondition(ArchConditions.beProtected());
    }

    @Override
    public ClassesShouldConjunction notBeProtected() {
        return addCondition(ArchConditions.notBeProtected());
    }

    @Override
    public ClassesShouldConjunction bePackagePrivate() {
        return addCondition(ArchConditions.bePackagePrivate());
    }

    @Override
    public ClassesShouldConjunction notBePackagePrivate() {
        return addCondition(ArchConditions.notBePackagePrivate());
    }

    @Override
    public ClassesShouldConjunction bePrivate() {
        return addCondition(ArchConditions.bePrivate());
    }

    @Override
    public ClassesShouldConjunction notBePrivate() {
        return addCondition(ArchConditions.notBePrivate());
    }

    @Override
    public ClassesShouldConjunction haveOnlyFinalFields() {
        return addCondition(ArchConditions.haveOnlyFinalFields());
    }

    @Override
    public ClassesShouldConjunction haveOnlyPrivateConstructors() {
        return addCondition(ArchConditions.haveOnlyPrivateConstructors());
    }

    @Override
    public ClassesShouldConjunction haveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.haveModifier(modifier));
    }

    @Override
    public ClassesShouldConjunction notHaveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.notHaveModifier(modifier));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationType));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationType));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationTypeName));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationTypeName));
    }

    @Override
    public ClassesShouldConjunction beAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.beAnnotatedWith(predicate));
    }

    @Override
    public ClassesShouldConjunction notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.notBeAnnotatedWith(predicate));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationType));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationType));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public ClassesShouldConjunction beMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(predicate));
    }

    @Override
    public ClassesShouldConjunction notBeMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(predicate));
    }

    @Override
    public ClassesShouldConjunction implement(Class<?> type) {
        return addCondition(ArchConditions.implement(type));
    }

    @Override
    public ClassesShouldConjunction notImplement(Class<?> type) {
        return addCondition(ArchConditions.notImplement(type));
    }

    @Override
    public ClassesShouldConjunction implement(String typeName) {
        return addCondition(ArchConditions.implement(typeName));
    }

    @Override
    public ClassesShouldConjunction notImplement(String typeName) {
        return addCondition(ArchConditions.notImplement(typeName));
    }

    @Override
    public ClassesShouldConjunction implement(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.implement(predicate));
    }

    @Override
    public ClassesShouldConjunction notImplement(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.notImplement(predicate));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(Class<?> type) {
        return addCondition(ArchConditions.beAssignableTo(type));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(Class<?> type) {
        return addCondition(ArchConditions.notBeAssignableTo(type));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(String typeName) {
        return addCondition(ArchConditions.beAssignableTo(typeName));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(String typeName) {
        return addCondition(ArchConditions.notBeAssignableTo(typeName));
    }

    @Override
    public ClassesShouldConjunction beAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.beAssignableTo(predicate));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.notBeAssignableTo(predicate));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(Class<?> type) {
        return addCondition(ArchConditions.beAssignableFrom(type));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(Class<?> type) {
        return addCondition(ArchConditions.notBeAssignableFrom(type));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(String typeName) {
        return addCondition(ArchConditions.beAssignableFrom(typeName));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(String typeName) {
        return addCondition(ArchConditions.notBeAssignableFrom(typeName));
    }

    @Override
    public ClassesShouldConjunction beAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.beAssignableFrom(predicate));
    }

    @Override
    public ClassesShouldConjunction notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.notBeAssignableFrom(predicate));
    }

    @Override
    public ClassesShouldConjunction accessField(Class<?> owner, String fieldName) {
        return addCondition(ArchConditions.accessField(owner, fieldName));
    }

    @Override
    public ClassesShouldConjunction getField(Class<?> owner, String fieldName) {
        return addCondition(ArchConditions.getField(owner, fieldName));
    }

    @Override
    public ClassesShouldConjunction setField(Class<?> owner, String fieldName) {
        return addCondition(ArchConditions.setField(owner, fieldName));
    }

    @Override
    public ClassesShouldConjunction accessField(String ownerName, String fieldName) {
        return addCondition(ArchConditions.accessField(ownerName, fieldName));
    }

    @Override
    public ClassesShouldConjunction getField(String ownerName, String fieldName) {
        return addCondition(ArchConditions.getField(ownerName, fieldName));
    }

    @Override
    public ClassesShouldConjunction setField(String ownerName, String fieldName) {
        return addCondition(ArchConditions.setField(ownerName, fieldName));
    }

    @Override
    public ClassesShouldConjunction accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return addCondition(ArchConditions.accessFieldWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction onlyAccessFieldsThat(DescribedPredicate<? super JavaField> predicate) {
        return addCondition(ArchConditions.onlyAccessFieldsThat(predicate));
    }

    @Override
    public ClassesShouldConjunction getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return addCondition(ArchConditions.getFieldWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return addCondition(ArchConditions.setFieldWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction callMethod(Class<?> owner, String methodName, Class<?>[] parameterTypes) {
        return addCondition(ArchConditions.callMethod(owner, methodName, parameterTypes));
    }

    @Override
    public ClassesShouldConjunction callMethod(String ownerName, String methodName, String... parameterTypeNames) {
        return addCondition(ArchConditions.callMethod(ownerName, methodName, parameterTypeNames));
    }

    @Override
    public ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate) {
        return addCondition(ArchConditions.callMethodWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction onlyCallMethodsThat(DescribedPredicate<? super JavaMethod> predicate) {
        return addCondition(ArchConditions.onlyCallMethodsThat(predicate));
    }

    @Override
    public ClassesShouldConjunction callConstructor(Class<?> owner, Class<?>[] parameterTypes) {
        return addCondition(ArchConditions.callConstructor(owner, parameterTypes));
    }

    @Override
    public ClassesShouldConjunction callConstructor(String ownerName, String... parameterTypeNames) {
        return addCondition(ArchConditions.callConstructor(ownerName, parameterTypeNames));
    }

    @Override
    public ClassesShouldConjunction callConstructorWhere(DescribedPredicate<? super JavaConstructorCall> predicate) {
        return addCondition(ArchConditions.callConstructorWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction onlyCallConstructorsThat(DescribedPredicate<? super JavaConstructor> predicate) {
        return addCondition(ArchConditions.onlyCallConstructorsThat(predicate));
    }

    @Override
    public ClassesShouldConjunction accessTargetWhere(DescribedPredicate<? super JavaAccess<?>> predicate) {
        return addCondition(ArchConditions.accessTargetWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction onlyAccessMembersThat(DescribedPredicate<? super JavaMember> predicate) {
        return addCondition(ArchConditions.onlyAccessMembersThat(predicate));
    }

    @Override
    public ClassesShouldConjunction callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return addCondition(ArchConditions.callCodeUnitWhere(predicate));
    }

    @Override
    public ClassesShouldConjunction onlyCallCodeUnitsThat(DescribedPredicate<? super JavaCodeUnit> predicate) {
        return addCondition(ArchConditions.onlyCallCodeUnitsThat(predicate));
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> accessClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.accessClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction accessClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.accessClassesThat(predicate));
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> onlyAccessClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.onlyAccessClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction onlyAccessClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.onlyAccessClassesThat(predicate));
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> dependOnClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.dependOnClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction dependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.dependOnClassesThat(predicate));
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> onlyDependOnClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.onlyDependOnClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction onlyDependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.onlyDependOnClassesThat(predicate));
    }

    @Override
    public OnlyBeAccessedSpecification<ClassesShouldConjunction> onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
    }

    @Override
    public ClassesThat<ClassesShouldConjunction> onlyHaveDependentClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, ClassesShouldConjunction>() {
            @Override
            public ClassesShouldConjunction apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.onlyHaveDependentClassesThat(predicate));
            }
        });
    }

    @Override
    public ClassesShouldConjunction onlyHaveDependentClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.onlyHaveDependentClassesThat(predicate));
    }

    @Override
    public ClassesShouldConjunction beInterfaces() {
        return addCondition(ArchConditions.beInterfaces());
    }

    @Override
    public ClassesShouldConjunction notBeInterfaces() {
        return addCondition(ArchConditions.notBeInterfaces());
    }

    @Override
    public ClassesShouldConjunction beEnums() {
        return addCondition(ArchConditions.beEnums());
    }

    @Override
    public ClassesShouldConjunction notBeEnums() {
        return addCondition(ArchConditions.notBeEnums());
    }

    @Override
    public ClassesShouldConjunction beTopLevelClasses() {
        return addCondition(ArchConditions.beTopLevelClasses());
    }

    @Override
    public ClassesShouldConjunction notBeTopLevelClasses() {
        return addCondition(ArchConditions.notBeTopLevelClasses());
    }

    @Override
    public ClassesShouldConjunction beNestedClasses() {
        return addCondition(ArchConditions.beNestedClasses());
    }

    @Override
    public ClassesShouldConjunction notBeNestedClasses() {
        return addCondition(ArchConditions.notBeNestedClasses());
    }

    @Override
    public ClassesShouldConjunction beMemberClasses() {
        return addCondition(ArchConditions.beMemberClasses());
    }

    @Override
    public ClassesShouldConjunction notBeMemberClasses() {
        return addCondition(ArchConditions.notBeMemberClasses());
    }

    @Override
    public ClassesShouldConjunction beInnerClasses() {
        return addCondition(ArchConditions.beInnerClasses());
    }

    @Override
    public ClassesShouldConjunction notBeInnerClasses() {
        return addCondition(ArchConditions.notBeInnerClasses());
    }

    @Override
    public ClassesShouldConjunction beAnonymousClasses() {
        return addCondition(ArchConditions.beAnonymousClasses());
    }

    @Override
    public ClassesShouldConjunction notBeAnonymousClasses() {
        return addCondition(ArchConditions.notBeAnonymousClasses());
    }

    @Override
    public ClassesShouldConjunction beLocalClasses() {
        return addCondition(ArchConditions.beLocalClasses());
    }

    @Override
    public ClassesShouldConjunction notBeLocalClasses() {
        return addCondition(ArchConditions.notBeLocalClasses());
    }

    @Override
    public ClassesShouldConjunction containNumberOfElements(DescribedPredicate<? super Integer> predicate) {
        return addCondition(ArchConditions.containNumberOfElements(predicate));
    }

    private ClassesShouldInternal copyWithNewCondition(ArchCondition<JavaClass> newCondition) {
        return new ClassesShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }

    ClassesShouldInternal addCondition(ArchCondition<? super JavaClass> condition) {
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
