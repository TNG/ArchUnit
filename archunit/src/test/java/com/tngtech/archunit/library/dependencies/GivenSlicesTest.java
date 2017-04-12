package com.tngtech.archunit.library.dependencies;

import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

public class GivenSlicesTest {
    @Test
    public void chosen_slices() {
        Set<Slice> slices = getSlicesMatchedByFilter(
                testSlices().that(have(descriptionMatching(".*first.*"))));

        assertThat(slices).extracting("description").containsOnly("Slice first");

        slices = getSlicesMatchedByFilter(
                testSlices()
                        .that(have(descriptionMatching(".*first.*")))
                        .or(have(descriptionMatching(".*second.*"))));

        assertThat(slices).extracting("description").containsOnly("Slice first", "Slice second");

        slices = getSlicesMatchedByFilter(
                testSlices()
                        .that(have(descriptionMatching(".*")))
                        .and(have(descriptionMatching("nothing")))
                        .or(have(descriptionMatching(".*ir.*"))));

        assertThat(slices).extracting("description").containsOnly("Slice first", "Slice third");
    }

    private GivenSlices testSlices() {
        return slices().matching(getClass().getPackage().getName() + ".testclasses.(*)..");
    }

    private DescribedPredicate<Slice> descriptionMatching(final String regex) {
        return new DescribedPredicate<Slice>("description matching '%s'", regex) {
            @Override
            public boolean apply(Slice input) {
                return input.getDescription().matches(regex);
            }
        };
    }

    private Set<Slice> getSlicesMatchedByFilter(GivenConjunction<Slice> givenSlices) {
        final Set<Slice> matched = new HashSet<>();
        givenSlices.should(new ArchCondition<Slice>("") {
            @Override
            public void check(Slice item, ConditionEvents events) {
                matched.add(item);
            }
        }).evaluate(new ClassFileImporter().importPackages(getClass().getPackage().getName() + ".testclasses"));
        return matched;
    }
}