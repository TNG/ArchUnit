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

    private ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
                                  Priority priority,
                                  ConditionAggregator<JavaClass> conditionAggregator,
                                  Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, conditionAggregator, prepareCondition);
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
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.beNamed(name)));
    }

    @Override
    public ClassesShouldConjunction resideInAPackage(String packageIdentifier) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.resideInAPackage(packageIdentifier)));
    }

    @Override
    public ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.setFieldWhere(predicate)));
    }

    @Override
    public ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate) {
        return copyWithNewCondition(conditionAggregator.add(ArchConditions.callMethodWhere(predicate)));
    }

    ClassesShouldInternal copyWithNewCondition(ArchCondition<JavaClass> newCondition) {
        return new ClassesShouldInternal(classesTransformer, priority, newCondition, prepareCondition);
    }

    ClassesShouldInternal addCondition(ArchCondition<JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator.add(condition));
    }

    @Override
    public ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatANDsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should"))
                .add(condition));
    }

    @Override
    public ClassesShould andShould() {
        return new ClassesShouldInternal(
                classesTransformer,
                priority,
                conditionAggregator.thatANDsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should")),
                prepareCondition);
    }

    @Override
    public ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition) {
        return copyWithNewCondition(conditionAggregator
                .thatORsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should"))
                .add(condition));
    }

    @Override
    public ClassesShould orShould() {
        return new ClassesShouldInternal(
                classesTransformer,
                priority,
                conditionAggregator.thatORsWith(ObjectsShouldInternal.<JavaClass>prependDescription("should")),
                prepareCondition);
    }
}
