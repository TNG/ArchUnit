package com.tngtech.archunit.testutil.syntax;

import java.util.Collections;

import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsHierarchyImplementationStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsHierarchyImplementationStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsHierarchyImplementationStep3;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsHierarchyImplementationStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsHierarchyImplementationStep5;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsInterfaceChildStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsInterfaceChildStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsInterfaceChildStep3;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsInterfaceChildStep5;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy.FiveStepsInterfaceParentStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsImplementationStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsImplementationStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsImplementationStep3;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsImplementationStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsInterfaceStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsInterfaceStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsInterfaceStep3;
import com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics.FourStepsInterfaceStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation.FourStepsLongUnboundImplementationStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation.FourStepsLongUnboundImplementationStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation.FourStepsLongUnboundInterfaceStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation.FourStepsLongUnboundInterfaceStep4;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsImplementationStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsImplementationStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsImplementationStep3;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsInterfaceStep1;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsInterfaceStep2;
import com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics.ThreeStepsInterfaceStep3;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.syntax.MethodChoiceStrategy.chooseAllArchUnitSyntaxMethods;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MethodCallChainTest {
    @DataProvider
    public static Object[][] callChainTestCases() {
        return testForEach(
                CallChainTestCase
                        .start(ThreeStepsInterfaceStep1.class, new ThreeStepsImplementationStep1())
                        .numberOfInvocations(1)
                        .expect(ThreeStepsInterfaceStep2.class, ThreeStepsImplementationStep2.class),
                CallChainTestCase
                        .start(ThreeStepsInterfaceStep1.class, new ThreeStepsImplementationStep1())
                        .numberOfInvocations(2)
                        .expect(ThreeStepsInterfaceStep3.class, ThreeStepsImplementationStep3.class),

                CallChainTestCase
                        .start(FourStepsInterfaceStep1.class, new FourStepsImplementationStep1())
                        .numberOfInvocations(1)
                        .expect(FourStepsInterfaceStep2.class, FourStepsImplementationStep2.class),
                CallChainTestCase
                        .start(FourStepsInterfaceStep1.class, new FourStepsImplementationStep1())
                        .numberOfInvocations(2)
                        .expect(FourStepsInterfaceStep3.class, FourStepsImplementationStep3.class),
                CallChainTestCase
                        .start(FourStepsInterfaceStep1.class, new FourStepsImplementationStep1())
                        .numberOfInvocations(3)
                        .expect(FourStepsInterfaceStep4.class, FourStepsImplementationStep4.class),

                CallChainTestCase
                        .start(FourStepsLongUnboundInterfaceStep1.class, new FourStepsLongUnboundImplementationStep1())
                        .numberOfInvocations(3)
                        .expect(FourStepsLongUnboundInterfaceStep4.class, FourStepsLongUnboundImplementationStep4.class),

                CallChainTestCase
                        .start(FiveStepsInterfaceChildStep1.class, new FiveStepsHierarchyImplementationStep1())
                        .numberOfInvocations(1)
                        .expect(FiveStepsInterfaceChildStep2.class, FiveStepsHierarchyImplementationStep2.class),
                CallChainTestCase
                        .start(FiveStepsInterfaceChildStep1.class, new FiveStepsHierarchyImplementationStep1())
                        .numberOfInvocations(2)
                        .expect(FiveStepsInterfaceChildStep3.class, FiveStepsHierarchyImplementationStep3.class),
                CallChainTestCase
                        .start(FiveStepsInterfaceChildStep1.class, new FiveStepsHierarchyImplementationStep1())
                        .numberOfInvocations(3)
                        // Fall through to parent, nevertheless four invocations bring us back to the interface subtype
                        .expect(FiveStepsInterfaceParentStep4.class, FiveStepsHierarchyImplementationStep4.class),
                CallChainTestCase
                        .start(FiveStepsInterfaceChildStep1.class, new FiveStepsHierarchyImplementationStep1())
                        .numberOfInvocations(4)
                        .expect(FiveStepsInterfaceChildStep5.class, FiveStepsHierarchyImplementationStep5.class)
        );
    }

    @Test
    @UseDataProvider("callChainTestCases")
    public <T> void run_test_cases(CallChainTestCase<T> testCase) {
        MethodCallChain callChain = createCallChainStart(testCase.startInterface, testCase.startImplementation);

        for (int i = 0; i < testCase.numberOfInvocations; i++) {
            invokeNext(callChain);
        }

        checkResult(callChain.getCurrentValue(), testCase.expectedInterface, testCase.expectedImplementationType);
    }

    private void checkResult(TypedValue currentValue, Class<?> expectedInterface, Class<?> expectedImplementation) {
        assertThat(currentValue.getType().getRawType()).as("Interface type").isEqualTo(expectedInterface);
        assertThat(currentValue.getValue()).as("current value").isInstanceOf(expectedImplementation);
    }

    private <T> MethodCallChain createCallChainStart(Class<T> startInterface, T startImplementation) {
        PropagatedType type = new PropagatedType(startInterface);
        return new MethodCallChain(chooseAllArchUnitSyntaxMethods(), new TypedValue(type, startImplementation));
    }

    private void invokeNext(MethodCallChain callChain) {
        callChain.invokeNextMethodCandidate(new Parameters(Collections.<Parameter>emptyList()));
    }

    private static class CallChainTestCase<T> {
        private final Class<T> startInterface;
        private final T startImplementation;
        private int numberOfInvocations;
        private Class<?> expectedInterface;
        private Class<?> expectedImplementationType;

        CallChainTestCase(Class<T> startInterface, T startImplementation) {

            this.startInterface = startInterface;
            this.startImplementation = startImplementation;
        }

        static <T> CallChainTestCase<T> start(Class<T> startInterface, T startImplementation) {
            return new CallChainTestCase<>(startInterface, startImplementation);
        }

        CallChainTestCase<T> numberOfInvocations(int number) {
            this.numberOfInvocations = number;
            return this;
        }

        <V> CallChainTestCase<T> expect(Class<V> expectedInterface, Class<? extends V> expectedImplementationType) {
            this.expectedInterface = expectedInterface;
            this.expectedImplementationType = expectedImplementationType;
            return this;
        }

        @Override
        public String toString() {
            return String.format("%s{start: [%s, %s], numberOfInvocations: [%d], expectedResult: [%s, %s]}",
                    getClass().getSimpleName(),
                    startInterface.getSimpleName(), startImplementation.getClass().getSimpleName(),
                    numberOfInvocations,
                    expectedInterface.getSimpleName(), expectedImplementationType.getSimpleName());
        }
    }
}
