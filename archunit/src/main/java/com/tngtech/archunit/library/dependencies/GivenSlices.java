package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

import static com.tngtech.archunit.library.dependencies.DependencyRules.beFreeOfCycles;
import static com.tngtech.archunit.library.dependencies.DependencyRules.slicesShouldNotDependOnEachOtherIn;

public class GivenSlices implements GivenObjects<Slice> {
    private Priority priority;
    private final Slices.Transformer classesTransformer;
    private final Optional<String> overriddenDescription;

    GivenSlices(Priority priority, Slices.Transformer classesTransformer) {
        this(priority, classesTransformer, Optional.<String>absent());
    }

    private GivenSlices(Priority priority, Slices.Transformer classesTransformer, Optional<String> overriddenDescription) {
        this.classesTransformer = classesTransformer;
        this.priority = priority;
        this.overriddenDescription = overriddenDescription;
    }

    @Override
    public ArchRule should(ArchCondition<Slice> condition) {
        return ArchRule.Factory.create(finishClassesTransformer(), condition, priority);
    }

    private Slices.Transformer finishClassesTransformer() {
        return overriddenDescription.isPresent() ?
                classesTransformer.as(overriddenDescription.get()) :
                classesTransformer;
    }

    @Override
    public GivenSlices as(String description) {
        return new GivenSlices(priority, classesTransformer, Optional.of(description));
    }

    @Override
    public GivenSlices that(DescribedPredicate<? super Slice> predicate) {
        return new GivenSlices(priority, classesTransformer.that(predicate), overriddenDescription);
    }

    /**
     * @see Slices#namingSlices(String)
     */
    public GivenSlices namingSlices(String pattern) {
        return new GivenSlices(priority, classesTransformer.namingSlices(pattern), overriddenDescription);
    }

    public ArchRule shouldBeFreeOfCycles() {
        return should(beFreeOfCycles());
    }

    public ArchRule shouldNotDependOnEachOther() {
        return slicesShouldNotDependOnEachOtherIn(finishClassesTransformer());
    }
}
