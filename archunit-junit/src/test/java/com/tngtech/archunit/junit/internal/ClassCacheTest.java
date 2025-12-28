package com.tngtech.archunit.junit.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.junit.internal.ClassCache.CacheClassFileImporter;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.junit.CacheMode.PER_CLASS;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(DataProviderRunner.class)
public class ClassCacheTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule()
            .resolveAdditionalDependenciesFromClassPath(false);

    @Spy
    private CacheClassFileImporter cacheClassFileImporter;

    @InjectMocks
    private ClassCache cache = new ClassCache();

    @Captor
    private ArgumentCaptor<Collection<Location>> locationCaptor;

    @Test
    public void loads_classes() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit"));

        assertThat(classes).as("Classes were found").isNotEmpty();
    }

    @Test
    public void reuses_loaded_classes_by_test() {
        cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit"));
        cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit"));

        verifyNumberOfImports(1);
    }

    @Test
    public void reuses_loaded_classes_by_locations_if_cacheMode_is_FOREVER() {
        cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit"));
        cache.getClassesToAnalyzeFor(EquivalentTestClass.class, analyzePackages("com.tngtech.archunit.junit"));

        verifyNumberOfImports(1);
    }

    @Test
    public void doesnt_reuse_loaded_classes_by_locations_if_cacheMode_is_PER_CLASS() {
        cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit").withCacheMode(PER_CLASS));
        assertThat(cache.cachedByLocations.asMap()).as("Classes cached by location").isEmpty();

        cache.getClassesToAnalyzeFor(EquivalentTestClass.class, analyzePackages("com.tngtech.archunit.junit").withCacheMode(PER_CLASS));
        verifyNumberOfImports(2);
    }

    @Test
    public void filters_jars_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, analyzePackagesOf(Rule.class));

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackageName()).doesNotContain("tngtech");
        }
    }

    @Test
    public void gets_all_classes_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, analyzePackagesOf(getClass()));

        assertThat(classes).isNotEmpty();
        assertThat(classes.contain(getClass())).as("root class is contained itself").isTrue();
    }

    @Test
    public void gets_all_classes_specified() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, new TestAnalysisRequest()
                .withClassesToAnalyze(getClass()));

        assertThat(classes).hasSize(1);
        assertThat(classes.contain(getClass())).as("root class is contained itself").isTrue();
    }

    @Test
    public void get_all_classes_by_LocationProvider() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, new TestAnalysisRequest()
                .withPackagesRoots(ClassCacheTest.class)
                .withLocationProviders(TestLocationProviderOfClass_String.class, TestLocationProviderOfClass_Rule.class));

        assertThatTypes(classes).contain(String.class, Rule.class, getClass());

        classes = cache.getClassesToAnalyzeFor(TestClassWithLocationProviderUsingTestClass.class,
                analyzeLocation(LocationOfClass.Provider.class));

        assertThatTypes(classes).contain(String.class);
        assertThatTypes(classes).doNotContain(getClass());
    }

    @Test
    public void rejects_LocationProviders_without_default_constructor() {
        assertThatThrownBy(
                () -> cache.getClassesToAnalyzeFor(WrongLocationProviderWithConstructorParam.class,
                        analyzeLocation(WrongLocationProviderWithConstructorParam.class))
        )
                .isInstanceOf(ArchTestExecutionException.class)
                .hasMessageContaining("public default constructor")
                .hasMessageContaining(LocationProvider.class.getSimpleName());
    }

    @Test
    public void if_no_import_locations_are_specified_and_whole_classpath_is_set_false_then_the_default_is_the_package_of_the_test_class() {
        TestAnalysisRequest defaultOptions = new TestAnalysisRequest().withWholeClasspath(false);

        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, defaultOptions);

        assertThatTypes(classes).contain(getClass(), TestAnalysisRequest.class);
        assertThatTypes(classes).doNotContain(ClassFileImporter.class);
    }

    @Test
    public void if_whole_classpath_is_set_true_then_the_whole_classpath_is_imported() {
        TestAnalysisRequest defaultOptions = new TestAnalysisRequest().withWholeClasspath(true);
        Class<?>[] expectedImportResult = new Class[]{getClass()};
        doReturn(new ClassFileImporter().importClasses(expectedImportResult))
                .when(cacheClassFileImporter).importClasses(anySet(), anyCollection());

        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, defaultOptions);

        assertThatTypes(classes).matchExactly(expectedImportResult);
        verify(cacheClassFileImporter).importClasses(anySet(), locationCaptor.capture());
        assertThat(locationCaptor.getValue())
                .has(locationContaining("archunit"))
                .has(locationContaining("asm"))
                .has(locationContaining("google"))
                .has(locationContaining("mockito"));
    }

    @Test
    public void filters_urls() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class,
                new TestAnalysisRequest().withImportOptions(TestFilterForJUnitJars.class).withWholeClasspath(true));

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackageName()).doesNotContain("tngtech");
            assertThat(clazz.getPackageName()).contains("junit");
        }
    }

    @Test
    public void non_existing_packages_are_ignored() {
        JavaClasses first = cache.getClassesToAnalyzeFor(TestClass.class, new TestAnalysisRequest()
                .withPackages("something.that.doesnt.exist")
                .withPackagesRoots(Rule.class));
        JavaClasses second = cache.getClassesToAnalyzeFor(TestClass.class,
                analyzePackagesOf(Rule.class));

        assertThat(first).isEqualTo(second);
        verifyNumberOfImports(1);
    }

    @DataProvider
    public static Object[][] test_classes_without_any_imported_classes() {
        return testForEach(
                new TestAnalysisRequest().withPackages("does.not.exist"),
                new TestAnalysisRequest().withLocationProviders(EmptyLocations.class));
    }

    @Test
    @UseDataProvider("test_classes_without_any_imported_classes")
    public void when_there_are_only_nonexisting_sources_nothing_is_imported(TestAnalysisRequest analysisRequest) {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, analysisRequest);

        assertThat(classes).isEmpty();

        verify(cacheClassFileImporter).importClasses(anySet(), locationCaptor.capture());
        assertThat(locationCaptor.getValue()).isEmpty();
    }

    @Test
    public void distinguishes_import_option_when_caching() {
        JavaClasses importingWholeClasspathWithFilter =
                cache.getClassesToAnalyzeFor(TestClass.class, new TestAnalysisRequest().withImportOptions(TestFilterForJUnitJars.class));
        JavaClasses importingWholeClasspathWithEquivalentButDifferentFilter =
                cache.getClassesToAnalyzeFor(EquivalentTestClass.class,
                        new TestAnalysisRequest().withImportOptions(AnotherTestFilterForJUnitJars.class));

        assertThat(importingWholeClasspathWithFilter)
                .as("number of classes imported")
                .hasSameSizeAs(importingWholeClasspathWithEquivalentButDifferentFilter);

        verifyNumberOfImports(2);
    }

    @Test
    public void clears_cache_by_class_on_command() {
        cache.getClassesToAnalyzeFor(TestClass.class, analyzePackages("com.tngtech.archunit.junit"));
        assertThat(cache.cachedByTest).isNotEmpty();
        cache.clear(EquivalentTestClass.class);
        assertThat(cache.cachedByTest).isNotEmpty();
        cache.clear(TestClass.class);
        assertThat(cache.cachedByTest).isEmpty();
    }

    private TestAnalysisRequest analyzePackages(String packages) {
        return new TestAnalysisRequest().withPackages(packages);
    }

    private ClassAnalysisRequest analyzePackagesOf(Class<?> clazz) {
        return new TestAnalysisRequest().withPackagesRoots(clazz);
    }

    private ClassAnalysisRequest analyzeLocation(Class<? extends LocationProvider> providerClass) {
        return new TestAnalysisRequest().withLocationProviders(providerClass);
    }

    private void verifyNumberOfImports(int number) {
        verify(cacheClassFileImporter, times(number)).importClasses(anySet(), anyCollection());
        verifyNoMoreInteractions(cacheClassFileImporter);
    }

    private static Condition<Iterable<? extends Location>> locationContaining(String part) {
        return new Condition<Iterable<? extends Location>>() {
            @Override
            public boolean matches(Iterable<? extends Location> locations) {
                for (Location location : locations) {
                    if (location.contains(part)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static class TestClass {
    }

    public static class EquivalentTestClass {
    }

    @LocationOfClass(String.class)
    public static class TestClassWithLocationProviderUsingTestClass {
    }

    public static class TestFilterForJUnitJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("/org/junit") && location.isJar();
        }
    }

    public static class AnotherTestFilterForJUnitJars implements ImportOption {
        private final TestFilterForJUnitJars filter = new TestFilterForJUnitJars();

        @Override
        public boolean includes(Location location) {
            return filter.includes(location);
        }
    }

    static class TestLocationProviderOfClass_String implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Locations.ofClass(String.class);
        }
    }

    static class TestLocationProviderOfClass_Rule implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Locations.ofClass(Rule.class);
        }
    }

    static class WrongLocationProviderWithConstructorParam implements LocationProvider {
        @SuppressWarnings("unused")
        public WrongLocationProviderWithConstructorParam(String illegalParameter) {
        }

        @Override
        public Set<Location> get(Class<?> testClass) {
            return Collections.emptySet();
        }
    }

    static class EmptyLocations implements LocationProvider {
        @Override
        public Set<Location> get(Class<?> testClass) {
            return Collections.emptySet();
        }
    }
}
