package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassFileSourceTest {
    static final String MODULE_INFO_FILE_NAME = "module-info.class";

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
            File dirOfFile = newFile.getParentFile();
            checkArgument(dirOfFile.exists() || dirOfFile.mkdirs(), "Cannot create %s", dirOfFile.getAbsolutePath());
            checkState(newFile.createNewFile());
        }

        ClassFileSource source = Location.of(dir.toPath()).asClassFileSource(importOptions);

        assertSourceMatches(source, expectedIncluded);
    }

    @Test
    public void filters_out_module_infos_in_Jar_location() {
        String onlyExpectedEntry = "pkg/Some.class";
        JarFile jarFile = new TestJarFile()
                .withEntry(onlyExpectedEntry)
                .withEntry(MODULE_INFO_FILE_NAME)
                .create();

        ClassFileSource source = Location.of(jarFile).asClassFileSource(new ImportOptions());

        assertThat(getOnlyElement(source).getUri().toString()).contains(onlyExpectedEntry);
    }

    @Test
    public void filters_out_module_infos_in_file_location() throws IOException {
        File dir = tempDir.newFolder();
        createDummyModuleInfoIn(dir);
        File classFile = createDummyclassFileIn(dir);

        ClassFileSource source = Location.of(dir.toPath()).asClassFileSource(new ImportOptions());

        assertThat(getOnlyElement(source).getUri().toString()).contains(classFile.getName());
    }

    @Test
    public void resolves_class_files_with_whitespace() throws IOException {
        File file = tempDir.newFile("path with spaces like kotlin does.class");

        ClassFileSource classFileSource = new ClassFileSource.FromFilePath(file.toPath(), new ImportOptions());

        checkAllElementsCanBeRead(classFileSource);
    }

    @Test
    public void resolves_jar_entries_with_whitespace() throws MalformedURLException {
        JarFile jarFile = new TestJarFile()
                .withEntry("path with spaces")
                .withEntry("path with spaces/like kotlin does.class")
                .create();

        ClassFileSource classFileSource = new ClassFileSource.FromJar(jarUrlOf(jarFile), "", new ImportOptions());

        checkAllElementsCanBeRead(classFileSource);
    }

    @SuppressWarnings("EmptyTryBlock")
    private void checkAllElementsCanBeRead(ClassFileSource classFileSource) {
        for (ClassFileLocation location : classFileSource) {
            try (InputStream ignored = location.openStream()) {
                // if the stream can be opened, we are satisfied
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private URL jarUrlOf(JarFile jarFile) throws MalformedURLException {
        return new URL("jar:" + Paths.get(jarFile.getName()).toUri().toURL() + "!/");
    }

    private void createDummyModuleInfoIn(File folder) throws IOException {
        createDummyFile(folder, MODULE_INFO_FILE_NAME);
    }

    private File createDummyclassFileIn(File folder) throws IOException {
        return createDummyFile(folder, "Some.class");
    }

    private File createDummyFile(File folder, String name) throws IOException {
        File file = new File(folder, name);
        checkState(file.createNewFile());
        return file;
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