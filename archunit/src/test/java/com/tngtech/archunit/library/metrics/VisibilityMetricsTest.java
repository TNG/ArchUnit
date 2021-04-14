package com.tngtech.archunit.library.metrics;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.metrics.testobjects.visibility.one.VisibleOne;
import com.tngtech.archunit.library.metrics.testobjects.visibility.two.VisibleTwo;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

public class VisibilityMetricsTest {

    @Test
    public void relative_visibility_with_single_element_reflects_element_visibility() {
        MetricsComponent<TestElement> visible = MetricsComponent.of("visible", new TestElement("isVisible"));
        MetricsComponent<TestElement> invisible = MetricsComponent.of("invisible", new TestElement("isNotVisible"));

        VisibilityMetrics metrics = ArchitectureMetrics.visibilityMetrics(MetricsComponents.of(visible, invisible), visibleIfNameContainsIsVisible);

        assertThat(metrics.getRelativeVisibility(visible.getIdentifier())).isEqualTo(1.0);
        assertThat(metrics.getRelativeVisibility(invisible.getIdentifier())).isEqualTo(0.0);
    }

    @Test
    public void relative_visibility_of_mixed_components_is_calculated_as_relation_of_visible_elements_to_all_elements() {
        MetricsComponent<TestElement> oneThird = componentWithVisibilityPercentage(33);
        MetricsComponent<TestElement> twoThirds = componentWithVisibilityPercentage(66);

        VisibilityMetrics metrics = ArchitectureMetrics.visibilityMetrics(MetricsComponents.of(oneThird, twoThirds), visibleIfNameContainsIsVisible);

        assertThat(metrics.getRelativeVisibility(oneThird.getIdentifier())).isCloseTo(0.33, offset(0.01));
        assertThat(metrics.getRelativeVisibility(twoThirds.getIdentifier())).isCloseTo(0.66, offset(0.01));
    }

    @Test
    public void average_relative_visibility_is_calculated_as_the_average_of_all_relative_visibilities() {
        MetricsComponent<TestElement> oneThird = componentWithVisibilityPercentage(33);
        MetricsComponent<TestElement> half = componentWithVisibilityPercentage(50);
        MetricsComponent<TestElement> twoThirds = componentWithVisibilityPercentage(66);
        MetricsComponent<TestElement> full = componentWithVisibilityPercentage(100);

        VisibilityMetrics metrics = ArchitectureMetrics.visibilityMetrics(MetricsComponents.of(oneThird, half, twoThirds, full), visibleIfNameContainsIsVisible);

        assertThat(metrics.getAverageRelativeVisibility()).isCloseTo(0.62, offset(0.01));
    }

    @Test
    public void global_relative_visibility_is_calculated_as_relation_of_visible_elements_to_all_elements_across_all_components() {
        MetricsComponent<TestElement> first = componentWithVisibilityDistribution(1, 3);
        MetricsComponent<TestElement> second = componentWithVisibilityDistribution(900, 1);
        MetricsComponent<TestElement> third = componentWithVisibilityDistribution(1, 67);

        VisibilityMetrics metrics = ArchitectureMetrics.visibilityMetrics(MetricsComponents.of(first, second, third), visibleIfNameContainsIsVisible);

        assertThat(metrics.getGlobalRelativeVisibility()).isCloseTo(0.93, offset(0.01));
    }

    @Test
    public void calculates_visibility_metrics_for_Java_classes_according_to_their_modifier() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(VisibleOne.class, VisibleTwo.class);
        JavaPackage packageOne = classes.getPackage(VisibleOne.class.getPackage().getName());
        JavaPackage packageTwo = classes.getPackage(VisibleTwo.class.getPackage().getName());

        VisibilityMetrics metrics = ArchitectureMetrics.visibilityMetrics(MetricsComponents.fromPackages(ImmutableSet.of(packageOne, packageTwo)));

        assertThat(metrics.getRelativeVisibility(packageOne.getName())).isEqualTo(0.5);
        assertThat(metrics.getRelativeVisibility(packageTwo.getName())).isEqualTo(1.0);
        assertThat(metrics.getAverageRelativeVisibility()).isEqualTo(0.75);
        assertThat(metrics.getGlobalRelativeVisibility()).isCloseTo(0.66, offset(0.01));
    }

    private MetricsComponent<TestElement> componentWithVisibilityPercentage(int percentage) {
        checkArgument(percentage >= 0 && percentage <= 100);
        return componentWithVisibilityDistribution(percentage, 100 - percentage);
    }

    private MetricsComponent<TestElement> componentWithVisibilityDistribution(int numberOfVisibleElements, int numberOfInvisibleElements) {
        Set<TestElement> elements = new HashSet<>();
        for (int i = 0; i < numberOfVisibleElements; i++) {
            elements.add(new TestElement("isVisible"));
        }
        for (int i = 0; i < numberOfInvisibleElements; i++) {
            elements.add(new TestElement("isNotVisible"));
        }

        return MetricsComponent.of("Visible(" + numberOfVisibleElements + ")/Invisible(" + numberOfInvisibleElements + ")", elements);
    }

    private static final Predicate<TestElement> visibleIfNameContainsIsVisible = new Predicate<TestElement>() {
        @Override
        public boolean apply(TestElement input) {
            return input.getName().contains("isVisible");
        }
    };
}
