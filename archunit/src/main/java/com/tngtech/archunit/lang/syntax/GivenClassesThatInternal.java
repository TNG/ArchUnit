/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class GivenClassesThatInternal implements GivenClassesThat {
    private final GivenClassesInternal givenClasses;
    private final PredicateAggregator<JavaClass> currentPredicate;

    GivenClassesThatInternal(GivenClassesInternal givenClasses, PredicateAggregator<JavaClass> predicate) {
        this.givenClasses = givenClasses;
        this.currentPredicate = predicate;
    }

    @Override
    public GivenClassesConjunction resideInAPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideInAnyPackage(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackage(String packageIdentifier) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public GivenClassesConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return givenWith(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(annotatedWith(annotationType)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return givenWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(String annotationTypeName) {
        return givenWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(String annotationTypeName) {
        return givenWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public GivenClassesConjunction areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(annotatedWith(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return givenWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public GivenClassesConjunction implement(Class<?> type) {
        return givenWith(JavaClass.Predicates.implement(type));
    }

    @Override
    public GivenClassesConjunction dontImplement(Class<?> type) {
        return givenWith(dont(JavaClass.Predicates.implement(type)));
    }

    @Override
    public GivenClassesConjunction implement(String typeName) {
        return givenWith(JavaClass.Predicates.implement(typeName));
    }

    @Override
    public GivenClassesConjunction dontImplement(String typeName) {
        return givenWith(dont(JavaClass.Predicates.implement(typeName)));
    }

    @Override
    public GivenClassesConjunction implement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(JavaClass.Predicates.implement(predicate));
    }

    @Override
    public GivenClassesConjunction dontImplement(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(dont(JavaClass.Predicates.implement(predicate)));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameStartingWith(String prefix) {
        return givenWith(have(simpleNameStartingWith(prefix)));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameNotStartingWith(String prefix) {
        return givenWith(ClassesThatPredicates.haveSimpleNameNotStartingWith(prefix));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameContaining(String infix) {
        return givenWith(have(simpleNameContaining(infix)));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameNotContaining(String infix) {
        return givenWith(ClassesThatPredicates.haveSimpleNameNotContaining(infix));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameEndingWith(String suffix) {
        return givenWith(have(simpleNameEndingWith(suffix)));
    }

    @Override
    public GivenClassesConjunction haveSimpleNameNotEndingWith(String suffix) {
        return givenWith(ClassesThatPredicates.haveSimpleNameNotEndingWith(suffix));
    }

    @Override
    public GivenClassesConjunction haveNameMatching(String regex) {
        return givenWith(have(nameMatching(regex)));
    }

    @Override
    public GivenClassesConjunction haveNameNotMatching(String regex) {
        return givenWith(ClassesThatPredicates.haveNameNotMatching(regex));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(Class<?> type) {
        return givenWith(are(assignableTo(type)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(Class<?> type) {
        return givenWith(are(not(assignableTo(type))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(String typeName) {
        return givenWith(are(assignableTo(typeName)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(String typeName) {
        return givenWith(are(not(assignableTo(typeName))));
    }

    @Override
    public GivenClassesConjunction areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(assignableTo(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(assignableTo(predicate))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(Class<?> type) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(type)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(Class<?> type) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(type))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(String typeName) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(typeName)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(String typeName) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(typeName))));
    }

    @Override
    public GivenClassesConjunction areAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(JavaClass.Predicates.assignableFrom(predicate)));
    }

    @Override
    public GivenClassesConjunction areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return givenWith(are(not(JavaClass.Predicates.assignableFrom(predicate))));
    }

    @Override
    public GivenClassesConjunction areInterfaces() {
        return givenWith(are(JavaClass.Predicates.INTERFACES));
    }

    @Override
    public GivenClassesConjunction areNotInterfaces() {
        return givenWith(are(not(JavaClass.Predicates.INTERFACES)));
    }

    @Override
    public GivenClassesConjunction arePublic() {
        return givenWith(ClassesThatPredicates.arePublic());
    }

    @Override
    public GivenClassesConjunction areNotPublic() {
        return givenWith(ClassesThatPredicates.areNotPublic());
    }

    @Override
    public GivenClassesConjunction areProtected() {
        return givenWith(ClassesThatPredicates.areProtected());
    }

    @Override
    public GivenClassesConjunction areNotProtected() {
        return givenWith(ClassesThatPredicates.areNotProtected());
    }

    @Override
    public GivenClassesConjunction arePackagePrivate() {
        return givenWith(ClassesThatPredicates.arePackagePrivate());
    }

    @Override
    public GivenClassesConjunction areNotPackagePrivate() {
        return givenWith(ClassesThatPredicates.areNotPackagePrivate());
    }

    @Override
    public GivenClassesConjunction arePrivate() {
        return givenWith(ClassesThatPredicates.arePrivate());
    }

    @Override
    public GivenClassesConjunction areNotPrivate() {
        return givenWith(ClassesThatPredicates.areNotPrivate());
    }

    @Override
    public GivenClassesConjunction haveFullyQualifiedName(String name) {
        return givenWith(ClassesThatPredicates.haveFullyQualifiedName(name));
    }

    @Override
    public GivenClassesConjunction dontHaveFullyQualifiedName(String name) {
        return givenWith(ClassesThatPredicates.dontHaveFullyQualifiedName(name));
    }

    @Override
    public GivenClassesConjunction haveSimpleName(String name) {
        return givenWith(ClassesThatPredicates.haveSimpleName(name));
    }

    @Override
    public GivenClassesConjunction dontHaveSimpleName(String name) {
        return givenWith(ClassesThatPredicates.dontHaveSimpleName(name));
    }

    @Override
    public GivenClassesConjunction haveModifier(JavaModifier modifier) {
        return givenWith(ClassesThatPredicates.haveModifier(modifier));
    }

    @Override
    public GivenClassesConjunction dontHaveModifier(JavaModifier modifier) {
        return givenWith(ClassesThatPredicates.dontHaveModifier(modifier));
    }

    private GivenClassesInternal givenWith(DescribedPredicate<? super JavaClass> predicate) {
        return givenClasses.with(currentPredicate.add(predicate));
    }
}
