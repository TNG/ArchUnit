package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludePackageInfos;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.ImportOption.OnlyIncludeTests;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getLast;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_PACKAGE_INFOS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.ONLY_INCLUDE_TESTS;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.crossProduct;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ImportOptionsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @DataProvider
    public static Object[][] do_not_include_tests() {
        return testForEach(new DoNotIncludeTests(), DO_NOT_INCLUDE_TESTS);
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
    public static Object[][] only_include_tests() {
        return testForEach(new OnlyIncludeTests(), ONLY_INCLUDE_TESTS);
    }

    @Test
    @UseDataProvider("only_include_tests")
    public void excludes_main_class(ImportOption onlyIncludeTests) {
        assertThat(onlyIncludeTests.includes(locationOf(OnlyIncludeTests.class)))
                .as("includes production location").isFalse();

        assertThat(onlyIncludeTests.includes(locationOf(getClass())))
                .as("includes test location").isTrue();
    }

    private static List<FolderPattern> getFolderPatterns() {
        return ImmutableList.of(
                // Gradle
                new FolderPattern("build", "classes", "test").expectTestFolder(),
                new FolderPattern("build", "classes", "java", "test").expectTestFolder(),
                new FolderPattern("build", "classes", "otherlang", "test").expectTestFolder(),
                new FolderPattern("build", "test-classes").expectMainFolder(),
                new FolderPattern("build", "classes", "main").expectMainFolder(),
                new FolderPattern("build", "classes", "java", "main").expectMainFolder(),
                new FolderPattern("build", "classes", "java", "main", "my", "test").expectMainFolder(),

                // Maven
                new FolderPattern("target", "classes", "test").expectMainFolder(),
                new FolderPattern("target", "test-classes").expectTestFolder(),
                new FolderPattern("target", "classes").expectMainFolder(),

                // IntelliJ
                new FolderPattern("out", "production", "classes").expectMainFolder(),
                new FolderPattern("out", "test", "classes").expectTestFolder(),
                new FolderPattern("out", "test", "classes", "my", "test").expectTestFolder(),
                new FolderPattern("out", "some", "classes").expectMainFolder()
        );
    }

    @DataProvider
    public static Object[][] test_location_predicates_and_expected_folder_patterns() {
        List<Object[]> includeMainFolderInput = new ArrayList<>();
        List<Object[]> includeTestFolderInput = new ArrayList<>();
        for (FolderPattern folderPattern : getFolderPatterns()) {
            includeMainFolderInput.add($(folderPattern.folders, folderPattern.isMainFolder));
            includeTestFolderInput.add($(folderPattern.folders, !folderPattern.isMainFolder));
        }

        Object[][] doNotIncludeTestsDataPoints = crossProduct(do_not_include_tests(), includeMainFolderInput.toArray(new Object[0][]));
        Object[][] onlyIncludeTestsDataPoints = crossProduct(only_include_tests(), includeTestFolderInput.toArray(new Object[0][]));

        return copyOf(concat(asList(doNotIncludeTestsDataPoints), asList(onlyIncludeTestsDataPoints))).toArray(new Object[0][]);
    }

    @Test
    @UseDataProvider("test_location_predicates_and_expected_folder_patterns")
    public void correctly_detects_all_output_folder_structures(
            ImportOption doNotIncludeTests, String[] folderName, boolean expectedInclude) throws IOException {

        File folder = temporaryFolder.newFolder(folderName);
        File targetFile = new File(folder, getClass().getSimpleName() + ".class");
        Files.copy(locationOf(getClass()).asURI().toURL().openStream(), targetFile.toPath());

        assertThat(doNotIncludeTests.includes(Location.of(targetFile.toPath())))
                .as("includes location %s", targetFile.getAbsolutePath()).isEqualTo(expectedInclude);
    }

    @DataProvider
    public static Object[][] do_not_include_jars() {
        return testForEach(new DoNotIncludeJars(), DO_NOT_INCLUDE_JARS);
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
                .isEqualTo(!comesFromJarArchive(Object.class));
    }

    @DataProvider
    public static Object[][] do_not_include_archives() {
        return testForEach(new DoNotIncludeArchives(), DO_NOT_INCLUDE_ARCHIVES);
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

    @DataProvider
    public static Object[][] do_not_include_package_info_classes() {
        return testForEach(new DoNotIncludePackageInfos(), DO_NOT_INCLUDE_PACKAGE_INFOS);
    }

    @Test
    @UseDataProvider("do_not_include_package_info_classes")
    public void detect_package_info_class(ImportOption doNotIncludePackageInfoClasses) throws URISyntaxException {
        Location packageInfoLocation = Location.of(getClass().getResource(testExampleResourcePath("package-info.class")).toURI());
        assertThat(doNotIncludePackageInfoClasses.includes(packageInfoLocation))
                .as("doNotIncludePackageInfoClasses includes package-info.class")
                .isFalse();

        Location thisClassLocation = locationOf(getClass());
        assertThat(doNotIncludePackageInfoClasses.includes(thisClassLocation))
                .as("doNotIncludePackageInfoClasses includes test class location")
                .isTrue();
    }

    private String testExampleResourcePath(String resourceName) {
        return "/" + getClass().getPackage().getName().replace(".", "/") + "/testexamples/" + resourceName;
    }

    private static Location locationOf(Class<?> clazz) {
        return getLast(Locations.ofClass(clazz));
    }

    private static boolean comesFromJarArchive(Class<?> clazz) {
        return LocationTest.urlOfClass(clazz).getProtocol().equals("jar");
    }

    private static class FolderPattern {
        final String[] folders;
        final boolean isMainFolder;

        FolderPattern(String... folders) {
            this(folders, false);
        }

        private FolderPattern(String[] folders, boolean isMainFolder) {
            this.folders = folders;
            this.isMainFolder = isMainFolder;
        }

        FolderPattern expectMainFolder() {
            return new FolderPattern(folders, true);
        }

        FolderPattern expectTestFolder() {
            return new FolderPattern(folders, false);
        }
    }
}
