package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.ClassCache.ClassFileImporterFactory;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ClassCacheTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule()
            .resolveAdditionalDependenciesFromClassPath(false);

    @Mock
    private ClassFileImporterFactory classFileImporterFactory;

    @InjectMocks
    private ClassCache cache = new ClassCache();

    private final List<ClassFileImporter> importersUsedForImport = new ArrayList<>();

    @Before
    public void setUp() {
        ClassFileImporter originalImporter = spy(new ClassFileImporter());
        when(classFileImporterFactory.create()).thenReturn(originalImporter);
        doAnswer(new Answer() {
            @Override
            public ClassFileImporter answer(InvocationOnMock invocation) throws Throwable {
                ClassFileImporter withImportOption = (ClassFileImporter) spy(invocation.callRealMethod());
                importersUsedForImport.add(withImportOption);
                return withImportOption;
            }
        }).when(originalImporter).withImportOption(any(ImportOption.class));
    }

    @Test
    public void loads_classes() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClass.class);

        assertThat(classes).as("Classes were found").isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reuses_loaded_classes_by_test() {
        cache.getClassesToAnalyseFor(TestClass.class);
        cache.getClassesToAnalyseFor(TestClass.class);

        verifyNumberOfImports(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reuses_loaded_classes_by_urls() {
        cache.getClassesToAnalyseFor(TestClass.class);
        cache.getClassesToAnalyseFor(EquivalentTestClass.class);

        verifyNumberOfImports(1);
    }

    @Test
    public void rejects_missing_analyse_annotation() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Object.class.getSimpleName());
        thrown.expectMessage("must be annotated");
        thrown.expectMessage(AnalyseClasses.class.getSimpleName());

        cache.getClassesToAnalyseFor(Object.class);
    }

    @Test
    public void filters_jars_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain("tngtech");
        }
    }

    @Test
    public void gets_all_classes_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(classes).isNotEmpty();
    }

    @Test
    public void filters_urls() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassFilteringJustJUnitJars.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain(ClassCache.class.getSimpleName());
            assertThat(clazz.getPackage()).contains("junit");
        }
    }

    @Test
    public void non_existing_packages_are_ignored() {
        JavaClasses first = cache.getClassesToAnalyseFor(TestClassWithNonExistingPackage.class);
        JavaClasses second = cache.getClassesToAnalyseFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(first).isEqualTo(second);
        verifyNumberOfImports(1);
    }

    @Test
    public void distinguishes_import_option_when_caching() {
        JavaClasses importingWholeClasspathWithFilter =
                cache.getClassesToAnalyseFor(TestClassFilteringJustJUnitJars.class);
        JavaClasses importingWholeClasspathWithEquivalentButDifferentFilter =
                cache.getClassesToAnalyseFor(TestClassFilteringJustJUnitJarsWithDifferentFilter.class);

        assertThat(importingWholeClasspathWithFilter)
                .as("number of classes imported")
                .hasSameSizeAs(importingWholeClasspathWithEquivalentButDifferentFilter);

        verifyNumberOfImports(2);
    }

    private void verifyNumberOfImports(int number) {
        assertThat(importersUsedForImport).as("Number of created importers").hasSize(number);
        for (ClassFileImporter importer : importersUsedForImport) {
            verify(importer).importLocations(anyCollection());
            verifyNoMoreInteractions(importer);
        }
    }

    @AnalyseClasses(packages = "com.tngtech.archunit.junit")
    public static class TestClass {
    }

    @AnalyseClasses(packages = "com.tngtech.archunit.junit")
    public static class EquivalentTestClass {
    }

    @AnalyseClasses(packagesOf = Rule.class)
    public static class TestClassWithFilterJustByPackageOfClass {
    }

    @AnalyseClasses(packages = "something.that.doesnt.exist", packagesOf = Rule.class)
    public static class TestClassWithNonExistingPackage {
    }

    @AnalyseClasses(importOption = TestFilterForJUnitJars.class)
    public static class TestClassFilteringJustJUnitJars {
    }

    @AnalyseClasses(importOption = AnotherTestFilterForJUnitJars.class)
    public static class TestClassFilteringJustJUnitJarsWithDifferentFilter {
    }

    public static class TestFilterForJUnitJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("junit") && location.contains(".jar");
        }
    }

    public static class AnotherTestFilterForJUnitJars implements ImportOption {
        private TestFilterForJUnitJars filter = new TestFilterForJUnitJars();

        @Override
        public boolean includes(Location location) {
            return filter.includes(location);
        }
    }
}