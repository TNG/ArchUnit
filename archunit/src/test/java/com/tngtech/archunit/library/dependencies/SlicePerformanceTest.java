package com.tngtech.archunit.library.dependencies;

import com.google.common.base.Splitter;
import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.CompleteGraphRoot;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.google.common.math.IntMath.factorial;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

@Category(Slow.class)
public class SlicePerformanceTest {
    private static final String completeGraphPackageRoot = CompleteGraphRoot.class.getPackage().getName();
    private static final JavaClasses classesFormingCompleteDependencyGraph =
            new ClassFileImporter().importPackages(completeGraphPackageRoot);
    private static final int numberOfClassesFormingCompleteGraph = classesFormingCompleteDependencyGraph.size() - 1;

    /**
     * This test would not terminate within a reasonable time with the old BFS cycle detection
     */
    @Test
    public void searching_for_cycles_should_terminate_reasonably_fast_for_complete_graph() {
        SliceRule cycleFree = slices()
                .matching(completeGraphPackageRoot + ".(*)")
                .should().beFreeOfCycles();

        String violations = cycleFree.evaluate(classesFormingCompleteDependencyGraph).getFailureReport().toString();
        int numberOfDetectedCycles = countOccurrences(violations, "Cycle detected: ");
        int expectedNumberOfCycles = getNumberOfCyclesInCompleteGraph(numberOfClassesFormingCompleteGraph);
        assertThat(numberOfDetectedCycles).as("number of cycles detected").isEqualTo(expectedNumberOfCycles);
    }

    private int countOccurrences(String text, String toFind) {
        return Splitter.on(toFind).splitToList(text).size() - 1;
    }

    /**
     * To calculate the number of expected cycles check the following (we denote binomial coefficients "n over k" as (n k) ):
     * For any k-cycle (cycle of k nodes within a graph of n nodes) we can denote
     * n1 -> n2 -> n3 -> ... -> n[k-1] -> n[k] -> n1
     * Fix n1 and we have (k-1)! possibilities to order the other k-1 nodes.
     * Now we have (n k) possibilities to choose k nodes to form a k-cycle and for each choice (k-1)! permutations.
     * Thus we retrieve the sum of all cycles as
     * SUM(k=2, n) (n k) * (k-1)! = SUM(k=2, n) n! / ((n-k)! * k)
     */
    private int getNumberOfCyclesInCompleteGraph(int numberOfNodes) {
        int numberOfCycles = 0;
        for (int k = 2; k <= numberOfNodes; k++) {
            numberOfCycles += factorial(numberOfNodes) / (factorial(numberOfNodes - k) * k);
        }
        return numberOfCycles;
    }
}
