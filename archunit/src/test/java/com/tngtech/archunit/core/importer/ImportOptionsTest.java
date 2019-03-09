package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.ImportOption.DontIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DontIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DontIncludeTests;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DONT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DONT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DONT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.crossProduct;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ImportOptionsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @DataProvider
    public static Object[][] do_not_include_tests() {
        return testForEach(new DoNotIncludeTests(), DO_NOT_INCLUDE_TESTS, new DontIncludeTests(), DONT_INCLUDE_TESTS);
    }

    @Test
    @UseDataProvider("do_not_include_tests")
    public void excludes_test_class(ImportOption doNotIncludeTests) {
        assertThat(doNotIncludeTests.includes(locationOf(DoNotIncludeTests.class)))
                .as("includes production location").isTrue();

        assertThat(doNotIncludeTests.includes(locationOf(getClass())))
                .as("includes test location").isFalse();
    }

    @DataProvider
    public static Object[][] folders() {
        return crossProduct(do_not_include_tests(), $$(
                // Gradle
                $(new String[]{"build", "classes", "test"}, false),
                $(new String[]{"build", "classes", "java", "test"}, false),
                $(new String[]{"build", "classes", "otherlang", "test"}, false),
                $(new String[]{"build", "test-classes"}, true),
                $(new String[]{"build", "classes", "main"}, true),
                $(new String[]{"build", "classes", "java", "main"}, true),
                $(new String[]{"build", "classes", "java", "main", "my", "test"}, true),

                // Maven
                $(new String[]{"target", "classes", "test"}, true),
                $(new String[]{"target", "test-classes"}, false),
                $(new String[]{"target", "classes"}, true),

                // IntelliJ
                $(new String[]{"out", "production", "classes"}, true),
                $(new String[]{"out", "test", "classes"}, false),
                $(new String[]{"out", "test", "classes", "my", "test"}, false),
                $(new String[]{"out", "some", "classes"}, true)
        ));
    }

    @Test
    @UseDataProvider("folders")
    public void detects_all_output_folder_structures(
            ImportOption doNotIncludeTests, String[] folderName, boolean expectedInclude) throws IOException {

        File folder = temporaryFolder.newFolder(folderName);
        File targetFile = new File(folder, getClass().getSimpleName() + ".class");
        Files.copy(locationOf(getClass()).asURI().toURL().openStream(), targetFile.toPath());

        assertThat(doNotIncludeTests.includes(Location.of(targetFile.toPath())))
                .as("includes location %s", targetFile.getAbsolutePath()).isEqualTo(expectedInclude);
    }

    @DataProvider
    public static Object[][] do_not_include_jars() {
        return testForEach(new DoNotIncludeJars(), DO_NOT_INCLUDE_JARS, new DontIncludeJars(), DONT_INCLUDE_JARS);
    }

    @Test
    @UseDataProvider("do_not_include_jars")
    public void detects_Jars_correctly(ImportOption doNotIncludeJars) {
        assertThat(doNotIncludeJars.includes(locationOf(getClass())))
                .as("includes file location")
                .isTrue();
        assertThat(doNotIncludeJars.includes(locationOf(Rule.class)))
                .as("includes Jar location")
                .isFalse();
        assertThat(doNotIncludeJars.includes(locationOf(Object.class)))
                .as("includes Jrt location")
                .isTrue();
    }

    @DataProvider
    public static Object[][] do_not_include_archives() {
        return testForEach(new DoNotIncludeArchives(), DO_NOT_INCLUDE_ARCHIVES, new DontIncludeArchives(), DONT_INCLUDE_ARCHIVES);
    }

    @Test
    @UseDataProvider("do_not_include_archives")
    public void detects_archives_correctly(ImportOption doNotIncludeArchives) {
        assertThat(doNotIncludeArchives.includes(locationOf(getClass())))
                .as("includes file location")
                .isTrue();
        assertThat(doNotIncludeArchives.includes(locationOf(Rule.class)))
                .as("includes Jar location")
                .isFalse();
        assertThat(doNotIncludeArchives.includes(locationOf(Object.class)))
                .as("includes Jrt location")
                .isFalse();
    }

    private static Location locationOf(Class<?> clazz) {
        return getOnlyElement(Locations.ofClass(clazz));
    }
}