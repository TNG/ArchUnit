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

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ComponentCouplingMetricsTest {

    @Test
    public void create_PlantUml_diagram() throws IOException {
        JavaClasses javaClasses = new ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackagesOf(PublicAPI.class);
        Set<JavaPackage> subpackages = javaClasses.getPackage("com.tngtech.archunit").getSubpackages();

        ComponentCouplingMetrics couplingMetrics = ComponentCouplingMetrics.of(MetricsComponentFactory.fromPackages(subpackages));

        Files.write(Paths.get("coupling.puml"), couplingMetrics.toPlantUmlDiagram().render().getBytes(UTF_8));
    }
}
