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
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.MembersShould;
import com.tngtech.archunit.lang.syntax.elements.MembersShouldConjunction;

abstract class AbstractMembersShouldInternal<MEMBER extends JavaMember, SELF extends AbstractMembersShouldInternal<MEMBER, SELF>>
        extends ObjectsShouldInternal<MEMBER>
        implements MembersShouldConjunction<MEMBER>, MembersShould<SELF> {

    AbstractMembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {
        super(classesTransformer, priority, prepareCondition);
    }

    AbstractMembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            ArchCondition<MEMBER> condition,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }

    AbstractMembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            ConditionAggregator<MEMBER> conditionAggregator,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {
        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    public SELF haveName(String name) {
        return addCondition(ArchConditions.<JavaMember>haveName(name));
    }

    @Override
    public SELF notHaveName(String name) {
        return addCondition(ArchConditions.notHaveName(name));
    }

    @Override
    public SELF haveNameMatching(String regex) {
        return addCondition(ArchConditions.haveNameMatching(regex));
    }

    @Override
    public SELF haveNameNotMatching(String regex) {
        return addCondition(ArchConditions.haveNameNotMatching(regex));
    }

    @Override
    public SELF haveFullName(String fullName) {
        return addCondition(ArchConditions.haveFullName(fullName));
    }

    @Override
    public SELF notHaveFullName(String fullName) {
        return addCondition(ArchConditions.notHaveFullName(fullName));
    }

    @Override
    public SELF haveFullNameMatching(String regex) {
        return addCondition(ArchConditions.haveFullNameMatching(regex));
    }

    @Override
    public SELF haveFullNameNotMatching(String regex) {
        return addCondition(ArchConditions.haveFullNameNotMatching(regex));
    }
    @Override
    public SELF bePublic() {
        return addCondition(ArchConditions.bePublic());
    }

    @Override
    public SELF notBePublic() {
        return addCondition(ArchConditions.notBePublic());
    }

    @Override
    public SELF beProtected() {
        return addCondition(ArchConditions.beProtected());
    }

    @Override
    public SELF notBeProtected() {
        return addCondition(ArchConditions.notBeProtected());
    }

    @Override
    public SELF bePackagePrivate() {
        return addCondition(ArchConditions.bePackagePrivate());
    }

    @Override
    public SELF notBePackagePrivate() {
        return addCondition(ArchConditions.notBePackagePrivate());
    }

    @Override
    public SELF bePrivate() {
        return addCondition(ArchConditions.bePrivate());
    }

    @Override
    public SELF notBePrivate() {
        return addCondition(ArchConditions.notBePrivate());
    }

    // only applicable to fields and methods; therefore not exposed via MembersShould
    public SELF beStatic() {
        return addCondition(ArchConditions.beStatic());
    }

    // only applicable to fields and methods; therefore not exposed via MembersShould
    public SELF notBeStatic() {
        return addCondition(ArchConditions.notBeStatic());
    }

    // only applicable to fields and methods; therefore not exposed via MembersShould
    public SELF beFinal() {
        return addCondition(ArchConditions.beFinal());
    }

    // only applicable to fields and methods; therefore not exposed via MembersShould
    public SELF notBeFinal() {
        return addCondition(ArchConditions.notBeFinal());
    }

    @Override
    public SELF haveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.haveModifier(modifier));
    }

    @Override
    public SELF notHaveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.notHaveModifier(modifier));
    }

    @Override
    public SELF beAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationType));
    }

    @Override
    public SELF notBeAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationType));
    }

    @Override
    public SELF beAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationTypeName));
    }

    @Override
    public SELF notBeAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationTypeName));
    }

    @Override
    public SELF beAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.beAnnotatedWith(predicate));
    }

    @Override
    public SELF notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.notBeAnnotatedWith(predicate));
    }

    @Override
    public SELF beMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationType));
    }

    @Override
    public SELF notBeMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationType));
    }

    @Override
    public SELF beMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public SELF notBeMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public SELF beMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(predicate));
    }

    @Override
    public SELF notBeMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(predicate));
    }

    @Override
    public SELF beDeclaredIn(Class<?> javaClass) {
        return addCondition(ArchConditions.beDeclaredIn(javaClass));
    }

    @Override
    public SELF notBeDeclaredIn(Class<?> javaClass) {
        return addCondition(ArchConditions.notBeDeclaredIn(javaClass));
    }

    @Override
    public SELF beDeclaredIn(String className) {
        return addCondition(ArchConditions.beDeclaredIn(className));
    }

    @Override
    public SELF notBeDeclaredIn(String className) {
        return addCondition(ArchConditions.notBeDeclaredIn(className));
    }

    @Override
    public SELF beDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.beDeclaredInClassesThat(predicate));
    }

    @Override
    public ClassesThat<SELF> beDeclaredInClassesThat() {
        return new ClassesThatInternal<>(new Function<DescribedPredicate<? super JavaClass>, SELF>() {
            @Override
            public SELF apply(DescribedPredicate<? super JavaClass> predicate) {
                return addCondition(ArchConditions.beDeclaredInClassesThat(predicate));
            }
        });
    }

    private SELF copyWithNewCondition(ArchCondition<? super MEMBER> newCondition) {
        return copyWithNewCondition(new ConditionAggregator<>(newCondition.<MEMBER>forSubType()));
    }

    abstract SELF copyWithNewCondition(ConditionAggregator<MEMBER> newCondition);

    SELF addCondition(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator.add(condition));
    }

    @Override
    public SELF andShould(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatANDsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should"))
                .add(condition));
    }

    @Override
    public SELF andShould() {
        return copyWithNewCondition(conditionAggregator.thatANDsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should")));
    }

    @Override
    public SELF orShould(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatORsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should"))
                .add(condition));
    }

    @Override
    public SELF orShould() {
        return copyWithNewCondition(conditionAggregator.thatORsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should")));
    }

    static class MembersShouldInternal extends AbstractMembersShouldInternal<JavaMember, MembersShouldInternal> {

        MembersShouldInternal(
                ClassesTransformer<? extends JavaMember> classesTransformer,
                Priority priority,
                Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition) {
            super(classesTransformer, priority, prepareCondition);
        }

        MembersShouldInternal(
                ClassesTransformer<? extends JavaMember> classesTransformer,
                Priority priority,
                ArchCondition<JavaMember> condition,
                Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition) {
            super(classesTransformer, priority, condition, prepareCondition);
        }

        MembersShouldInternal(
                ClassesTransformer<? extends JavaMember> classesTransformer,
                Priority priority,
                ConditionAggregator<JavaMember> conditionAggregator,
                Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition) {
            super(classesTransformer, priority, conditionAggregator, prepareCondition);
        }

        @Override
        MembersShouldInternal copyWithNewCondition(ConditionAggregator<JavaMember> newCondition) {
            return new MembersShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
        }
    }
}
