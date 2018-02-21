package com.tngtech.archunit.junit;

import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClassCache cache;
    @Mock
    private SharedCache sharedCache;

    @InjectMocks
    private ArchUnitRunner runner = newRunner();

    @Before
    public void setUp() {
        when(sharedCache.get()).thenReturn(cache);
    }

    @Test
    public void runner_clears_cache_after_test_run() {
        when(cache.getClassesToAnalyzeFor(SomeArchTest.class)).thenReturn(importClasses());
        runner.run(new RunNotifier());
        verify(sharedCache).clear(SomeArchTest.class);
    }

    @Test
    public void runner_clears_cache_after_exception_during_test_run() {
        when(cache.getClassesToAnalyzeFor(SomeArchTest.class)).thenThrow(new RuntimeException("Bummer"));
        runner.run(new RunNotifier());
        verify(sharedCache).clear(SomeArchTest.class);
    }

    private ArchUnitRunner newRunner() {
        try {
            return new ArchUnitRunner(SomeArchTest.class);
        } catch (InitializationError error) {
            throw new RuntimeException(error);
        }
    }

    @AnalyzeClasses(locations = DummyLocation.class)
    public static class SomeArchTest {
        @ArchTest
        public static ArchRule rule1 = classes().should(beOkay());
        @ArchTest
        public static ArchRule rule2 = classes().should(beOkay());

        private static ArchCondition<JavaClass> beOkay() {
            return new ArchCondition<JavaClass>("be okay") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                }
            };
        }
    }

    static class DummyLocation implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Locations.ofClass(getClass());
        }
    }
}