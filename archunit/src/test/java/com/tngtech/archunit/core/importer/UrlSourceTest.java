package com.tngtech.archunit.core.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Joiner;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlSourceTest {
    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void resolves_from_system_property() throws MalformedURLException {
        String classPath = createClassPathProperty(
                "/some/path/classes", "/other/lib/some.jar", "/more/classes", "/my/.m2/repo/greatlib.jar");
        System.setProperty("java.class.path", classPath);

        UrlSource urlSource = UrlSource.From.classPathSystemProperty();

        assertThat(urlSource).containsOnly(
                new URL("file:/some/path/classes/"),
                new URL("jar:file:/other/lib/some.jar!/"),
                new URL("file:/more/classes/"),
                new URL("jar:file:/my/.m2/repo/greatlib.jar!/")
        );
    }

    private String createClassPathProperty(String... paths) {
        return Joiner.on(File.pathSeparatorChar).join(paths);
    }
}