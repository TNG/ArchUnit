package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.junit.internal.ArchUnitRunnerInternal.SharedCache;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClassCache cache;
    @Mock
    private SharedCache sharedCache;
    @Captor
    private ArgumentCaptor<ClassAnalysisRequest> analysisRequestCaptor;

    @InjectMocks
    private ArchUnitRunnerInternal runner = newRunner(SomeArchTest.class);
    @InjectMocks
    private ArchUnitRunnerInternal runnerOfMaxTest = newRunner(MaxAnnotatedTest.class);
    @InjectMocks
    private ArchUnitRunnerInternal runnerOfMetaAnnotatedAnalyzerClasses = newRunner(MetaAnnotatedTest.class);

    @Before
    public void setUp() {
        when(sharedCache.get()).thenReturn(cache);
    }

    @Test
    public void runner_creates_correct_analysis_request() {
        runnerOfMaxTest.run(new RunNotifier());

        verify(cache).getClassesToAnalyzeFor(eq(MaxAnnotatedTest.class), analysisRequestCaptor.capture());

        AnalyzeClasses analyzeClasses = MaxAnnotatedTest.class.getAnnotation(AnalyzeClasses.class);
        ClassAnalysisRequest analysisRequest = analysisRequestCaptor.getValue();
        assertThat(analysisRequest.getPackageNames()).isEqualTo(analyzeClasses.packages());
        assertThat(analysisRequest.getPackageRoots()).isEqualTo(analyzeClasses.packagesOf());
        assertThat(analysisRequest.getLocationProviders()).isEqualTo(analyzeClasses.locations());
        assertThat(analysisRequest.scanWholeClasspath()).as("scan whole classpath").isTrue();
        assertThat(analysisRequest.getImportOptions()).isEqualTo(analyzeClasses.importOptions());
    }

    @Test
    public void runner_clears_cache_after_test_run() {
        when(cache.getClassesToAnalyzeFor(eq(SomeArchTest.class), any(ClassAnalysisRequest.class))).thenReturn(importClasses());
        runner.run(new RunNotifier());
        verify(sharedCache).clear(SomeArchTest.class);
    }

    @Test
    public void runner_clears_cache_after_exception_during_test_run() {
        when(cache.getClassesToAnalyzeFor(eq(SomeArchTest.class), any(ClassAnalysisRequest.class))).thenThrow(new RuntimeException("Bummer"));
        runner.run(new RunNotifier());
        verify(sharedCache).clear(SomeArchTest.class);
    }

    @Test
    public void rejects_missing_analyze_annotation() {
        assertThatThrownBy(
                () -> new ArchUnitRunnerInternal(Object.class)
        )
                .isInstanceOf(ArchTestInitializationException.class)
                .hasMessageContaining(Object.class.getSimpleName())
                .hasMessageContaining("must be annotated")
                .hasMessageContaining(AnalyzeClasses.class.getSimpleName());
    }

    @Test
    public void runner_creates_correct_analysis_request_for_meta_annotated_class() {
        runnerOfMetaAnnotatedAnalyzerClasses.run(new RunNotifier());

        verify(cache).getClassesToAnalyzeFor(eq(MetaAnnotatedTest.class), analysisRequestCaptor.capture());

        AnalyzeClasses analyzeClasses = MetaAnnotatedTest.class.getAnnotation(MetaAnnotatedTest.MetaAnalyzeCls.class)
                .annotationType().getAnnotation(AnalyzeClasses.class);
        ClassAnalysisRequest analysisRequest = analysisRequestCaptor.getValue();
        assertThat(analysisRequest.getPackageNames()).isEqualTo(analyzeClasses.packages());
        assertThat(analysisRequest.getPackageRoots()).isEqualTo(analyzeClasses.packagesOf());
        assertThat(analysisRequest.getLocationProviders()).isEqualTo(analyzeClasses.locations());
        assertThat(analysisRequest.scanWholeClasspath()).as("scan whole classpath").isTrue();
        assertThat(analysisRequest.getImportOptions()).isEqualTo(analyzeClasses.importOptions());
    }

    @Test
    public void rejects_if_multiple_analyze_annotations() {
        assertThatThrownBy(
                () -> new ArchUnitRunnerInternal(MultipleAnalyzeClzAnnotationsTest.class)
        )
                .isInstanceOf(ArchTestInitializationException.class)
                .hasMessageContaining("Multiple")
                .hasMessageContaining(AnalyzeClasses.class.getSimpleName())
                .hasMessageContaining("found")
                .hasMessageContaining(MultipleAnalyzeClzAnnotationsTest.class.getSimpleName())
                .hasMessageContaining("not supported");
    }

    private ArchUnitRunnerInternal newRunner(Class<?> testClass) {
        try {
            return new ArchUnitRunnerInternal(testClass);
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

    static class OtherDummyLocation implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Locations.ofClass(ArchUnitRunnerInternal.class);
        }
    }

    static class DummyImportOption implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return false;
        }
    }

    static class OtherDummyImportOption implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return false;
        }
    }

    @AnalyzeClasses(
            packages = {"com.foo", "com.bar"},
            packagesOf = {ArchUnitRunnerInternal.class, ArchUnitRunnerTest.class},
            locations = {DummyLocation.class, OtherDummyLocation.class},
            wholeClasspath = true,
            importOptions = {DummyImportOption.class, OtherDummyImportOption.class}
    )
    public static class MaxAnnotatedTest {
        @ArchTest
        public static void someTest(JavaClasses classes) {
        }
    }

    @MetaAnnotatedTest.MetaAnalyzeCls
    public static class MetaAnnotatedTest {
        @ArchTest
        public static void someTest(JavaClasses classes) {
        }

        @Retention(RetentionPolicy.RUNTIME)
        @AnalyzeClasses(
                packages = {"com.forty", "com.two"},
                wholeClasspath = true
        )
        public @interface MetaAnalyzeCls {
        }
    }

    @MetaAnnotatedTest.MetaAnalyzeCls
    @AnalyzeClasses
    public static class MultipleAnalyzeClzAnnotationsTest {

    }
}
