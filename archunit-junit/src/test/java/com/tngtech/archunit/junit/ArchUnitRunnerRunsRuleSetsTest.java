package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
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

import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleSetsTest.Rules.someFieldRuleName;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleSetsTest.Rules.someMethodRuleName;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.getRule;
import static com.tngtech.archunit.junit.ArchUnitRunnerTestUtils.newRunnerFor;
import static com.tngtech.archunit.lang.ArchRule.all;
import static com.tngtech.archunit.lang.ArchRule.classes;
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
    private ArchUnitRunner runner = newRunnerFor(ArchTestWithRuleSet.class);

    private JavaClasses cachedClasses = javaClasses(ArchUnitRunnerRunsRuleSetsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyseFor(any(Class.class))).thenReturn(cachedClasses);
    }

    @Test
    public void should_find_children() throws Exception {
        assertThat(runner.getChildren()).as("Rules defined in Test Class").hasSize(2);
        assertThat(runner.getChildren()).extractingResultOf("describeSelf").extractingResultOf("getMethodName")
                .as("Descriptions").containsOnly(someFieldRuleName, someMethodRuleName);
    }

    @Test
    public void can_run_rule_field() throws Exception {
        run(someFieldRuleName);
    }

    @Test
    public void can_run_rule_method() throws Exception {
        run(someMethodRuleName);
    }

    private void run(String ruleName) {
        ArchTestExecution rule = getRule(ruleName, runner);

        runner.runChild(rule, runNotifier);

        verify(runNotifier).fireTestStarted(any(Description.class));
        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(ruleName);
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class ArchTestWithRuleSet {
        @ArchTest
        public static final ArchRules<JavaClass> rules = ArchRules.in(Rules.class);
    }

    public static class Rules {
        public static final String someFieldRuleName = "someFieldRule";
        public static final String someMethodRuleName = "someMethodRule";

        @ArchTest
        public static final ArchRule<JavaClass> someFieldRule = all(classes())
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