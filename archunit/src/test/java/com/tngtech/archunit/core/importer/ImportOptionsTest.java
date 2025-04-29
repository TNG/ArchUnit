package com.tngtech.archunit.core.importer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeGradleTestFixtures;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludePackageInfos;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.ImportOption.OnlyIncludeTests;
import org.junit.Rule;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.google.common.collect.Iterables.getLast;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_PACKAGE_INFOS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TEST_FIXTURES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.ONLY_INCLUDE_TESTS;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static com.tngtech.archunit.testutil.TestUtils.relativeResourceUri;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class ImportOptionsTest {
    @TempDir
    Path temporaryPath;

    static Stream<ImportOption> do_not_include_tests() {
        return Stream.of(new DoNotIncludeTests(), DO_NOT_INCLUDE_TESTS);
    }

    @ParameterizedTest
    @MethodSource("do_not_include_tests")
    void excludes_test_class(ImportOption doNotIncludeTests) {
        assertThat(doNotIncludeTests.includes(locationOf(DoNotIncludeTests.class)))
                .as("includes production location").isTrue();

        assertThat(doNotIncludeTests.includes(locationOf(getClass())))
                .as("includes test location").isFalse();
    }

    static Stream<ImportOption> only_include_tests() {
        return Stream.of(new OnlyIncludeTests(), ONLY_INCLUDE_TESTS);
    }

    @ParameterizedTest
    @MethodSource("only_include_tests")
    void excludes_main_class(ImportOption onlyIncludeTests) {
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
                new FolderPattern("out", "test", "some-module").expectTestFolder(),
                new FolderPattern("out", "test", "classes", "my", "test").expectTestFolder(),
                new FolderPattern("out", "some", "classes").expectMainFolder()
        );
    }

    static Stream<Arguments> test_location_predicates_and_expected_folder_patterns() {
        Set<Arguments> includeMainFolderInput = new HashSet<>();
        Set<Arguments> includeTestFolderInput = new HashSet<>();
        for (FolderPattern folderPattern : getFolderPatterns()) {
            includeMainFolderInput.add($(folderPattern.folders, folderPattern.isMainFolder));
            includeTestFolderInput.add($(folderPattern.folders, !folderPattern.isMainFolder));
        }

        Stream<Arguments> doNotIncludeTestsDataPoints = crossProduct(do_not_include_tests(), includeMainFolderInput);
        Stream<Arguments> onlyIncludeTestsDataPoints = crossProduct(only_include_tests(), includeTestFolderInput);

        return Stream.concat(doNotIncludeTestsDataPoints, onlyIncludeTestsDataPoints);
    }

    private static Stream<Arguments> crossProduct(Stream<ImportOption> importOptionStream, Set<Arguments> includeMainFolderInput) {
        Set<Arguments> importOptions = importOptionStream.map(Arguments::of).collect(Collectors.toSet());
        return Sets.cartesianProduct(importOptions, includeMainFolderInput).stream()
                .map(args -> Arguments.of(Stream.concat(stream(args.get(0).get()), stream(args.get(1).get())).toArray()));
    }

    @ParameterizedTest
    @MethodSource("test_location_predicates_and_expected_folder_patterns")
    void correctly_detects_all_output_folder_structures(
            ImportOption doNotIncludeTests, String[] folderName, boolean expectedInclude) throws IOException {

        Path path = temporaryPath.resolve(String.join("/", folderName)).resolve("c.class");

        assertThat(doNotIncludeTests.includes(Location.of(path)))
                .as("includes location %s", path).isEqualTo(expectedInclude);
    }

    static Stream<ImportOption> excludes_test_fixtures() {
        return Stream.of(new DoNotIncludeGradleTestFixtures(), DO_NOT_INCLUDE_TEST_FIXTURES);
    }

    @ParameterizedTest
    @MethodSource
    void excludes_test_fixtures(ImportOption doNotIncludeTestFixtures) {
        assertThat(doNotIncludeTestFixtures.includes(Location.of(URI.create("file:///any/build/classes/java/test/com/SomeTest.class"))))
                .as("includes test class from file path").isTrue();
        assertThat(doNotIncludeTestFixtures.includes(Location.of(URI.create("jar:file:///any/build/libs/some-test.jar!/com/SomeTest"))))
                .as("includes test class from JAR path").isTrue();

        assertThat(doNotIncludeTestFixtures.includes(Location.of(URI.create("file:///any/build/classes/java/testFixtures/com/SomeFixture.class"))))
                .as("excludes test fixture from file path").isFalse();
        assertThat(doNotIncludeTestFixtures.includes(Location.of(URI.create("jar:file:///any/build/libs/some-test-test-fixtures.jar!/com/SomeFixture"))))
                .as("excludes test fixture from JAR path").isFalse();
    }

    static Stream<ImportOption> do_not_include_jars() {
        return Stream.of(new DoNotIncludeJars(), DO_NOT_INCLUDE_JARS);
    }

    @ParameterizedTest
    @MethodSource("do_not_include_jars")
    void detects_Jars_correctly(ImportOption doNotIncludeJars) {
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

    static Stream<ImportOption> do_not_include_archives() {
        return Stream.of(new DoNotIncludeArchives(), DO_NOT_INCLUDE_ARCHIVES);
    }

    @ParameterizedTest
    @MethodSource("do_not_include_archives")
    void detects_archives_correctly(ImportOption doNotIncludeArchives) {
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

    static Stream<ImportOption> do_not_include_package_info_classes() {
        return Stream.of(new DoNotIncludePackageInfos(), DO_NOT_INCLUDE_PACKAGE_INFOS);
    }

    @ParameterizedTest
    @MethodSource("do_not_include_package_info_classes")
    void detect_package_info_class(ImportOption doNotIncludePackageInfoClasses) {
        Location packageInfoLocation = Location.of(relativeResourceUri(getClass(), "testexamples/package-info.class"));
        assertThat(doNotIncludePackageInfoClasses.includes(packageInfoLocation))
                .as("doNotIncludePackageInfoClasses includes package-info.class")
                .isFalse();

        Location thisClassLocation = locationOf(getClass());
        assertThat(doNotIncludePackageInfoClasses.includes(thisClassLocation))
                .as("doNotIncludePackageInfoClasses includes test class location")
                .isTrue();
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
