package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import static com.google.common.collect.Iterators.getOnlyElement;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.collect.Streams.stream;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.tngtech.archunit.core.importer.LocationTest.classFileEntry;
import static com.tngtech.archunit.core.importer.LocationTest.urlOfClass;
import static com.tngtech.archunit.core.importer.LocationsTest.unchecked;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SpringLocationsTest {
    /**
     * Spring Boot configures some system properties that we want to reset afterward (e.g. custom URL stream handler)
     */
    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @DataProvider
    public static Object[][] springBootJars() {
        Function<Function<TestJarFile, TestJarFile>, TestJarFile> createSpringBootJar = setUpJarFile -> setUpJarFile.apply(new TestJarFile())
                .withNestedClassFilesDirectory("BOOT-INF/classes")
                .withEntry(classFileEntry(SomeEnum.class).toAbsolutePath());

        return testForEach(
                createSpringBootJar.apply(TestJarFile::withDirectoryEntries),
                createSpringBootJar.apply(TestJarFile::withoutDirectoryEntries)
        );
    }

    @Test
    @UseDataProvider("springBootJars")
    public void finds_locations_of_packages_from_Spring_Boot_ClassLoader_for_JARs(TestJarFile jarFileToTest) throws Exception {
        try (JarFile jarFile = jarFileToTest.create()) {

            configureSpringBootContextClassLoaderKnowingOnly(jarFile);

            String jarUri = new File(jarFile.getName()).toURI().toString();
            Location location = Locations.ofPackage(SomeEnum.class.getPackage().getName()).stream()
                    .filter(it -> it.contains(jarUri))
                    .collect(onlyElement());

            byte[] expectedClassContent = toByteArray(urlOfClass(SomeEnum.class).openStream());
            Stream<byte[]> actualClassContents = stream(location.asClassFileSource(new ImportOptions()))
                    .map(it -> unchecked(() -> toByteArray(it.openStream())));

            boolean containsExpectedContent = actualClassContents.anyMatch(it -> Arrays.equals(it, expectedClassContent));
            assertThat(containsExpectedContent)
                    .as("one of the found class files has the expected class file content")
                    .isTrue();
        }
    }

    private static void configureSpringBootContextClassLoaderKnowingOnly(JarFile jarFile) throws IOException {
        // This hooks in Spring Boot's own JAR URL protocol handler which knows how to handle URLs with
        // multiple separators (e.g. "jar:file:/dir/some.jar!/BOOT-INF/classes!/pkg/some.class")
        org.springframework.boot.loader.jar.JarFile.registerUrlProtocolHandler();

        try (JarFileArchive jarFileArchive = new JarFileArchive(new File(jarFile.getName()))) {
            JarFileArchive bootInfClassArchive = getNestedJarFileArchive(jarFileArchive, "BOOT-INF/classes/");

            Thread.currentThread().setContextClassLoader(
                    new LaunchedURLClassLoader(false, bootInfClassArchive, new URL[]{bootInfClassArchive.getUrl()}, null)
            );
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static JarFileArchive getNestedJarFileArchive(JarFileArchive jarFileArchive, String path) throws IOException {
        Iterator<Archive> archiveCandidates = jarFileArchive.getNestedArchives(entry -> entry.getName().equals(path), entry -> true);
        return (JarFileArchive) getOnlyElement(archiveCandidates);
    }
}
