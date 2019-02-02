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

import com.google.common.base.Supplier;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;

import static com.tngtech.archunit.base.DescribedPredicate.dont;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class ClassesShouldThatInternal implements ClassesShouldThat, ClassesShouldConjunction {
    private final ClassesShouldInternal classesShould;
    private final PredicateAggregator<JavaClass> predicateAggregator;
    private final Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>> createCondition;
    private final FinishedRule finishedRule = new FinishedRule();

    ClassesShouldThatInternal(ClassesShouldInternal classesShould,
            Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>> createCondition) {
        this(classesShould, new PredicateAggregator<JavaClass>(), createCondition);
    }

    private ClassesShouldThatInternal(ClassesShouldInternal classesShould,
            PredicateAggregator<JavaClass> predicateAggregator,
            Function<DescribedPredicate<JavaClass>, ArchCondition<JavaClass>> createCondition) {
        this.classesShould = classesShould;
        this.predicateAggregator = predicateAggregator;
        this.createCondition = createCondition;
    }

    @Override
    public ClassesShouldConjunction resideInAPackage(String packageIdentifier) {
        return shouldWith(JavaClass.Predicates.resideInAPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideInAnyPackage(String... packageIdentifiers) {
        return shouldWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackage(String packageIdentifier) {
        return shouldWith(JavaClass.Predicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return shouldWith(JavaClass.Predicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @Override
    public ClassesShouldConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(annotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public ClassesShouldConjunction areAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction areNotAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public ClassesShouldConjunction areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(annotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public ClassesShouldConjunction areMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(metaAnnotatedWith(annotationType)));
    }

    @Override
    public ClassesShouldConjunction areNotMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(not(metaAnnotatedWith(annotationType))));
    }

    @Override
    public ClassesShouldConjunction areMetaAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(metaAnnotatedWith(annotationTypeName)));
    }

    @Override
    public ClassesShouldConjunction areNotMetaAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(not(metaAnnotatedWith(annotationTypeName))));
    }

    @Override
    public ClassesShouldConjunction areMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(metaAnnotatedWith(predicate)));
    }

    @Override
    public ClassesShouldConjunction areNotMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(not(metaAnnotatedWith(predicate))));
    }

    @Override
    public ClassesShouldConjunction implement(Class<?> type) {
        return shouldWith(JavaClass.Predicates.implement(type));
    }

    @Override
    public ClassesShouldConjunction dontImplement(Class<?> type) {
        return shouldWith(dont(JavaClass.Predicates.implement(type)));
    }

    @Override
    public ClassesShouldConjunction implement(String typeName) {
        return shouldWith(JavaClass.Predicates.implement(typeName));
    }

    @Override
    public ClassesShouldConjunction dontImplement(String typeName) {
        return shouldWith(dont(JavaClass.Predicates.implement(typeName)));
    }

    @Override
    public ClassesShouldConjunction implement(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(JavaClass.Predicates.implement(predicate));
    }

    @Override
    public ClassesShouldConjunction dontImplement(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(dont(JavaClass.Predicates.implement(predicate)));
    }

    @Override
    public ClassesShouldConjunction haveNameMatching(String regex) {
        return shouldWith(have(nameMatching(regex)));
    }

    @Override
    public ClassesShouldConjunction haveNameNotMatching(String regex) {
        return shouldWith(SyntaxPredicates.haveNameNotMatching(regex));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameStartingWith(String prefix) {
        return shouldWith(have(simpleNameStartingWith(prefix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotStartingWith(String prefix) {
        return shouldWith(SyntaxPredicates.haveSimpleNameNotStartingWith(prefix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameContaining(String infix) {
        return shouldWith(have(simpleNameContaining(infix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotContaining(String infix) {
        return shouldWith(SyntaxPredicates.haveSimpleNameNotContaining(infix));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameEndingWith(String suffix) {
        return shouldWith(have(simpleNameEndingWith(suffix)));
    }

    @Override
    public ClassesShouldConjunction haveSimpleNameNotEndingWith(String suffix) {
        return shouldWith(SyntaxPredicates.haveSimpleNameNotEndingWith(suffix));
    }

    @Override
    public ClassesShouldConjunction areAssignableTo(Class<?> type) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(type)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableTo(Class<?> type) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(type))));
    }

    @Override
    public ClassesShouldConjunction areAssignableTo(String typeName) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(typeName)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableTo(String typeName) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(typeName))));
    }

    @Override
    public ClassesShouldConjunction areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(predicate)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(predicate))));
    }

    @Override
    public ClassesShouldConjunction areAssignableFrom(Class<?> type) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(type)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableFrom(Class<?> type) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(type))));
    }

    @Override
    public ClassesShouldConjunction areAssignableFrom(String typeName) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(typeName)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableFrom(String typeName) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(typeName))));
    }

    @Override
    public ClassesShouldConjunction areAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(predicate)));
    }

    @Override
    public ClassesShouldConjunction areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(predicate))));
    }

    @Override
    public ClassesShouldConjunction areInterfaces() {
        return shouldWith(are(JavaClass.Predicates.INTERFACES));
    }

    @Override
    public ClassesShouldConjunction areNotInterfaces() {
        return shouldWith(are(not(JavaClass.Predicates.INTERFACES)));
    }

    @Override
    public ClassesShouldConjunction arePublic() {
        return shouldWith(SyntaxPredicates.arePublic());
    }

    @Override
    public ClassesShouldConjunction areNotPublic() {
        return shouldWith(SyntaxPredicates.areNotPublic());
    }

    @Override
    public ClassesShouldConjunction areProtected() {
        return shouldWith(SyntaxPredicates.areProtected());
    }

    @Override
    public ClassesShouldConjunction areNotProtected() {
        return shouldWith(SyntaxPredicates.areNotProtected());
    }

    @Override
    public ClassesShouldConjunction arePackagePrivate() {
        return shouldWith(SyntaxPredicates.arePackagePrivate());
    }

    @Override
    public ClassesShouldConjunction areNotPackagePrivate() {
        return shouldWith(SyntaxPredicates.areNotPackagePrivate());
    }

    @Override
    public ClassesShouldConjunction arePrivate() {
        return shouldWith(SyntaxPredicates.arePrivate());
    }

    @Override
    public ClassesShouldConjunction areNotPrivate() {
        return shouldWith(SyntaxPredicates.areNotPrivate());
    }

    @Override
    public ClassesShouldConjunction haveFullyQualifiedName(String name) {
        return shouldWith(SyntaxPredicates.haveFullyQualifiedName(name));
    }

    @Override
    public ClassesShouldConjunction dontHaveFullyQualifiedName(String name) {
        return shouldWith(SyntaxPredicates.dontHaveFullyQualifiedName(name));
    }

    @Override
    public ClassesShouldConjunction haveSimpleName(String name) {
        return shouldWith(SyntaxPredicates.haveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction dontHaveSimpleName(String name) {
        return shouldWith(SyntaxPredicates.dontHaveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction haveModifier(JavaModifier modifier) {
        return shouldWith(SyntaxPredicates.haveModifier(modifier));
    }

    @Override
    public ClassesShouldConjunction dontHaveModifier(JavaModifier modifier) {
        return shouldWith(SyntaxPredicates.dontHaveModifier(modifier));
    }

    @Override
    public String getDescription() {
        return finishedRule.get().getDescription();
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        return finishedRule.get().evaluate(classes);
    }

    @Override
    public void check(JavaClasses classes) {
        finishedRule.get().check(classes);
    }

    @Override
    public ArchRule because(String reason) {
        return ArchRule.Factory.withBecause(this, reason);
    }

    @Override
    public ArchRule as(String description) {
        return finishedRule.get().as(description);
    }

    private ClassesShouldThatInternal shouldWith(DescribedPredicate<? super JavaClass> predicate) {
        return new ClassesShouldThatInternal(classesShould,
                predicateAggregator.add(predicate),
                createCondition);
    }

    @Override
    public ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition) {
        return classesShould.addCondition(createCondition.apply(predicateAggregator.get())).andShould(condition);
    }

    @Override
    public ClassesShould andShould() {
        return classesShould.addCondition(createCondition.apply(predicateAggregator.get())).andShould();
    }

    @Override
    public ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition) {
        return classesShould.addCondition(createCondition.apply(predicateAggregator.get())).orShould(condition);
    }

    @Override
    public ClassesShould orShould() {
        return classesShould.addCondition(createCondition.apply(predicateAggregator.get())).orShould();
    }

    private class FinishedRule implements Supplier<ArchRule> {
        @Override
        public ArchRule get() {
            return classesShould.copyWithNewCondition(
                    classesShould.conditionAggregator
                            .add(createCondition.apply(predicateAggregator.get())));
        }
    }
}
