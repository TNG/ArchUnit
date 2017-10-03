package com.tngtech.archunit.junit;

import java.lang.reflect.Method;
import java.util.Collection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleSetsTest.ArchTestWithRuleLibrary.someOtherMethodRuleName;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleSetsTest.Rules.someFieldRuleName;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleSetsTest.Rules.someMethodRuleName;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.getRule;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.newRunnerFor;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ClassesIdentityTransformer.classes;
import static com.tngtech.archunit.testutil.TestUtils.invoke;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsRuleSetsTest {
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

    @InjectMocks
    private ArchUnitRunner runnerForRuleSet = newRunnerFor(ArchTestWithRuleSet.class);

    @InjectMocks
    private ArchUnitRunner runnerForRuleLibrary = newRunnerFor(ArchTestWithRuleLibrary.class);

    private JavaClasses cachedClasses = importClassesWithContext(ArchUnitRunnerRunsRuleSetsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyzeFor(any(Class.class))).thenReturn(cachedClasses);
    }

    @Test
    public void should_find_children_in_rule_set() throws Exception {
        assertThat(runnerForRuleSet.getChildren()).as("Rules defined in Test Class").hasSize(2);
        assertThat(runnerForRuleSet.getChildren())
                .extracting(resultOf("describeSelf"))
                .extractingResultOf("getMethodName")
                .as("Descriptions").containsOnly(someFieldRuleName, someMethodRuleName);
    }

    @Test
    public void should_find_children_in_rule_library() throws Exception {
        assertThat(runnerForRuleLibrary.getChildren()).as("Rules defined in Library").hasSize(3);
        assertThat(runnerForRuleLibrary.getChildren())
                .extracting(resultOf("describeSelf"))
                .extractingResultOf("getMethodName")
                .as("Descriptions").containsOnly(someFieldRuleName, someMethodRuleName, someOtherMethodRuleName);
    }

    @Test
    public void can_run_rule_field() throws Exception {
        run(someFieldRuleName);
    }

    @Test
    public void can_run_rule_method() throws Exception {
        run(someMethodRuleName);
    }

    // extractingResultOf(..) only looks for public methods
    private Extractor<Object, Object> resultOf(final String methodName) {
        return new Extractor<Object, Object>() {
            @Override
            public Object extract(Object input) {
                Collection<Method> candidates = ReflectionUtils.getAllMethods(input.getClass(), new ReflectionUtils.Predicate<Method>() {
                    @Override
                    public boolean apply(Method input) {
                        return input.getName().equals(methodName);
                    }
                });
                checkState(!candidates.isEmpty(),
                        "Couldn't find any method named '%s' with hierarchy of %s",
                        methodName, input.getClass().getName());
                return invoke(candidates.iterator().next(), input);
            }
        };
    }

    private void run(String ruleName) {
        ArchTestExecution rule = getRule(ruleName, runnerForRuleSet);

        runnerForRuleSet.runChild(rule, runNotifier);

        verify(runNotifier).fireTestStarted(any(Description.class));
        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(ruleName);
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithRuleLibrary {
        static final String someOtherMethodRuleName = "someOtherMethodRule";

        @ArchTest
        public static final ArchRules rules = ArchRules.in(ArchTestWithRuleSet.class);

        @ArchTest
        public static void someOtherMethodRule(JavaClasses classes) {
        }
    }

    @AnalyzeClasses(packages = "some.pkg")
    public static class ArchTestWithRuleSet {
        @ArchTest
        public static final ArchRules rules = ArchRules.in(Rules.class);
    }

    public static class Rules {
        static final String someFieldRuleName = "someFieldRule";
        static final String someMethodRuleName = "someMethodRule";

        @ArchTest
        public static final ArchRule someFieldRule = all(classes())
                .should(new ArchCondition<JavaClass>("satisfy something") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                    }
                });

        @ArchTest
        public static void someMethodRule(JavaClasses classes) {
        }
    }
}