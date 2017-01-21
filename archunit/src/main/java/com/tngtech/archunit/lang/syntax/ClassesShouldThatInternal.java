package com.tngtech.archunit.lang.syntax;

import java.lang.annotation.Annotation;

import com.google.common.base.Supplier;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldThat;
import com.tngtech.archunit.lang.syntax.elements.ShouldConjunction;

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
    public ShouldConjunction areAnnotatedWith(Class<? extends Annotation> annotationType) {
        return shouldWith(are(annotatedWith(annotationType)));
    }

    @Override
    public ShouldConjunction haveNameMatching(String regex) {
        return shouldWith(have(nameMatching(regex)));
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
