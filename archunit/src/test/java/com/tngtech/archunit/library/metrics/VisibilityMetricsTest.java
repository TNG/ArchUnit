package com.tngtech.archunit.library.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.metrics.components.MetricsComponentFactory;
import org.junit.Test;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class VisibilityMetricsTest {

    @Test
    public void create_visibility_table_from_classes() throws IOException {
        Set<JavaPackage> packages = new ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackagesOf(PublicAPI.class)
                .getPackage("com.tngtech.archunit").getSubpackages();

        VisibilityMetrics visibilityMetrics = VisibilityMetrics.of(MetricsComponentFactory.fromPackages(packages));

        Files.write(Paths.get("visibility.adoc"), visibilityMetrics.toAsciiDocTable().render().getBytes(UTF_8));
    }
}
