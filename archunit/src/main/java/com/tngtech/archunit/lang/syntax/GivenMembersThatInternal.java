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
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersConjunction;
import com.tngtech.archunit.lang.syntax.elements.MembersThat;

import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

public class GivenMembersThatInternal<MEMBER extends JavaMember> implements MembersThat<GivenMembersConjunction<MEMBER>> {
    private final AbstractGivenMembersInternal<MEMBER, ?> givenMembers;
    private final PredicateAggregator<MEMBER> currentPredicate;

    GivenMembersThatInternal(AbstractGivenMembersInternal<MEMBER, ?> givenMembers, PredicateAggregator<MEMBER> currentPredicate) {
        this.givenMembers = givenMembers;
        this.currentPredicate = currentPredicate;
    }

    @Override
    public GivenMembersConjunction<MEMBER> haveName(String name) {
        return givenWith(have(name(name)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> dontHaveName(String name) {
        return givenWith(dont(have(name(name))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> haveNameNotMatching(String regex) {
        return givenWith(SyntaxPredicates.haveNameNotMatching(regex));
    }

    @Override
    public GivenMembersConjunction<MEMBER> arePublic() {
        return givenWith(SyntaxPredicates.arePublic());
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotPublic() {
        return givenWith(SyntaxPredicates.areNotPublic());
    }

    @Override
    public GivenMembersConjunction<MEMBER> areProtected() {
        return givenWith(SyntaxPredicates.areProtected());
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotProtected() {
        return givenWith(SyntaxPredicates.areNotProtected());
    }

    @Override
    public GivenMembersConjunction<MEMBER> arePackagePrivate() {
        return givenWith(SyntaxPredicates.arePackagePrivate());
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotPackagePrivate() {
        return givenWith(SyntaxPredicates.areNotPackagePrivate());
    }

    @Override
    public GivenMembersConjunction<MEMBER> arePrivate() {
        return givenWith(SyntaxPredicates.arePrivate());
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotPrivate() {
        return givenWith(SyntaxPredicates.areNotPrivate());
    }

    @Override
    public GivenMembersConjunction<MEMBER> haveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.haveModifier(modifier));
    }

    @Override
    public GivenMembersConjunction<MEMBER> dontHaveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.dontHaveModifier(modifier));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(annotatedWith(annotationType)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areAnnotatedWith(String annotationTypeName) {
        return givenWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(annotatedWith(predicate)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(metaAnnotatedWith(annotationType)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(metaAnnotatedWith(annotationType))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areMetaAnnotatedWith(String annotationTypeName) {
        return givenWith(are(metaAnnotatedWith(annotationTypeName)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotMetaAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(metaAnnotatedWith(annotationTypeName))));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(metaAnnotatedWith(predicate)));
    }

    @Override
    public GivenMembersConjunction<MEMBER> areNotMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(not(metaAnnotatedWith(predicate))));
    }

    private AbstractGivenMembersInternal<MEMBER, ?> givenWith(DescribedPredicate<? super MEMBER> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
