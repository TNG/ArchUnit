package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.ClassFileImporter.ImportOption;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.Location;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ClassCacheTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Spy
    private ClassFileImporter classFileImporter = new ClassFileImporter();

    @InjectMocks
    private ClassCache cache = new ClassCache();

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

        verify(classFileImporter, times(1)).importLocations(anyCollection());
        verifyNoMoreInteractions(classFileImporter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reuses_loaded_classes_by_urls() {
        cache.getClassesToAnalyseFor(TestClass.class);
        cache.getClassesToAnalyseFor(EquivalentTestClass.class);

        verify(classFileImporter, times(1)).importLocations(anyCollection());
        verifyNoMoreInteractions(classFileImporter);
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
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithImportOption.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain(ClassCache.class.getSimpleName());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void non_existing_packages_are_ignored() {
        JavaClasses first = cache.getClassesToAnalyseFor(TestClassWithNonExistingPackage.class);
        JavaClasses second = cache.getClassesToAnalyseFor(TestClassWithFilterJustByPackageOfClass.class);

        assertThat(first).isEqualTo(second);
        verify(classFileImporter, times(1)).importLocations(anyCollection());
        verifyNoMoreInteractions(classFileImporter);
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

    @AnalyseClasses(importOption = TestFilter.class)
    public static class TestClassWithImportOption {
    }

    public static class TestFilter implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("junit") && location.contains(".jar");
        }
    }
}