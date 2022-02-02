package com.tngtech.archunit.core.importer;

import java.net.URL;

public class UrlSourceTestUtils {
    public static Iterable<URL> getClasspath() {
        return UrlSource.From.classPathSystemProperties();
    }
}
