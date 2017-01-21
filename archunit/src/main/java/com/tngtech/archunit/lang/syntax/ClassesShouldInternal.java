package com.tngtech.archunit.lang.syntax;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;
import com.tngtech.archunit.lang.syntax.elements.ShouldConjunction;

class ClassesShouldInternal extends ObjectsShouldInternal<JavaClass>
        implements ClassesShould, ShouldConjunction {

    ClassesShouldInternal(Priority priority,
                          ClassesTransformer<JavaClass> classesTransformer,
                          Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(priority, classesTransformer, ImmutableList.<ArchCondition<JavaClass>>of(), prepareCondition);
    }

    ClassesShouldInternal(Priority priority,
                          ClassesTransformer<JavaClass> classesTransformer,
                          List<ArchCondition<JavaClass>> conditions,
                          Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        super(classesTransformer, priority, conditions, prepareCondition);
    }

    @Override
    public AccessSpecificationInternal access() {
        return new AccessSpecificationInternal(this);
    }

    @Override
    public OnlyBeAccessedSpecification<ShouldConjunction> onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
    }

    @Override
    public ShouldConjunction resideInAPackage(String packageIdentifier) {
        return copyWithCondition(ArchConditions.resideInAPackage(packageIdentifier));
    }

    ClassesShouldInternal copyWithCondition(ArchCondition<JavaClass> condition) {
        ImmutableList<ArchCondition<JavaClass>> newConditions = ImmutableList.<ArchCondition<JavaClass>>builder()
                .addAll(this.conditions).add(condition).build();
        return new ClassesShouldInternal(priority, classesTransformer, newConditions, prepareCondition);
    }
}
