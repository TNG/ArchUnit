package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

class ClassesShouldInternal extends ObjectsShouldInternal<JavaClass>
        implements ClassesShould, ClassesShouldConjunction {

    ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
                          Priority priority,
                          Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, prepareCondition);
    }

    ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
                          Priority priority,
                          ArchCondition<JavaClass> condition,
                          Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, condition, prepareCondition);
    }

    @Override
    public AccessSpecificationInternal access() {
        return new AccessSpecificationInternal(this);
    }

    @Override
    public OnlyBeAccessedSpecification<ClassesShouldConjunction> onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
    }

    @Override
    public ClassesShouldConjunction beNamed(final String name) {
        return copyWithNewCondition(conditionAggregator.and(ArchConditions.beNamed(name)));
    }

    @Override
    public ClassesShouldConjunction resideInAPackage(String packageIdentifier) {
        return copyWithNewCondition(conditionAggregator.and(ArchConditions.resideInAPackage(packageIdentifier)));
    }

    @Override
    public ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return copyWithNewCondition(conditionAggregator.and(ArchConditions.setFieldWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate) {
        return copyWithNewCondition(conditionAggregator.and(ArchConditions.callMethodWhere(predicate)));
    }

    ClassesShouldInternal copyWithNewCondition(ArchCondition<JavaClass> newCondition) {
        return new ClassesShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }

    @Override
    public ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator.or(condition.as("should " + condition.getDescription())));
    }
}
