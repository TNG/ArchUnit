package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class VisualizerTest {
    @Test
    public void testVisualizeFromFilesWithoutEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");

        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        new Visualizer().visualize(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure"));
        File jsonFile = new File(outputDir, "classes.json");
        assertThat(jsonFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(JsonTestUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
    }

    @Test
    public void testVisualizeFromJarWithoutEvaluationResult() throws IOException {
        JavaClasses classes = new ClassFileImporter().importJar(new JarFile(JsonTestUtils.getJsonFile("/TestJson.jar")));

        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-jar-test");
        new Visualizer().visualize(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure"));
        File jsonFile = new File(outputDir, "classes.json");
        assertThat(jsonFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(JsonTestUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
    }
}
