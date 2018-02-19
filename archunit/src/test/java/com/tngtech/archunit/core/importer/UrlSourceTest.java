package com.tngtech.archunit.core.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlSourceTest {
    private static final String JAVA_CLASS_PATH_PROP = "java.class.path";
    private static final String JAVA_BOOT_PATH_PROP = "sun.boot.class.path";

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void resolves_from_system_property() throws MalformedURLException {
        String classPath = createClassPathProperty(
                "/some/path/classes", "/other/lib/some.jar", "/more/classes", "/my/.m2/repo/greatlib.jar");
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);
        String bootstrapClassPath = createClassPathProperty("/some/bootstrap/classes", "/more/bootstrap/bootlib.jar");
        System.setProperty(JAVA_BOOT_PATH_PROP, bootstrapClassPath);

        UrlSource urlSource = UrlSource.From.classPathSystemProperties();

        assertThat(urlSource).containsOnly(
                new URL("file:/some/path/classes/"),
                new URL("jar:file:/other/lib/some.jar!/"),
                new URL("file:/more/classes/"),
                new URL("jar:file:/my/.m2/repo/greatlib.jar!/"),
                new URL("file:/some/bootstrap/classes/"),
                new URL("jar:file:/more/bootstrap/bootlib.jar!/")
        );
    }

    @Test
    public void resolves_resiliently_from_system_property() {
        System.clearProperty(JAVA_BOOT_PATH_PROP);
        System.clearProperty(JAVA_CLASS_PATH_PROP);

        assertThat(UrlSource.From.classPathSystemProperties()).isEmpty();
    }

    @Test
    public void returns_unique_urls() {
        URL url = getClass().getResource(".");
        ImmutableList<URL> redundantInput = ImmutableList.of(url, url);

        UrlSource source = UrlSource.From.iterable(redundantInput);

        assertThat(source).hasSize(1).containsOnly(url);
    }

    private String createClassPathProperty(String... paths) {
        return Joiner.on(File.pathSeparatorChar).join(paths);
    }
}