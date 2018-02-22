package com.tngtech.archunit.junit;

import java.util.Collections;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.ClassCache.CacheClassFileImporter;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.junit.CacheMode.PER_CLASS;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unchecked")
@RunWith(DataProviderRunner.class)
public class ClassCacheTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule()
            .resolveAdditionalDependenciesFromClassPath(false);

    @Spy
    private CacheClassFileImporter cacheClassFileImporter;

    @InjectMocks
    private ClassCache cache = new ClassCache();

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
            assertThat(clazz.getPackage()).doesNotContain("tngtech");
        }
    }

    @Test
    public void gets_all_classes_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, analyzePackagesOf(getClass()));

        assertThat(classes).isNotEmpty();
        assertThat(classes.contain(getClass())).as("root class is contained itself").isTrue();
    }

    @Test
    public void get_all_classes_by_LocationProvider() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class, new TestAnalysisRequest()
                .withPackagesRoots(ClassCacheTest.class)
                .withLocationProviders(TestLocationProviderOfClass_String.class, TestLocationProviderOfClass_Rule.class));

        assertThatClasses(classes).contain(String.class, Rule.class, getClass());

        classes = cache.getClassesToAnalyzeFor(TestClassWithLocationProviderUsingTestClass.class,
                analyzeLocation(LocationOfClass.Provider.class));

        assertThatClasses(classes).contain(String.class);
        assertThatClasses(classes).dontContain(getClass());
    }

    @DataProvider
    public static Object[][] illegalLocationProviderClasses() {
        return $$(
                $(WrongLocationProviderWithConstructorParam.class),
                $(WrongLocationProviderWithPrivateConstructor.class)
        );
    }

    @Test
    @UseDataProvider("illegalLocationProviderClasses")
    public void rejects_LocationProviders_without_public_default_constructor(
            Class<? extends LocationProvider> illegalProviderClass) {

        thrown.expect(ArchTestExecutionException.class);
        thrown.expectMessage("public default constructor");
        thrown.expectMessage(LocationProvider.class.getSimpleName());

        cache.getClassesToAnalyzeFor(illegalProviderClass, analyzeLocation(illegalProviderClass));
    }

    @Test
    public void filters_urls() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class,
                new TestAnalysisRequest().withImportOptions(TestFilterForJUnitJars.class));

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain(ClassCache.class.getSimpleName());
            assertThat(clazz.getPackage()).contains("junit");
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
        verify(cacheClassFileImporter, times(number)).importClasses(any(ImportOptions.class), ArgumentMatchers.<Location>anyCollection());
        verifyNoMoreInteractions(cacheClassFileImporter);
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
        private TestFilterForJUnitJars filter = new TestFilterForJUnitJars();

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

    static class WrongLocationProviderWithPrivateConstructor implements LocationProvider {
        private WrongLocationProviderWithPrivateConstructor() {
        }

        @Override
        public Set<Location> get(Class<?> testClass) {
            return Collections.emptySet();
        }
    }
}