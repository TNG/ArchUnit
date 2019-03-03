/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.lang.syntax.elements.MembersShould;
import com.tngtech.archunit.lang.syntax.elements.MembersShouldConjunction;

class MembersShouldInternal<MEMBER extends JavaMember> extends ObjectsShouldInternal<MEMBER>
        implements MembersShouldConjunction<MEMBER>, MembersShould<MembersShouldConjunction<MEMBER>> {

    MembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {
        super(classesTransformer, priority, prepareCondition);
    }

    MembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            ArchCondition<MEMBER> condition,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {

        super(classesTransformer, priority, condition, prepareCondition);
    }

    private MembersShouldInternal(
            ClassesTransformer<? extends MEMBER> classesTransformer,
            Priority priority,
            ConditionAggregator<MEMBER> conditionAggregator,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition) {
        super(classesTransformer, priority, conditionAggregator, prepareCondition);
    }

    @Override
    public MembersShouldConjunction<MEMBER> haveName(String name) {
        return addCondition(ArchConditions.<JavaMember>haveName(name));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notHaveName(String name) {
        return addCondition(ArchConditions.notHaveName(name));
    }

    @Override
    public MembersShouldConjunction<MEMBER> haveNameMatching(String regex) {
        return addCondition(ArchConditions.haveNameMatching(regex));
    }

    @Override
    public MembersShouldConjunction<MEMBER> haveNameNotMatching(String regex) {
        return addCondition(ArchConditions.haveNameNotMatching(regex));
    }

    @Override
    public MembersShouldConjunction<MEMBER> bePublic() {
        return addCondition(ArchConditions.bePublic());
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBePublic() {
        return addCondition(ArchConditions.notBePublic());
    }

    @Override
    public MembersShouldConjunction<MEMBER> beProtected() {
        return addCondition(ArchConditions.beProtected());
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeProtected() {
        return addCondition(ArchConditions.notBeProtected());
    }

    @Override
    public MembersShouldConjunction<MEMBER> bePackagePrivate() {
        return addCondition(ArchConditions.bePackagePrivate());
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBePackagePrivate() {
        return addCondition(ArchConditions.notBePackagePrivate());
    }

    @Override
    public MembersShouldConjunction<MEMBER> bePrivate() {
        return addCondition(ArchConditions.bePrivate());
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBePrivate() {
        return addCondition(ArchConditions.notBePrivate());
    }

    @Override
    public MembersShouldConjunction<MEMBER> haveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.haveModifier(modifier));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notHaveModifier(JavaModifier modifier) {
        return addCondition(ArchConditions.notHaveModifier(modifier));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationType));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationType));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beAnnotatedWith(annotationTypeName));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeAnnotatedWith(annotationTypeName));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return addCondition(ArchConditions.beAnnotatedWith(predicate));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return addCondition(ArchConditions.notBeAnnotatedWith(predicate));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationType));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationType));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeMetaAnnotatedWith(String annotationTypeName) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(annotationTypeName));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return addCondition(ArchConditions.beMetaAnnotatedWith(predicate));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return addCondition(ArchConditions.notBeMetaAnnotatedWith(predicate));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beDeclaredIn(Class<?> javaClass) {
        return addCondition(ArchConditions.beDeclaredIn(javaClass));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeDeclaredIn(Class<?> javaClass) {
        return addCondition(ArchConditions.notBeDeclaredIn(javaClass));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beDeclaredIn(String className) {
        return addCondition(ArchConditions.beDeclaredIn(className));
    }

    @Override
    public MembersShouldConjunction<MEMBER> notBeDeclaredIn(String className) {
        return addCondition(ArchConditions.notBeDeclaredIn(className));
    }

    @Override
    public MembersShouldConjunction<MEMBER> beDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return addCondition(ArchConditions.beDeclaredInClassesThat(predicate));
    }

    private MembersShouldInternal<MEMBER> copyWithNewCondition(ArchCondition<? super MEMBER> newCondition) {
        return new MembersShouldInternal<>(classesTransformer, priority, newCondition.<MEMBER>forSubType(), prepareCondition);
    }

    private MembersShouldInternal<MEMBER> addCondition(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator.add(condition));
    }

    @Override
    public MembersShouldConjunction<MEMBER> andShould(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatANDsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should"))
                .add(condition));
    }

    @Override
    public MembersShould<MembersShouldConjunction<MEMBER>> andShould() {
        return new MembersShouldInternal<>(
                classesTransformer,
                priority,
                conditionAggregator.thatANDsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should")),
                prepareCondition);
    }

    @Override
    public MembersShouldConjunction<MEMBER> orShould(ArchCondition<? super MEMBER> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatORsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should"))
                .add(condition));
    }

    @Override
    public MembersShould<MembersShouldConjunction<MEMBER>> orShould() {
        return new MembersShouldInternal<>(
                classesTransformer,
                priority,
                conditionAggregator.thatORsWith(ObjectsShouldInternal.<MEMBER>prependDescription("should")),
                prepareCondition);
    }
}
