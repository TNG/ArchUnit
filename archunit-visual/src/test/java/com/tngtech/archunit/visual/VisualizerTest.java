package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass1;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import org.assertj.core.api.Condition;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

public class VisualizerTest {
    @Test
    public void testVisualizeFromFilesWithoutEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        outputDir.delete();
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize();
        File jsonFile = new File(outputDir, "classes.json");
        assertThat(jsonFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(ResourcesUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
    }

    @Test
    public void testVisualizeFromFilesWithEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        outputDir.delete();
        File violationFile = new File(outputDir, "violations.json");
        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
        EvaluationResult evaluationResult = rule.evaluate(classes);
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize(Arrays.asList(evaluationResult), true);
        File jsonFile = new File(outputDir, "classes.json");
        assertThat(jsonFile).exists();
        assertThat(violationFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(ResourcesUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
        assertThat(ResourcesUtils.jsonToMapArray(violationFile))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                });
    }

    @Test
    public void testVisualizeWithExistingViolationFileAndNewEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        outputDir.mkdir();
        File violationFile = new File(outputDir, "violations.json");
        try {
            violationFile.createNewFile();
            Files.copy(ResourcesUtils.getResource("testjson/violation/existing-violations.json").toPath(), violationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass1.class, "sayHi");
        EvaluationResult evaluationResult = rule.evaluate(classes);
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize(Arrays.asList(evaluationResult), false);
        assertThat(violationFile).exists();
        assertThat(ResourcesUtils.jsonToMapArray(violationFile))
                .as("violations")
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass2.sayHelloAndBye()");
                    }
                })
                .areAtLeastOne(new Condition<Map<Object, Object>>() {
                    @Override
                    public boolean matches(Map<Object, Object> value) {
                        return value.containsValue("no classes should call method ComplexClass1.sayHi()");
                    }
                });
    }

    @Test
    public void testVisualizeFromJarWithoutEvaluationResult() throws IOException {
        JavaClasses classes = new ClassFileImporter().importJar(new JarFile(ResourcesUtils.getResource("/TestJson.jar")));

        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-jar-test");
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize();
        File jsonFile = new File(outputDir, "classes.json");
        assertThat(jsonFile).exists();
        assertThat(new File(outputDir, "report.html")).exists();
        assertThat(ResourcesUtils.jsonToMap(jsonFile))
                .as("created package structure")
                .containsValue("com.tngtech.archunit.visual.testjson.structure");
    }
}
