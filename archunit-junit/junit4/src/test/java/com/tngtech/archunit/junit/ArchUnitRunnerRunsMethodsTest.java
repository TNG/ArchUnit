package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIgnoredMethod.toBeIgnored;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIllegalTestMethods.noParams;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIllegalTestMethods.tooManyParams;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithTestMethod.testSomething;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.IgnoredArchTest.toBeIgnoredOne;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.IgnoredArchTest.toBeIgnoredTwo;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.getRule;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.newRunnerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsMethodsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SharedCache cache;
    @Mock
    private ClassCache classCache;

    @Mock
    private RunNotifier runNotifier;

    @Captor
    private ArgumentCaptor<Description> descriptionCaptor;

    @Captor
    private ArgumentCaptor<Failure> failureCaptor;

    @InjectMocks
    private ArchUnitRunner runner = newRunnerFor(ArchTestWithTestMethod.class);

    private JavaClasses cachedClasses = importClassesWithContext(ArchUnitRunnerRunsMethodsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyzeFor(any(Class.class))).thenReturn(cachedClasses);
    }

    @Test
    public void executes_test_methods_and_supplies_JavaClasses() throws InitializationError {
        runner.runChild(getRule(testSomething, runner), runNotifier);
        verify(runNotifier, never()).fireTestFailure(any(Failure.class));
        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(testSomething);
        assertThat(ArchTestWithTestMethod.suppliedClasses).as("Supplied Classes").isEqualTo(cachedClasses);
    }

    @Test
    public void fails_methods_with_no_parameters() throws InitializationError {
        runAndAssertWrongParametersForChild(noParams, newRunner(ArchTestWithIllegalTestMethods.class));
    }

    private ArchUnitRunner newRunner(Class<ArchTestWithIllegalTestMethods> testClass) throws InitializationError {
        return newRunnerFor(testClass, cache);
    }

    @Test
    public void fails_methods_with_too_many_parameters() throws InitializationError {
        runAndAssertWrongParametersForChild(tooManyParams, newRunner(ArchTestWithIllegalTestMethods.class));
    }

    private void runAndAssertWrongParametersForChild(String name, ArchUnitRunner runner) {
        runner.runChild(getRule(name, runner), runNotifier);
        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        Failure failure = failureCaptor.getValue();
        assertThat(failure.getDescription().toString()).as("Failure description").contains(name);
        assertThat(failure.getException().getMessage()).as("Failure Cause")
                .contains("@" + ArchTest.class.getSimpleName())
                .contains("exactly one parameter of type " + JavaClasses.class.getSimpleName());
    }

    @Test
    public void ignores_all_methods_in_classes_annotated_with_ArchIgnore() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(IgnoredArchTest.class);

        runner.runChild(getRule(toBeIgnoredOne, runner), runNotifier);
        runner.runChild(getRule(toBeIgnoredTwo, runner), runNotifier);
        verify(runNotifier, times(2)).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(toBeIgnoredOne)
                .contains(toBeIgnoredTwo);
    }

    @Test
    public void ignores_methods_annotated_with_ArchIgnore() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(ArchTestWithIgnoredMethod.class);

        runner.runChild(getRule(toBeIgnored, runner), runNotifier);
        verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(toBeIgnored);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithTestMethod {
        static final String testSomething = "testSomething";

        static JavaClasses suppliedClasses;

        @ArchTest
        public static void testSomething(JavaClasses classes) {
            suppliedClasses = classes;
        }
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithIllegalTestMethods {
        static final String noParams = "noParams";
        static final String tooManyParams = "tooManyParams";

        @ArchTest
        public static void noParams() {
        }

        @ArchTest
        public static void tooManyParams(JavaClasses classes, int tooMuch) {
        }
    }

    @ArchIgnore
    @AnalyzeClasses(packages = "some.pkg")
    public static class IgnoredArchTest {
        static final String toBeIgnoredOne = "toBeIgnoredOne";
        static final String toBeIgnoredTwo = "toBeIgnoredTwo";

        @ArchTest
        public static void toBeIgnoredOne(JavaClasses classes) {
        }

        @ArchTest
        public static void toBeIgnoredTwo(JavaClasses classes) {
        }
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithIgnoredMethod {
        static final String toBeIgnored = "toBeIgnored";

        @ArchIgnore
        @ArchTest
        public static void toBeIgnored(JavaClasses classes) {
        }
    }
}