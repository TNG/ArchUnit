package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassFileSourceTest {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @DataProvider
    public static Object[][] expected_classes() {
        return $$(
                $(ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class"),
                        new ImportOptions(),
                        ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class")),

                $(ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class"),
                        locationContains("/one/"),
                        ImmutableSet.of("/one/Foo.class", "/one/Bar.class")),

                $(ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class"),
                        locationContains("/two/"),
                        ImmutableSet.of("/two/Bar.class")),

                $(ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class"),
                        locationContains("Bar"),
                        ImmutableSet.of("/one/Bar.class", "/two/Bar.class")),

                $(ImmutableSet.of("/one/Foo.class", "/one/Bar.class", "/two/Bar.class"),
                        locationContains("notthere"),
                        ImmutableSet.of()));
    }

    @Test
    @UseDataProvider("expected_classes")
    public void classes_in_JAR_are_filtered(Set<String> givenEntries, ImportOptions importOptions, final Set<String> expectedIncluded) {
        TestJarFile testJarFile = new TestJarFile();
        for (String entry : givenEntries) {
            testJarFile.withEntry(entry);
        }
        JarFile jarFile = testJarFile.create();

        ClassFileSource source = Location.of(jarFile).asClassFileSource(importOptions);

        assertSourceMatches(source, expectedIncluded);
    }

    @Test
    @UseDataProvider("expected_classes")
    public void classes_from_file_path_are_filtered(
            Set<String> givenFiles, ImportOptions importOptions, final Set<String> expectedIncluded) throws IOException {

        File dir = tempDir.newFolder();
        for (String file : givenFiles) {
            File newFile = new File(dir, file);
            newFile.getParentFile().mkdirs();
            checkState(newFile.createNewFile());
        }

        ClassFileSource source = Location.of(dir.toPath()).asClassFileSource(importOptions);

        assertSourceMatches(source, expectedIncluded);
    }

    private void assertSourceMatches(ClassFileSource source, Set<String> expectedIncluded) {
        assertThat(source).hasSize(expectedIncluded.size());
        assertThat(source)
                .extracting("uri")
                .extractingResultOf("toString")
                .usingElementComparator(MATCH_IF_EXPECTED_IS_SUBSTRING)
                .containsOnlyElementsOf(expectedIncluded);
    }

    private static ImportOptions locationContains(final String part) {
        return new ImportOptions().with(new ImportOption() {
            @Override
            public boolean includes(Location location) {
                return location.contains(part);
            }
        });
    }

    private static final Comparator<Object> MATCH_IF_EXPECTED_IS_SUBSTRING = new Comparator<Object>() {
        @Override
        public int compare(Object uri, Object expectedSub) {
            String uriString = (String) uri;
            String expectedSubstring = (String) expectedSub;
            return uriString.contains(expectedSubstring) ? 0 : uriString.compareTo(expectedSubstring);
        }
    };
}