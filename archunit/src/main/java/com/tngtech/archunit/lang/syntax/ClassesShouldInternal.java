package com.tngtech.archunit.lang.syntax;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.OnlyBeAccessedSpecification;

import static com.google.common.base.Preconditions.checkState;

class ClassesShouldInternal implements ClassesShould {
    private final Supplier<ArchRule> finishedRule = Suppliers.memoize(new FinishedRule());

    private final List<ArchCondition<JavaClass>> conditions;
    private final ClassesTransformer<JavaClass> classesTransformer;
    private final Priority priority;
    private final Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition;

    ClassesShouldInternal(ClassesShouldInternal copy) {
        this(copy.classesTransformer, copy.priority, copy.conditions, copy.prepareCondition);
    }

    ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer,
                          Priority priority,
                          Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(classesTransformer, priority, ImmutableList.<ArchCondition<JavaClass>>of(), prepareCondition);
    }

    private ClassesShouldInternal(ClassesTransformer<JavaClass> classesTransformer, Priority priority, List<ArchCondition<JavaClass>> conditions,
                                  Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this.conditions = conditions;
        this.classesTransformer = classesTransformer;
        this.priority = priority;
        this.prepareCondition = prepareCondition;
    }

    @Override
    public AccessSpecificationInternal access() {
        return new AccessSpecificationInternal(this);
    }

    @Override
    public OnlyBeAccessedSpecification onlyBeAccessed() {
        return new OnlyBeAccessedSpecificationInternal(this);
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

    ClassesShouldInternal copyWithCondition(ArchCondition<JavaClass> condition) {
        ImmutableList<ArchCondition<JavaClass>> newConditions = ImmutableList.<ArchCondition<JavaClass>>builder()
                .addAll(this.conditions).add(condition).build();
        return new ClassesShouldInternal(classesTransformer, priority, newConditions, prepareCondition);
    }

    private class FinishedRule implements Supplier<ArchRule> {
        @Override
        public ArchRule get() {
            return ArchRule.Factory.create(classesTransformer, createCondition(), priority);
        }

        private ArchCondition<JavaClass> createCondition() {
            checkState(conditions.size() >= 1,
                    "No condition was specified for this rule, so this rule would always be satisfied");
            LinkedList<ArchCondition<JavaClass>> combine = new LinkedList<>(conditions);
            ArchCondition<JavaClass> result = combine.pollFirst();
            for (ArchCondition<JavaClass> condition : combine) {
                result = result.and(condition);
            }
            return prepareCondition.apply(result);
        }
    }
}
