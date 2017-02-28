package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.google.common.base.Supplier;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.properties.HasName.Predicates.nameMatching;
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
    public ClassesShouldConjunction resideInPackage(String packageIdentifier) {
        return shouldWith(JavaClass.Predicates.resideInPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideInAnyPackage(String... packageIdentifiers) {
        return shouldWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackage(String packageIdentifier) {
        return shouldWith(ClassesThatPredicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public ClassesShouldConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return shouldWith(ClassesThatPredicates.resideOutsideOfPackages(packageIdentifiers));
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
    public ClassesShouldConjunction haveNameMatching(String regex) {
        return shouldWith(have(nameMatching(regex)));
    }

    @Override
    public ClassesShouldConjunction haveNameNotMatching(String regex) {
        return shouldWith(ClassesThatPredicates.haveNameNotMatching(regex));
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
    public ClassesShouldConjunction arePublic() {
        return shouldWith(ClassesThatPredicates.arePublic());
    }

    @Override
    public ClassesShouldConjunction areNotPublic() {
        return shouldWith(ClassesThatPredicates.areNotPublic());
    }

    @Override
    public ClassesShouldConjunction areProtected() {
        return shouldWith(ClassesThatPredicates.areProtected());
    }

    @Override
    public ClassesShouldConjunction areNotProtected() {
        return shouldWith(ClassesThatPredicates.areNotProtected());
    }

    @Override
    public ClassesShouldConjunction arePackagePrivate() {
        return shouldWith(ClassesThatPredicates.arePackagePrivate());
    }

    @Override
    public ClassesShouldConjunction areNotPackagePrivate() {
        return shouldWith(ClassesThatPredicates.areNotPackagePrivate());
    }

    @Override
    public ClassesShouldConjunction arePrivate() {
        return shouldWith(ClassesThatPredicates.arePrivate());
    }

    @Override
    public ClassesShouldConjunction areNotPrivate() {
        return shouldWith(ClassesThatPredicates.areNotPrivate());
    }

    @Override
    public ClassesShouldConjunction areNamed(String name) {
        return shouldWith(ClassesThatPredicates.areNamed(name));
    }

    @Override
    public ClassesShouldConjunction areNotNamed(String name) {
        return shouldWith(ClassesThatPredicates.areNotNamed(name));
    }

    @Override
    public ClassesShouldConjunction haveSimpleName(String name) {
        return shouldWith(ClassesThatPredicates.haveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction dontHaveSimpleName(String name) {
        return shouldWith(ClassesThatPredicates.dontHaveSimpleName(name));
    }

    @Override
    public ClassesShouldConjunction haveModifier(JavaModifier modifier) {
        return shouldWith(ClassesThatPredicates.haveModifier(modifier));
    }

    @Override
    public ClassesShouldConjunction dontHaveModifier(JavaModifier modifier) {
        return shouldWith(ClassesThatPredicates.dontHaveModifier(modifier));
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
    public ArchRule as(String description) {
        return finishedRule.get().as(description);
    }

    private ClassesShouldThatInternal shouldWith(DescribedPredicate<? super JavaClass> predicate) {
        return new ClassesShouldThatInternal(classesShould,
                predicateAggregator.and(predicate),
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
