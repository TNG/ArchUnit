package com.tngtech.archunit.library.cycle_detection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.AbstractObjectAssert;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("UnusedReturnValue")
class CyclesAssertion extends AbstractObjectAssert<CyclesAssertion, Collection<Cycle<?>>> {

    protected CyclesAssertion(Collection<Cycle<?>> cycles) {
        super(cycles, CyclesAssertion.class);
    }

    static CyclesAssertion assertThatCycles(Iterable<? extends Cycle<?>> cycles) {
        return new CyclesAssertion(ImmutableList.copyOf(cycles));
    }

    CyclesAssertion hasSize(int size) {
        assertThat(actual).as(descriptionText()).hasSize(size);
        return this;
    }

    CyclesAssertion containsOnly(Cycle<?>... cycles) {
        hasSize(cycles.length);

        Set<Set<List<?>>> thisOriginsAndTargets = actual.stream().map(it -> toOriginsAndTargets(it.getEdges())).collect(toSet());
        Set<Set<List<?>>> otherOriginsAndTargets = Arrays.stream(cycles).map(it -> toOriginsAndTargets(it.getEdges())).collect(toSet());
        assertThat(thisOriginsAndTargets).isEqualTo(otherOriginsAndTargets);

        return this;
    }

    private Set<List<?>> toOriginsAndTargets(List<? extends Edge<?>> edges) {
        return edges.stream().map(it -> ImmutableList.of(it.getOrigin(), it.getTarget())).collect(toSet());
    }

    CyclesAssertion isEmpty() {
        assertThat(actual).as(descriptionText()).isEmpty();
        return this;
    }

    CyclesAssertion isNotEmpty() {
        assertThat(actual).as(descriptionText()).isNotEmpty();
        return this;
    }
}
