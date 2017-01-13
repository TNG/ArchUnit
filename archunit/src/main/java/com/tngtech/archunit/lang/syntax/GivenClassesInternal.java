package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

class GivenClassesInternal extends GivenObjectsInternal<JavaClass, GivenClassesInternal> implements GivenClasses {
    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer) {
        this(priority, classesTransformer, Functions.<ArchCondition<JavaClass>>identity());
    }

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer,
                         Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(priority, classesTransformer, prepareCondition);
    }

    private GivenClassesInternal(
            Priority priority,
            ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
            PredicateAggregator<JavaClass> relevantObjectsPredicates) {
        super(priority, classesTransformer, prepareCondition, relevantObjectsPredicates);
    }

    @Override
    public GivenClassesInternal with(DescribedPredicate<JavaClass> predicate) {
        PredicateAggregator<JavaClass> relevantObjectsPredicates = this.relevantObjectsPredicates.and(predicate);
        return new GivenClassesInternal(priority, classesTransformer, prepareCondition, relevantObjectsPredicates);
    }

    @Override
    public ClassesShould should() {
        ClassesTransformer<JavaClass> finishedTransformer = finish(classesTransformer);
        return new ClassesShouldInternal(finishedTransformer, this.priority, prepareCondition);
    }

    private ClassesTransformer<JavaClass> finish(ClassesTransformer<JavaClass> classesTransformer) {
        return relevantObjectsPredicates.isPresent() ?
                classesTransformer.that(relevantObjectsPredicates.get()) :
                classesTransformer;
    }

    @Override
    public GivenClassesThat that() {
        return new GivenClassesThatInternal(this);
    }
}
