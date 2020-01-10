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
import com.tngtech.archunit.lang.syntax.elements.GivenMembersConjunction;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANONYMOUS_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INNER_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.LOCAL_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.MEMBER_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.NESTED_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.TOP_LEVEL_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class MembersDeclaredInClassesThat<MEMBER extends JavaMember, CONJUNCTION extends GivenMembersConjunction<MEMBER>>
        implements ClassesThat<CONJUNCTION> {
    private final Function<DescribedPredicate<? super JavaClass>, CONJUNCTION> predicateAggregator;

    MembersDeclaredInClassesThat(Function<DescribedPredicate<? super JavaClass>, CONJUNCTION> predicateAggregator) {
        this.predicateAggregator = predicateAggregator;
    }

    @Override
    public CONJUNCTION haveFullyQualifiedName(String name) {
        return givenWith(SyntaxPredicates.haveFullyQualifiedName(name));
    }

    @Override
    public CONJUNCTION dontHaveFullyQualifiedName(String name) {
        return givenWith(SyntaxPredicates.dontHaveFullyQualifiedName(name));
    }

    @Override
    public CONJUNCTION doNotHaveFullyQualifiedName(String name) {
        return givenWith(SyntaxPredicates.doNotHaveFullyQualifiedName(name));
    }

    @Override
    public CONJUNCTION haveSimpleName(String name) {
        return givenWith(SyntaxPredicates.haveSimpleName(name));
    }

    @Override
    public CONJUNCTION dontHaveSimpleName(String name) {
        return givenWith(SyntaxPredicates.dontHaveSimpleName(name));
    }

    @Override
    public CONJUNCTION doNotHaveSimpleName(String name) {
        return givenWith(SyntaxPredicates.doNotHaveSimpleName(name));
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
    public CONJUNCTION haveSimpleNameStartingWith(String prefix) {
        return givenWith(have(simpleNameStartingWith(prefix)));
    }

    @Override
    public CONJUNCTION haveSimpleNameNotStartingWith(String prefix) {
        return givenWith(SyntaxPredicates.haveSimpleNameNotStartingWith(prefix));
    }

    @Override
    public CONJUNCTION haveSimpleNameContaining(String infix) {
        return givenWith(have(simpleNameContaining(infix)));
    }

    @Override
    public CONJUNCTION haveSimpleNameNotContaining(String infix) {
        return givenWith(SyntaxPredicates.haveSimpleNameNotContaining(infix));
    }

    @Override
    public CONJUNCTION haveSimpleNameEndingWith(String suffix) {
        return givenWith(have(simpleNameEndingWith(suffix)));
    }

    @Override
    public CONJUNCTION haveSimpleNameNotEndingWith(String suffix) {
        return givenWith(SyntaxPredicates.haveSimpleNameNotEndingWith(suffix));
    }

    @Override
    public CONJUNCTION resideInAPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @Override
    public CONJUNCTION resideInAnyPackage(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public CONJUNCTION resideOutsideOfPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public CONJUNCTION resideOutsideOfPackages(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
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

    @Override
    public CONJUNCTION haveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.haveModifier(modifier));
    }

    @Override
    public CONJUNCTION dontHaveModifier(JavaModifier modifier) {
        return givenWith(SyntaxPredicates.dontHaveModifier(modifier));
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
    public CONJUNCTION implement(Class<?> type) {
        return givenWith(JavaClass.Predicates.implement(type));
    }

    @Override
    public CONJUNCTION dontImplement(Class<?> type) {
        return givenWith(dont(JavaClass.Predicates.implement(type)));
    }

    @Override
    public CONJUNCTION doNotImplement(Class<?> type) {
        return givenWith(doNot(JavaClass.Predicates.implement(type)));
    }

    @Override
    public CONJUNCTION implement(String typeName) {
        return givenWith(JavaClass.Predicates.implement(typeName));
    }

    @Override
    public CONJUNCTION dontImplement(String typeName) {
        return givenWith(dont(JavaClass.Predicates.implement(typeName)));
    }

    @Override
    public CONJUNCTION doNotImplement(String typeName) {
        return givenWith(doNot(JavaClass.Predicates.implement(typeName)));
    }

    @Override
    public CONJUNCTION implement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(JavaClass.Predicates.implement(predicate));
    }

    @Override
    public CONJUNCTION dontImplement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(dont(JavaClass.Predicates.implement(predicate)));
    }

    @Override
    public CONJUNCTION doNotImplement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(doNot(JavaClass.Predicates.implement(predicate)));
    }

    @Override
    public CONJUNCTION areAssignableTo(Class<?> type) {
        return givenWith(are(assignableTo(type)));
    }

    @Override
    public CONJUNCTION areNotAssignableTo(Class<?> type) {
        return givenWith(are(not(assignableTo(type))));
    }

    @Override
    public CONJUNCTION areAssignableTo(String typeName) {
        return givenWith(are(assignableTo(typeName)));
    }

    @Override
    public CONJUNCTION areNotAssignableTo(String typeName) {
        return givenWith(are(not(assignableTo(typeName))));
    }

    @Override
    public CONJUNCTION areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(assignableTo(predicate)));
    }

    @Override
    public CONJUNCTION areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(assignableTo(predicate))));
    }

    @Override
    public CONJUNCTION areAssignableFrom(Class<?> type) {
        return givenWith(are(assignableFrom(type)));
    }

    @Override
    public CONJUNCTION areNotAssignableFrom(Class<?> type) {
        return givenWith(are(not(assignableFrom(type))));
    }

    @Override
    public CONJUNCTION areAssignableFrom(String typeName) {
        return givenWith(are(assignableFrom(typeName)));
    }

    @Override
    public CONJUNCTION areNotAssignableFrom(String typeName) {
        return givenWith(are(not(assignableFrom(typeName))));
    }

    @Override
    public CONJUNCTION areAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(assignableFrom(predicate)));
    }

    @Override
    public CONJUNCTION areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(assignableFrom(predicate))));
    }

    @Override
    public CONJUNCTION areInterfaces() {
        return givenWith(are(INTERFACES));
    }

    @Override
    public CONJUNCTION areNotInterfaces() {
        return givenWith(are(not(INTERFACES)));
    }

    @Override
    public CONJUNCTION areEnums() {
        return givenWith(are(ENUMS));
    }

    @Override
    public CONJUNCTION areNotEnums() {
        return givenWith(are(not(ENUMS)));
    }

    @Override
    public CONJUNCTION areTopLevelClasses() {
        return givenWith(are(TOP_LEVEL_CLASSES));
    }

    @Override
    public CONJUNCTION areNotTopLevelClasses() {
        return givenWith(are(not(TOP_LEVEL_CLASSES)));
    }

    @Override
    public CONJUNCTION areNestedClasses() {
        return givenWith(are(NESTED_CLASSES));
    }

    @Override
    public CONJUNCTION areNotNestedClasses() {
        return givenWith(are(not(NESTED_CLASSES)));
    }

    @Override
    public CONJUNCTION areMemberClasses() {
        return givenWith(are(MEMBER_CLASSES));
    }

    @Override
    public CONJUNCTION areNotMemberClasses() {
        return givenWith(are(not(MEMBER_CLASSES)));
    }

    @Override
    public CONJUNCTION areInnerClasses() {
        return givenWith(are(INNER_CLASSES));
    }

    @Override
    public CONJUNCTION areNotInnerClasses() {
        return givenWith(are(not(INNER_CLASSES)));
    }

    @Override
    public CONJUNCTION areAnonymousClasses() {
        return givenWith(are(ANONYMOUS_CLASSES));
    }

    @Override
    public CONJUNCTION areNotAnonymousClasses() {
        return givenWith(are(not(ANONYMOUS_CLASSES)));
    }

    @Override
    public CONJUNCTION areLocalClasses() {
        return givenWith(are(LOCAL_CLASSES));
    }

    @Override
    public CONJUNCTION areNotLocalClasses() {
        return givenWith(are(not(LOCAL_CLASSES)));
    }

    @Override
    public CONJUNCTION belongToAnyOf(final Class<?>... classes) {
        return givenWith(JavaClass.Predicates.belongToAnyOf(classes));
    }

    private CONJUNCTION givenWith(DescribedPredicate<? super JavaClass> predicate) {
        return predicateAggregator.apply(predicate);
    }
}
