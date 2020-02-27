package com.tngtech.archunit.library.dependencies;

import com.google.common.base.Splitter;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.CompleteSevenNodesGraphRoot;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.math.IntMath.factorial;
import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SliceRuleTest {
    private static final String completeGraphPackageRoot = CompleteSevenNodesGraphRoot.class.getPackage().getName();
    private static final JavaClasses classesFormingCompleteDependencyGraph = new ClassFileImporter().importPackages(completeGraphPackageRoot);

    @Rule
    public final ArchConfigurationRule configurationRule = new ArchConfigurationRule();

    private final SliceRule slicesShouldBeFreeOfCycles = slices()
            .matching(completeGraphPackageRoot + ".(*)")
            .should().beFreeOfCycles();

    @DataProvider
    public static Object[][] cycle_limits() {
        final int totalNumberOfCycles = getNumberOfCyclesInCompleteGraph(7);
        final int halfOfTotal = totalNumberOfCycles / 2;
        return $$(
                $(new Runnable() {
                    @Override
                    public void run() {
                        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME,
                                String.valueOf(halfOfTotal));
                    }

                    @Override
                    public String toString() {
                        return "limited number of cycles to half of existing number of cycles should report half the cycles";
                    }
                }, halfOfTotal),
                $(new Runnable() {
                    @Override
                    public void run() {
                        // keep default
                    }

                    @Override
                    public String toString() {
                        return "if no limit of cycles is configured should report max. 100 cycles";
                    }
                }, 100),
                $(new Runnable() {
                    @Override
                    public void run() {
                        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME,
                                String.valueOf(totalNumberOfCycles));
                    }

                    @Override
                    public String toString() {
                        return "if the exact amount of existing cycles is configured as limit should report all cycles";
                    }
                }, totalNumberOfCycles),
                $(new Runnable() {
                    @Override
                    public void run() {
                        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME,
                                String.valueOf(2 * totalNumberOfCycles));
                    }

                    @Override
                    public String toString() {
                        return "if more than the amount of existing cycles is configured as limit should report all cycles";
                    }
                }, totalNumberOfCycles)
        );
    }

    @Test
    @UseDataProvider("cycle_limits")
    public void limits_number_of_cycles_to_configured_limit(Runnable configureLimit, int expectedNumberOfReportedCycles) {

        configureLimit.run();
        String violations = slicesShouldBeFreeOfCycles.evaluate(classesFormingCompleteDependencyGraph).getFailureReport().toString();

        int numberOfDetectedCycles = countCyclesInMessage(violations);
        assertThat(numberOfDetectedCycles).as("number of cycles detected").isEqualTo(expectedNumberOfReportedCycles);
    }

    @Test
    public void reports_number_of_violations_if_all_cycles_are_reported() {
        int expectedNumberOfCycles = getNumberOfCyclesInCompleteGraph(7);

        String failureReport = evaluateCompleteGraphCycleFreeWithCycleLimit(expectedNumberOfCycles);

        assertThat(failureReport).as("failure report").contains("(" + expectedNumberOfCycles + " times)");
    }

    @Test
    public void reports_hint_that_cycles_have_been_omitted_if_number_of_cycles_exceed_configured_limit() {
        int expectedNumberOfCycles = getNumberOfCyclesInCompleteGraph(7);
        int expectedLimit = expectedNumberOfCycles / 2;

        String failureReport = evaluateCompleteGraphCycleFreeWithCycleLimit(expectedLimit);

        assertThat(failureReport).as("failure report").contains("( >= " + expectedLimit + " times - "
                + "the maximum number of cycles to detect has been reached; "
                + "this limit can be adapted using the `archunit.properties` value `cycles.maxNumberToDetect=xxx`)");
    }

    private String evaluateCompleteGraphCycleFreeWithCycleLimit(int expectedNumberOfCycles) {
        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME, String.valueOf(expectedNumberOfCycles));

        return slicesShouldBeFreeOfCycles.evaluate(classesFormingCompleteDependencyGraph).getFailureReport().toString();
    }

    static int countCyclesInMessage(String text) {
        return Splitter.on("Cycle detected: ").splitToList(text).size() - 1;
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
    static int getNumberOfCyclesInCompleteGraph(int numberOfNodes) {
        int numberOfCycles = 0;
        for (int k = 2; k <= numberOfNodes; k++) {
            numberOfCycles += factorial(numberOfNodes) / (factorial(numberOfNodes - k) * k);
        }
        return numberOfCycles;
    }
}
