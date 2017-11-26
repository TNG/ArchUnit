package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.tngtech.archunit.core.importer.ImportOption.DontIncludeTests;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class DontIncludeTestsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DontIncludeTests dontIncludeTests = new DontIncludeTests();

    @Test
    public void excludes_test_class() {
        assertThat(dontIncludeTests.includes(locationOf(DontIncludeTests.class)))
                .as("includes production location").isTrue();

        assertThat(dontIncludeTests.includes(locationOf(getClass())))
                .as("includes test location").isFalse();
    }

    @DataProvider
    public static Object[][] folders() {
        return $$(
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
        );
    }

    @Test
    @UseDataProvider("folders")
    public void detects_all_output_folder_structures(String[] folderName, boolean expectedInclude) throws IOException {
        File folder = temporaryFolder.newFolder(folderName);
        File targetFile = new File(folder, getClass().getSimpleName() + ".class");
        Files.copy(locationOf(getClass()).asURI().toURL().openStream(), targetFile.toPath());

        assertThat(dontIncludeTests.includes(Location.of(targetFile.toPath())))
                .as("includes location %s", targetFile.getAbsolutePath()).isEqualTo(expectedInclude);
    }

    private Location locationOf(Class<?> clazz) {
        return getOnlyElement(Locations.ofClass(clazz));
    }
}