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
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClass.class);

        assertThat(classes).as("Classes were found").isNotEmpty();
    }

    @Test
    public void reuses_loaded_classes_by_test() {
        cache.getClassesToAnalyzeFor(TestClass.class);
        cache.getClassesToAnalyzeFor(TestClass.class);

        verifyNumberOfImports(1);
    }

    @Test
    public void reuses_loaded_classes_by_locations_if_cacheMode_is_FOREVER() {
        cache.getClassesToAnalyzeFor(TestClass.class);
        cache.getClassesToAnalyzeFor(EquivalentTestClass.class);

        verifyNumberOfImports(1);
    }

    @Test
    public void doesnt_reuse_loaded_classes_by_locations_if_cacheMode_is_PER_CLASS() {
        cache.getClassesToAnalyzeFor(TestClassWithCacheModePerClass.class);
        assertThat(cache.cachedByLocations.asMap()).as("Classes cached by location").isEmpty();

        cache.getClassesToAnalyzeFor(EquivalentTestClass.class);
        verifyNumberOfImports(2);
    }

    @Test
    public void rejects_missing_analyze_annotation() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Object.class.getSimpleName());
        thrown.expectMessage("must be annotated");
        thrown.expectMessage(AnalyzeClasses.class.getSimpleName());

        cache.getClassesToAnalyzeFor(Object.class);
    }

    @Test
    public void filters_jars_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain("tngtech");
        }
    }

    @Test
    public void gets_all_classes_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(classes).isNotEmpty();
    }

    @Test
    public void get_all_classes_by_LocationProvider() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClassWithLocationProviders.class);

        assertThatClasses(classes).contain(String.class, Rule.class, getClass());

        classes = cache.getClassesToAnalyzeFor(TestClassWithLocationProviderUsingTestClass.class);

        assertThatClasses(classes).contain(String.class);
        assertThatClasses(classes).dontContain(getClass());
    }

    @DataProvider
    public static Object[][] illegalLocationProviderClasses() {
        return $$(
                $(TestClassWithIllegalLocationProviderWithConstructorParam.class),
                $(TestClassWithIllegalLocationProviderWithPrivateConstructor.class)
        );
    }

    @Test
    @UseDataProvider("illegalLocationProviderClasses")
    public void rejects_LocationProviders_without_public_default_constructor(
            Class<? extends LocationProvider> illegalProviderClass) {

        thrown.expect(ArchTestExecutionException.class);
        thrown.expectMessage("public default constructor");
        thrown.expectMessage(LocationProvider.class.getSimpleName());

        cache.getClassesToAnalyzeFor(illegalProviderClass);
    }

    @Test
    public void filters_urls() {
        JavaClasses classes = cache.getClassesToAnalyzeFor(TestClassFilteringJustJUnitJars.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain(ClassCache.class.getSimpleName());
            assertThat(clazz.getPackage()).contains("junit");
        }
    }

    @Test
    public void non_existing_packages_are_ignored() {
        JavaClasses first = cache.getClassesToAnalyzeFor(TestClassWithNonExistingPackage.class);
        JavaClasses second = cache.getClassesToAnalyzeFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(first).isEqualTo(second);
        verifyNumberOfImports(1);
    }

    @Test
    public void distinguishes_import_option_when_caching() {
        JavaClasses importingWholeClasspathWithFilter =
                cache.getClassesToAnalyzeFor(TestClassFilteringJustJUnitJars.class);
        JavaClasses importingWholeClasspathWithEquivalentButDifferentFilter =
                cache.getClassesToAnalyzeFor(TestClassFilteringJustJUnitJarsWithDifferentFilter.class);

        assertThat(importingWholeClasspathWithFilter)
                .as("number of classes imported")
                .hasSameSizeAs(importingWholeClasspathWithEquivalentButDifferentFilter);

        verifyNumberOfImports(2);
    }

    @Test
    public void clears_cache_by_class_on_command() {
        cache.getClassesToAnalyzeFor(TestClass.class);
        assertThat(cache.cachedByTest).isNotEmpty();
        cache.clear(EquivalentTestClass.class);
        assertThat(cache.cachedByTest).isNotEmpty();
        cache.clear(TestClass.class);
        assertThat(cache.cachedByTest).isEmpty();
    }

    private void verifyNumberOfImports(int number) {
        verify(cacheClassFileImporter, times(number)).importClasses(any(ImportOptions.class), ArgumentMatchers.<Location>anyCollection());
        verifyNoMoreInteractions(cacheClassFileImporter);
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.junit")
    public static class TestClass {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.junit")
    public static class EquivalentTestClass {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.junit", cacheMode = PER_CLASS)
    public static class TestClassWithCacheModePerClass {
    }

    @AnalyzeClasses(packagesOf = Rule.class)
    public static class TestClassWithFilterJustByPackageOfClass {
    }

    @AnalyzeClasses(packages = "something.that.doesnt.exist", packagesOf = Rule.class)
    public static class TestClassWithNonExistingPackage {
    }

    @AnalyzeClasses(importOptions = TestFilterForJUnitJars.class)
    public static class TestClassFilteringJustJUnitJars {
    }

    @AnalyzeClasses(importOptions = AnotherTestFilterForJUnitJars.class)
    public static class TestClassFilteringJustJUnitJarsWithDifferentFilter {
    }

    @AnalyzeClasses(
            packagesOf = ClassCacheTest.class,
            locations = {TestLocationProviderOfClass_String.class, TestLocationProviderOfClass_Rule.class})
    public static class TestClassWithLocationProviders {
    }

    @LocationOfClass(String.class)
    @AnalyzeClasses(locations = {LocationOfClass.Provider.class})
    public static class TestClassWithLocationProviderUsingTestClass {
    }

    @AnalyzeClasses(locations = WrongLocationProviderWithConstructorParam.class)
    public static class TestClassWithIllegalLocationProviderWithConstructorParam {
    }

    @AnalyzeClasses(locations = WrongLocationProviderWithPrivateConstructor.class)
    public static class TestClassWithIllegalLocationProviderWithPrivateConstructor {
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