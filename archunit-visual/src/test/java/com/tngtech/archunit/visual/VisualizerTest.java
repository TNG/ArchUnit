package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import org.assertj.core.api.Condition;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testVisualizeFromFilesWithEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");

        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
        EvaluationResult evaluationResult = rule.evaluate(classes);
        new Visualizer().visualize(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure"), Arrays.asList(evaluationResult));
        File jsonFile = new File(outputDir, "classes.json");
        File violationFile = new File(outputDir, "violations.json");
        assertThat(jsonFile).exists();
        assertThat(violationFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(JsonTestUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
        assertThat(JsonTestUtils.jsonToMapArray(violationFile))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                });
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
