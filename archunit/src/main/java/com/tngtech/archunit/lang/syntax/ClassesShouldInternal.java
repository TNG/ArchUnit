package com.tngtech.archunit.lang.syntax;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

class ClassesShouldInternal extends ObjectsShouldInternal<JavaClass> implements ClassesShould {

    ClassesShouldInternal(ClassesShouldInternal copy) {
        this(copy.priority, copy.classesTransformer, copy.conditions, copy.prepareCondition);
    }

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
    public OnlyBeAccessedSpecification onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
    }

    ClassesShouldInternal copyWithCondition(ArchCondition<JavaClass> condition) {
        ImmutableList<ArchCondition<JavaClass>> newConditions = ImmutableList.<ArchCondition<JavaClass>>builder()
                .addAll(this.conditions).add(condition).build();
        return new ClassesShouldInternal(priority, classesTransformer, newConditions, prepareCondition);
    }
}
