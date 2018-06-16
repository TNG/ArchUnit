package com.tngtech.archunit.core.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlSourceTest {
    private static final String JAVA_CLASS_PATH_PROP = "java.class.path";
    private static final String JAVA_BOOT_PATH_PROP = "sun.boot.class.path";

    private static final char CHARACTER_THAT_IS_HOPEFULLY_ILLEGAL_ON_EVERY_PLATFORM = '\0';

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolves_from_system_property() throws MalformedURLException {
        Path firstFileEntry = Paths.get("some", "path", "classes");
        Path firstJarEntry = Paths.get("other", "lib", "some.jar");
        Path secondFileEntry = Paths.get("more", "classes");
        Path secondJarEntry = Paths.get("my", ".m2", "repo", "greatlib.jar");
        String classPath = createClassPathProperty(firstFileEntry.toString(), firstJarEntry.toString(),
                secondFileEntry.toString(), secondJarEntry.toString());
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);

        Path bootstrapFileEntry = Paths.get("some", "bootstrap", "classes");
        Path bootstrapJarEntry = Paths.get("more", "bootstrap", "bootlib.jar");
        String bootstrapClassPath = createClassPathProperty(bootstrapFileEntry.toString(), bootstrapJarEntry.toString());
        System.setProperty(JAVA_BOOT_PATH_PROP, bootstrapClassPath);

        UrlSource urlSource = UrlSource.From.classPathSystemProperties();

        assertThat(urlSource).containsOnly(
                firstFileEntry.toUri().toURL(),
                new URL("jar:" + firstJarEntry.toUri() + "!/"),
                secondFileEntry.toUri().toURL(),
                new URL("jar:" + secondJarEntry.toUri() + "!/"),
                bootstrapFileEntry.toUri().toURL(),
                new URL("jar:" + bootstrapJarEntry.toUri() + "!/")
        );
    }

    @Test
    public void resolves_missing_system_properties_resiliently() {
        System.clearProperty(JAVA_BOOT_PATH_PROP);
        System.clearProperty(JAVA_CLASS_PATH_PROP);

        assertThat(UrlSource.From.classPathSystemProperties()).isEmpty();
    }

    @Test
    public void ignores_invalid_paths_in_class_path_property() throws MalformedURLException {
        Path valid = Paths.get("some", "valid", "path");

        String classPath = createClassPathProperty(valid.toString(),
                "/invalid/path/because/of/" + CHARACTER_THAT_IS_HOPEFULLY_ILLEGAL_ON_EVERY_PLATFORM + "/");
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);

        assertThat(UrlSource.From.classPathSystemProperties()).containsOnly(valid.toUri().toURL());
    }

    @Test
    public void returns_unique_urls() {
        URL url = getClass().getResource(".");
        ImmutableList<URL> redundantInput = ImmutableList.of(url, url);

        UrlSource source = UrlSource.From.iterable(redundantInput);

        assertThat(source).hasSize(1).containsOnly(url);
    }

    @Test
    public void handles_paths_with_spaces() throws Exception {
        Path path_with_spaces = temporaryFolder.newFolder("path with spaces").toPath();
        Path destination = path_with_spaces.resolve(getClass().getName() + ".class");
        Files.copy(Paths.get(LocationTest.urlOfClass(getClass()).toURI()), destination);

        String classPath = createClassPathProperty(destination.toString());
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);
        UrlSource urls = UrlSource.From.classPathSystemProperties();

        assertThat(urls).contains(destination.toUri().toURL());
    }

    private String createClassPathProperty(String... paths) {
        return Joiner.on(File.pathSeparatorChar).join(paths);
    }
}