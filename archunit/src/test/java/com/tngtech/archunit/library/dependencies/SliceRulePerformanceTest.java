package com.tngtech.archunit.library.dependencies;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.ninenodes.CompleteNineNodesGraphRoot;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.dependencies.SliceRuleTest.getNumberOfCyclesInCompleteGraph;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

@Category(Slow.class)
public class SliceRulePerformanceTest {
    private static final String completeGraphPackageRoot = CompleteNineNodesGraphRoot.class.getPackage().getName();
    private static final JavaClasses classesFormingCompleteDependencyGraph =
            new ClassFileImporter().importPackages(completeGraphPackageRoot);
    private static final int numberOfClassesFormingCompleteGraph = classesFormingCompleteDependencyGraph.size() - 1;

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    /**
     * This test would not terminate within a reasonable time with the old BFS cycle detection
     */
    @Test
    public void searching_for_cycles_should_terminate_reasonably_fast_for_complete_graph() {
        SliceRule cycleFree = slices()
                .matching(completeGraphPackageRoot + ".(*)")
                .should().beFreeOfCycles();
        int expectedNumberOfCycles = getNumberOfCyclesInCompleteGraph(numberOfClassesFormingCompleteGraph);
        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME, String.valueOf(2 * expectedNumberOfCycles));

        FailureReport failureReport = cycleFree.evaluate(classesFormingCompleteDependencyGraph).getFailureReport();

        int numberOfDetectedCycles = FluentIterable.from(failureReport.getDetails()).filter(new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.contains("Cycle detected: ");
            }
        }).size();
        assertThat(numberOfDetectedCycles).as("number of cycles detected").isEqualTo(expectedNumberOfCycles);
    }
}
