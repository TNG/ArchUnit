package com.tngtech.archunit.library.metrics;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.metrics.testobjects.componentdependency.simple.SimpleWithoutDependencies;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

public class ComponentDependencyMetricsTest {

    @Test
    public void component_dependency_metrics_of_an_isolated_component() {
        JavaPackage javaPackage = new ClassFileImporter().importPackagesOf(SimpleWithoutDependencies.class).get(SimpleWithoutDependencies.class).getPackage();
        MetricsComponents<JavaClass> components = MetricsComponents.fromPackages(singleton(javaPackage));

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getEfferentCoupling(javaPackage.getName()))
                .as("Efferent Coupling of " + javaPackage.getName()).isEqualTo(0);
        assertThat(metrics.getAfferentCoupling(javaPackage.getName()))
                .as("Afferent Coupling of " + javaPackage.getName()).isEqualTo(0);
        assertThat(metrics.getInstability(javaPackage.getName()))
                .as("Instability of " + javaPackage.getName()).isEqualTo(1);
        assertThat(metrics.getAbstractness(javaPackage.getName()))
                .as("Abstractness of " + javaPackage.getName()).isEqualTo(0);
        assertThat(metrics.getNormalizedDistanceFromMainSequence(javaPackage.getName()))
                .as("Normalized Distance from Main Sequence of " + javaPackage.getName()).isEqualTo(0);
    }

    @Test
    public void efferent_coupling_is_calculated_by_number_of_outgoing_dependencies_to_other_components() {
        MetricsComponents<JavaClass> components = importTestGraph();

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getEfferentCoupling(testComponentName("fullyconcrete")))
                .as("Efferent Coupling of fullyconcrete").isEqualTo(0);
        assertThat(metrics.getEfferentCoupling(testComponentName("fullyabstract")))
                .as("Efferent Coupling of fullyabstract").isEqualTo(1);
        assertThat(metrics.getEfferentCoupling(testComponentName("halfabstract")))
                .as("Efferent Coupling of halfabstract").isEqualTo(1);
        assertThat(metrics.getEfferentCoupling(testComponentName("otherconcrete1")))
                .as("Efferent Coupling of otherconcrete1").isEqualTo(2);
        assertThat(metrics.getEfferentCoupling(testComponentName("otherconcrete2")))
                .as("Efferent Coupling of otherconcrete2").isEqualTo(2);
    }

    @Test
    public void afferent_coupling_is_calculated_by_number_of_incoming_dependencies_from_other_components() {
        MetricsComponents<JavaClass> components = importTestGraph();

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getAfferentCoupling(testComponentName("fullyconcrete")))
                .as("Afferent Coupling of fullyconcrete").isEqualTo(3);
        assertThat(metrics.getAfferentCoupling(testComponentName("fullyabstract")))
                .as("Afferent Coupling of fullyabstract").isEqualTo(2);
        assertThat(metrics.getAfferentCoupling(testComponentName("halfabstract")))
                .as("Afferent Coupling of halfabstract").isEqualTo(0);
        assertThat(metrics.getAfferentCoupling(testComponentName("otherconcrete1")))
                .as("Afferent Coupling of otherconcrete1").isEqualTo(0);
        assertThat(metrics.getAfferentCoupling(testComponentName("otherconcrete2")))
                .as("Afferent Coupling of otherconcrete2").isEqualTo(1);
    }

    @Test
    public void instability_is_calculated_as_the_relation_of_efferent_to_total_coupling() {
        MetricsComponents<JavaClass> components = importTestGraph();

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getInstability(testComponentName("fullyconcrete")))
                .as("Instability of fullyconcrete").isEqualTo(0);
        assertThat(metrics.getInstability(testComponentName("fullyabstract")))
                .as("Instability of fullyabstract").isCloseTo(1 / 3.0, offset(0.01));
        assertThat(metrics.getInstability(testComponentName("halfabstract")))
                .as("Instability of halfabstract").isEqualTo(1);
        assertThat(metrics.getInstability(testComponentName("otherconcrete1")))
                .as("Instability of otherconcrete1").isEqualTo(1);
        assertThat(metrics.getInstability(testComponentName("otherconcrete2")))
                .as("Instability of otherconcrete2").isCloseTo(2 / 3.0, offset(0.01));
    }

    @Test
    public void abstractness_is_calculated_as_the_relation_of_public_abstract_classes_to_total_number_of_public_classes() {
        MetricsComponents<JavaClass> components = importTestGraph();

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getAbstractness(testComponentName("fullyconcrete")))
                .as("Abstractness of fullyconcrete").isEqualTo(0);
        assertThat(metrics.getAbstractness(testComponentName("fullyabstract")))
                .as("Abstractness of fullyabstract").isEqualTo(1);
        assertThat(metrics.getAbstractness(testComponentName("halfabstract")))
                .as("Abstractness of halfabstract").isCloseTo(1 / 2.0, offset(0.01));
        assertThat(metrics.getAbstractness(testComponentName("otherconcrete1")))
                .as("Abstractness of otherconcrete1").isEqualTo(0);
        assertThat(metrics.getAbstractness(testComponentName("otherconcrete2")))
                .as("Abstractness of otherconcrete2").isEqualTo(0);
    }

    @Test
    public void normalized_distance_from_main_sequence_is_calculated_as_distance_from_the_ideal_relation_of_abstractness_and_instability() {
        MetricsComponents<JavaClass> components = importTestGraph();

        ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(components);

        assertThat(metrics.getNormalizedDistanceFromMainSequence(testComponentName("fullyconcrete")))
                .as("Normalized Distance from Main Sequence of fullyconcrete").isEqualTo(1);
        assertThat(metrics.getNormalizedDistanceFromMainSequence(testComponentName("fullyabstract")))
                .as("Normalized Distance from Main Sequence of fullyabstract").isCloseTo(1 / 3.0, offset(0.01));
        assertThat(metrics.getNormalizedDistanceFromMainSequence(testComponentName("halfabstract")))
                .as("Normalized Distance from Main Sequence of halfabstract").isCloseTo(1 / 2.0, offset(0.01));
        assertThat(metrics.getNormalizedDistanceFromMainSequence(testComponentName("otherconcrete1")))
                .as("Normalized Distance from Main Sequence of otherconcrete1").isEqualTo(0);
        assertThat(metrics.getNormalizedDistanceFromMainSequence(testComponentName("otherconcrete2")))
                .as("Normalized Distance from Main Sequence of otherconcrete2").isCloseTo(1 / 3.0, offset(0.01));
    }

    @Test
    public void rejects_requesting_metrics_of_unknown_component() {
        final ComponentDependencyMetrics metrics = ArchitectureMetrics.componentDependencyMetrics(MetricsComponents.<JavaClass>of());

        List<ThrowingCallable> callables = ImmutableList.of(
                new ThrowingCallable() {
                    @Override
                    public void call() {
                        metrics.getEfferentCoupling("unknown");
                    }
                }, new ThrowingCallable() {
                    @Override
                    public void call() {
                        metrics.getAfferentCoupling("unknown");
                    }
                }, new ThrowingCallable() {
                    @Override
                    public void call() {
                        metrics.getInstability("unknown");
                    }
                }, new ThrowingCallable() {
                    @Override
                    public void call() {
                        metrics.getAbstractness("unknown");
                    }
                }, new ThrowingCallable() {
                    @Override
                    public void call() {
                        metrics.getNormalizedDistanceFromMainSequence("unknown");
                    }
                });
        for (ThrowingCallable callable : callables) {
            assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown component with identifier 'unknown'");
        }
    }

    private String testComponentName(String relativeName) {
        return testGraphPackageName() + "." + relativeName;
    }

    private MetricsComponents<JavaClass> importTestGraph() {
        String graphPackage = testGraphPackageName();
        Set<JavaPackage> packages = new ClassFileImporter().importPackages(graphPackage).getPackage(graphPackage).getSubpackages();
        return MetricsComponents.fromPackages(packages);
    }

    private String testGraphPackageName() {
        return getClass().getPackage().getName() + ".testobjects.componentdependency.graph";
    }
}
