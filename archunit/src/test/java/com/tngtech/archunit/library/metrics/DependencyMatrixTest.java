package com.tngtech.archunit.library.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.metrics.components.MetricsComponentFactory;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DependencyMatrixTest {
    @Test
    public void creates_AsciiDoc_table() throws IOException {
        JavaClasses javaClasses = new ClassFileImporter().importPackagesOf(PublicAPI.class);
        Set<JavaPackage> subpackages = javaClasses.getPackage("com.tngtech.archunit").getSubpackages();

        DependencyMatrix dependencyMatrix = DependencyMatrix.of(MetricsComponentFactory.fromPackages(subpackages));

        Files.write(Paths.get("dependencies.adoc"), dependencyMatrix.toAsciiDocTable().render().getBytes(UTF_8));
    }
}
