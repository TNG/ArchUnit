package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.library.dependencies.DependencyRules.beFreeOfCycles;
import static com.tngtech.archunit.library.dependencies.DependencyRules.slicesShouldNotDependOnEachOtherIn;

public final class GivenSlices implements GivenObjects<Slice>, GivenConjunction<Slice>, CanOverrideDescription<GivenSlices> {
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

    @Override
    public GivenSlices and(DescribedPredicate<? super Slice> predicate) {
        return that(predicate);
    }

    private Slices.Transformer finishClassesTransformer() {
        return overriddenDescription.isPresent() ?
                classesTransformer.as(overriddenDescription.get()) :
                classesTransformer;
    }

    @Override
    public GivenSlices that(DescribedPredicate<? super Slice> predicate) {
        return new GivenSlices(priority, classesTransformer.that(predicate), overriddenDescription);
    }

    @Override
    public GivenSlices as(String newDescription) {
        return new GivenSlices(priority, classesTransformer, Optional.of(newDescription));
    }

    /**
     * @see Slices#namingSlices(String)
     */
    @PublicAPI(usage = ACCESS)
    public GivenSlices namingSlices(String pattern) {
        return new GivenSlices(priority, classesTransformer.namingSlices(pattern), overriddenDescription);
    }

    @PublicAPI(usage = ACCESS)
    public ArchRule shouldBeFreeOfCycles() {
        return should(beFreeOfCycles());
    }

    @PublicAPI(usage = ACCESS)
    public ArchRule shouldNotDependOnEachOther() {
        return slicesShouldNotDependOnEachOtherIn(finishClassesTransformer());
    }
}
