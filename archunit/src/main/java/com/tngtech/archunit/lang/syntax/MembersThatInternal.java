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
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.MembersThat;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullName;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class MembersThatInternal<
        MEMBER extends JavaMember,
        CONJUNCTION extends AbstractGivenMembersInternal<MEMBER, CONJUNCTION>
        >
        implements MembersThat<CONJUNCTION> {

    final CONJUNCTION givenMembers;
    final PredicateAggregator<MEMBER> currentPredicate;

    MembersThatInternal(
            CONJUNCTION givenMembers, PredicateAggregator<MEMBER> currentPredicate) {
        this.givenMembers = givenMembers;
        this.currentPredicate = currentPredicate;
    }

    @Override
    public CONJUNCTION haveName(String name) {
        return givenWith(have(name(name)));
    }

    @Override
    public CONJUNCTION doNotHaveName(String name) {
        return givenWith(doNot(have(name(name))));
    }

    @Override
    public CONJUNCTION haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public CONJUNCTION haveNameNotMatching(String regex) {
        return givenWith(SyntaxPredicates.haveNameNotMatching(regex));
    }

    @Override
    public CONJUNCTION haveFullName(String fullName) {
        return givenWith(have(fullName(fullName)));
    }

    @Override
    public CONJUNCTION doNotHaveFullName(String fullName) {
        return givenWith(doNot(have(fullName(fullName))));
    }

    @Override
    public CONJUNCTION haveFullNameMatching(String regex) {
        return givenWith(have(fullNameMatching(regex)));
    }

    @Override
    public CONJUNCTION haveFullNameNotMatching(String regex) {
        return givenWith(have(not(fullNameMatching(regex)).as("full name not matching '%s'", regex)));
    }

    @Override
    public CONJUNCTION arePublic() {
        return givenWith(SyntaxPredicates.arePublic());
    }

    @Override
    public CONJUNCTION areNotPublic() {
        return givenWith(SyntaxPredicates.areNotPublic());
    }

    @Override
    public CONJUNCTION areProtected() {
        return givenWith(SyntaxPredicates.areProtected());
    }

    @Override
    public CONJUNCTION areNotProtected() {
        return givenWith(SyntaxPredicates.areNotProtected());
    }

    @Override
    public CONJUNCTION arePackagePrivate() {
        return givenWith(SyntaxPredicates.arePackagePrivate());
    }

    @Override
    public CONJUNCTION areNotPackagePrivate() {
        return givenWith(SyntaxPredicates.areNotPackagePrivate());
    }

    @Override
    public CONJUNCTION arePrivate() {
        return givenWith(SyntaxPredicates.arePrivate());
    }

    @Override
    public CONJUNCTION areNotPrivate() {
        return givenWith(SyntaxPredicates.areNotPrivate());
    }

    // only applicable to fields and methods; therefore not exposed via MembersThat
    public CONJUNCTION areStatic() {
        return givenWith(SyntaxPredicates.areStatic());
    }

    // only applicable to fields and methods; therefore not exposed via MembersThat
    public CONJUNCTION areNotStatic() {
        return givenWith(SyntaxPredicates.areNotStatic());
    }

    // only applicable to (classes,) fields and methods; therefore not exposed via MembersThat
    public CONJUNCTION areFinal() {
        return givenWith(SyntaxPredicates.areFinal());
    }

    // only applicable to (classes,) fields and methods; therefore not exposed via MembersThat
    public CONJUNCTION areNotFinal() {
        return givenWith(SyntaxPredicates.areNotFinal());
    }

    @Override
    public CONJUNCTION haveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.haveModifier(modifier));
    }

    @Override
    public CONJUNCTION doNotHaveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.doNotHaveModifier(modifier));
    }

    @Override
    public CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(annotatedWith(annotationType)));
    }

    @Override
    public CONJUNCTION areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public CONJUNCTION areAnnotatedWith(String annotationTypeName) {
        return givenWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public CONJUNCTION areNotAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public CONJUNCTION areAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return givenWith(are(annotatedWith(predicate)));
    }

    @Override
    public CONJUNCTION areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return givenWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public CONJUNCTION areMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(metaAnnotatedWith(annotationType)));
    }

    @Override
    public CONJUNCTION areNotMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(metaAnnotatedWith(annotationType))));
    }

    @Override
    public CONJUNCTION areMetaAnnotatedWith(String annotationTypeName) {
        return givenWith(are(metaAnnotatedWith(annotationTypeName)));
    }

    @Override
    public CONJUNCTION areNotMetaAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(metaAnnotatedWith(annotationTypeName))));
    }

    @Override
    public CONJUNCTION areMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return givenWith(are(metaAnnotatedWith(predicate)));
    }

    @Override
    public CONJUNCTION areNotMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return givenWith(are(not(metaAnnotatedWith(predicate))));
    }

    @Override
    public CONJUNCTION areDeclaredIn(Class<?> javaClass) {
        return givenWith(are(declaredIn(javaClass)));
    }

    @Override
    public CONJUNCTION areNotDeclaredIn(Class<?> javaClass) {
        return givenWith(are(not(declaredIn(javaClass))));
    }

    @Override
    public CONJUNCTION areDeclaredIn(String className) {
        return givenWith(are(declaredIn(className)));
    }

    @Override
    public CONJUNCTION areNotDeclaredIn(String className) {
        return givenWith(are(not(declaredIn(className))));
    }

    @Override
    public CONJUNCTION areDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(declaredInClassesThat(predicate)));
    }

    @Override
    public ClassesThat<CONJUNCTION> areDeclaredInClassesThat() {
        return new MembersDeclaredInClassesThat<>(new Function<DescribedPredicate<? super JavaClass>, CONJUNCTION>() {
            @Override
            public CONJUNCTION apply(DescribedPredicate<? super JavaClass> predicate) {
                return givenWith(are(declaredInClassesThat(predicate)));
            }
        });
    }

    private DescribedPredicate<JavaMember> declaredInClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return declaredIn(predicate).as("declared in classes that %s", predicate.getDescription());
    }

    private CONJUNCTION givenWith(DescribedPredicate<? super MEMBER> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
