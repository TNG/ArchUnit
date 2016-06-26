package com.tngtech.archunit.junit;

import java.net.URL;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.ClassFileImporter.ImportOption;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
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

        verify(classFileImporter, times(1)).importUrls(anyCollection());
        verifyNoMoreInteractions(classFileImporter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void reuses_loaded_classes_by_urls() {
        cache.getClassesToAnalyseFor(TestClass.class);
        cache.getClassesToAnalyseFor(EquivalentTestClass.class);

        verify(classFileImporter, times(1)).importUrls(anyCollection());
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
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithFilterByPackageAndJarOfClass.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain("tngtech");
        }
    }

    @Test
    public void gets_all_classes_relative_to_class() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithFilterJustByJarOfClass.class);

        assertThat(classes).isNotEmpty();
    }

    @Test
    public void filters_urls() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithUrlFilter.class);

        assertThat(classes).isNotEmpty();
        for (JavaClass clazz : classes) {
            assertThat(clazz.getPackage()).doesNotContain(ClassCache.class.getSimpleName());
        }
    }

    @Test
    public void if_intersection_is_empty_no_urls_are_scanned() {
        JavaClasses classes = cache.getClassesToAnalyseFor(TestClassWithEmptyIntersection.class);

        assertThat(classes).isEmpty();
    }

    @AnalyseClasses(packages = "com.tngtech.archunit.junit")
    public static class TestClass {
    }

    @AnalyseClasses(packages = "com.tngtech.archunit.junit")
    public static class EquivalentTestClass {
    }

    @AnalyseClasses(packages = {"org", "com"}, locationsOf = Rule.class)
    public static class TestClassWithFilterByPackageAndJarOfClass {
    }

    @AnalyseClasses(locationsOf = Rule.class)
    public static class TestClassWithFilterJustByJarOfClass {
    }

    @AnalyseClasses(packages = "something.that.doesnt.exist", locationsOf = Rule.class)
    public static class TestClassWithEmptyIntersection {
    }

    @AnalyseClasses(importOption = TestFilter.class)
    public static class TestClassWithUrlFilter {
    }

    public static class TestFilter implements ImportOption {
        @Override
        public boolean includes(URL url) {
            return url.getFile().contains("junit") && url.getFile().endsWith(".jar");
        }
    }
}