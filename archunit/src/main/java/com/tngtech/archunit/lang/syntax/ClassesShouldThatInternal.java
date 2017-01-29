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
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;
import com.tngtech.archunit.lang.syntax.elements.ShouldConjunction;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class ClassesShouldThatInternal implements ClassesShouldThat, ShouldConjunction {
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
    public ShouldConjunction resideInPackage(String packageIdentifier) {
        return shouldWith(JavaClass.Predicates.resideInPackage(packageIdentifier));
    }

    @Override
    public ShouldConjunction resideInAnyPackage(String... packageIdentifiers) {
        return shouldWith(JavaClass.Predicates.resideInAnyPackage(packageIdentifiers));
    }

    @Override
    public ShouldConjunction resideOutsideOfPackage(String packageIdentifier) {
        return shouldWith(ClassesThatPredicates.resideOutsideOfPackage(packageIdentifier));
    }

    @Override
    public ShouldConjunction resideOutsideOfPackages(String... packageIdentifiers) {
        return shouldWith(ClassesThatPredicates.resideOutsideOfPackages(packageIdentifiers));
    }

    @Override
    public ShouldConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(annotatedWith(annotationType)));
    }

    @Override
    public ShouldConjunction areNotAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(not(annotatedWith(annotationType))));
    }

    @Override
    public ShouldConjunction areAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(annotatedWith(annotationTypeName)));
    }

    @Override
    public ShouldConjunction areNotAnnotatedWith(String annotationTypeName) {
        return shouldWith(are(not(annotatedWith(annotationTypeName))));
    }

    @Override
    public ShouldConjunction areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(annotatedWith(predicate)));
    }

    @Override
    public ShouldConjunction areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate) {
        return shouldWith(are(not(annotatedWith(predicate))));
    }

    @Override
    public ShouldConjunction haveNameMatching(String regex) {
        return shouldWith(have(nameMatching(regex)));
    }

    @Override
    public ShouldConjunction haveNameNotMatching(String regex) {
        return shouldWith(ClassesThatPredicates.haveNameNotMatching(regex));
    }

    @Override
    public ShouldConjunction areAssignableTo(Class<?> type) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(type)));
    }

    @Override
    public ShouldConjunction areNotAssignableTo(Class<?> type) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(type))));
    }

    @Override
    public ShouldConjunction areAssignableTo(String typeName) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(typeName)));
    }

    @Override
    public ShouldConjunction areNotAssignableTo(String typeName) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(typeName))));
    }

    @Override
    public ShouldConjunction areAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(JavaClass.Predicates.assignableTo(predicate)));
    }

    @Override
    public ShouldConjunction areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(not(JavaClass.Predicates.assignableTo(predicate))));
    }

    @Override
    public ShouldConjunction areAssignableFrom(Class<?> type) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(type)));
    }

    @Override
    public ShouldConjunction areNotAssignableFrom(Class<?> type) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(type))));
    }

    @Override
    public ShouldConjunction areAssignableFrom(String typeName) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(typeName)));
    }

    @Override
    public ShouldConjunction areNotAssignableFrom(String typeName) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(typeName))));
    }

    @Override
    public ShouldConjunction areAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(JavaClass.Predicates.assignableFrom(predicate)));
    }

    @Override
    public ShouldConjunction areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate) {
        return shouldWith(are(not(JavaClass.Predicates.assignableFrom(predicate))));
    }

    @Override
    public ShouldConjunction arePublic() {
        return shouldWith(ClassesThatPredicates.arePublic());
    }

    @Override
    public ShouldConjunction areNotPublic() {
        return shouldWith(ClassesThatPredicates.areNotPublic());
    }

    @Override
    public ShouldConjunction areProtected() {
        return shouldWith(ClassesThatPredicates.areProtected());
    }

    @Override
    public ShouldConjunction areNotProtected() {
        return shouldWith(ClassesThatPredicates.areNotProtected());
    }

    @Override
    public ShouldConjunction arePackagePrivate() {
        return shouldWith(ClassesThatPredicates.arePackagePrivate());
    }

    @Override
    public ShouldConjunction areNotPackagePrivate() {
        return shouldWith(ClassesThatPredicates.areNotPackagePrivate());
    }

    @Override
    public ShouldConjunction arePrivate() {
        return shouldWith(ClassesThatPredicates.arePrivate());
    }

    @Override
    public ShouldConjunction areNotPrivate() {
        return shouldWith(ClassesThatPredicates.areNotPrivate());
    }

    @Override
    public ShouldConjunction areNamed(String name) {
        return shouldWith(ClassesThatPredicates.areNamed(name));
    }

    @Override
    public ShouldConjunction areNotNamed(String name) {
        return shouldWith(ClassesThatPredicates.areNotNamed(name));
    }

    @Override
    public ShouldConjunction haveSimpleName(String name) {
        return shouldWith(ClassesThatPredicates.haveSimpleName(name));
    }

    @Override
    public ShouldConjunction dontHaveSimpleName(String name) {
        return shouldWith(ClassesThatPredicates.dontHaveSimpleName(name));
    }

    @Override
    public ShouldConjunction haveModifier(JavaModifier modifier) {
        return shouldWith(ClassesThatPredicates.haveModifier(modifier));
    }

    @Override
    public ShouldConjunction dontHaveModifier(JavaModifier modifier) {
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

    private class FinishedRule implements Supplier<ArchRule> {
        @Override
        public ArchRule get() {
            return classesShould.copyWithCondition(createCondition.apply(predicateAggregator.get()));
        }
    }
}
