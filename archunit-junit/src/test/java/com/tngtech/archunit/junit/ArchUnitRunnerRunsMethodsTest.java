package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIgnoredMethod.toBeIgnored;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIllegalTestMethods.noParams;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithIllegalTestMethods.tooManyParams;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.ArchTestWithTestMethod.testSomething;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.IgnoredArchTest.toBeIgnoredOne;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsMethodsTest.IgnoredArchTest.toBeIgnoredTwo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsMethodsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
    private ArchUnitRunner runner = newRunner();

    private JavaClasses cachedClasses = javaClasses(ArchUnitRunnerRunsMethodsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyseFor(any(Class.class))).thenReturn(cachedClasses);
    }

    @Test
    public void executes_test_methods_and_supplies_JavaClasses() throws InitializationError {
        runner.runChild(getTestMethod(testSomething, runner), runNotifier);
        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(testSomething);
        assertThat(ArchTestWithTestMethod.suppliedClasses).as("Supplied Classes").isEqualTo(cachedClasses);
    }

    @Test
    public void fails_methods_with_no_parameters() throws InitializationError {
        runAndAssertWrongParametersForChild(noParams, new ArchUnitRunner(ArchTestWithIllegalTestMethods.class));
    }

    @Test
    public void fails_methods_with_too_many_parameters() throws InitializationError {
        runAndAssertWrongParametersForChild(tooManyParams, new ArchUnitRunner(ArchTestWithIllegalTestMethods.class));
    }

    private void runAndAssertWrongParametersForChild(String name, ArchUnitRunner runner) {
        runner.runChild(getTestMethod(name, runner), runNotifier);
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

        runner.runChild(getTestMethod(toBeIgnoredOne, runner), runNotifier);
        runner.runChild(getTestMethod(toBeIgnoredTwo, runner), runNotifier);
        verify(runNotifier, times(2)).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(toBeIgnoredOne)
                .contains(toBeIgnoredTwo);
    }

    @Test
    public void ignores_methods_annotated_with_ArchIgnore() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(ArchTestWithIgnoredMethod.class);

        runner.runChild(getTestMethod(toBeIgnored, runner), runNotifier);
        verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(toBeIgnored);
    }

    private ArchTestExecution getTestMethod(String name, ArchUnitRunner runner) {
        for (ArchTestExecution ruleToTest : runner.getChildren()) {
            if (ruleToTest instanceof ArchTestMethodExecution
                    && name.equals(((ArchTestMethodExecution) ruleToTest).getTestMethod().getName())) {
                return ruleToTest;
            }
        }
        throw new RuntimeException(String.format("Couldn't find Method with name '%s'", name));
    }

    private ArchUnitRunner newRunner() {
        try {
            return new ArchUnitRunner(ArchTestWithTestMethod.class);
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class ArchTestWithTestMethod {
        static final String testSomething = "testSomething";

        static JavaClasses suppliedClasses;

        @ArchTest
        public void testSomething(JavaClasses classes) {
            suppliedClasses = classes;
        }
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class ArchTestWithIllegalTestMethods {
        static final String noParams = "noParams";
        static final String tooManyParams = "tooManyParams";

        @ArchTest
        public void noParams() {
        }

        @ArchTest
        public void tooManyParams(JavaClasses classes, int tooMuch) {
        }
    }

    @ArchIgnore
    @AnalyseClasses(packages = "some.pkg")
    public static class IgnoredArchTest {
        static final String toBeIgnoredOne = "toBeIgnoredOne";
        static final String toBeIgnoredTwo = "toBeIgnoredTwo";

        @ArchTest
        public void toBeIgnoredOne(JavaClasses classes) {
        }

        @ArchTest
        public void toBeIgnoredTwo(JavaClasses classes) {
        }
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class ArchTestWithIgnoredMethod {
        static final String toBeIgnored = "toBeIgnored";

        @ArchIgnore
        @ArchTest
        public void toBeIgnored(JavaClasses classes) {
        }
    }
}