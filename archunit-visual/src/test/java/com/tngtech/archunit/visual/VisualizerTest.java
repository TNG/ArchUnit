package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class VisualizerTest {
    @Test
    public void testVisualizeFromFilesWithoutEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        outputDir.delete();
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize();
        File file = new File(outputDir, "report.html");
        assertThat(file).exists();
        assertThat(ResourcesUtils.getStringOfFile(file)).contains("\"com.tngtech.archunit.visual.testjson.structure\"");
    }

    @Test
    public void testVisualizeFromFilesWithEvaluationResult() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        File outputDir = new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "visualizer-file-test");
        outputDir.delete();
        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
        EvaluationResult evaluationResult = rule.evaluate(classes);
        new Visualizer(classes, outputDir,
                VisualizationContext.includeOnly("com.tngtech.archunit.visual.testjson.structure")).visualize(Arrays.asList(evaluationResult));
        File file = new File(outputDir, "report.html");
        assertThat(file).exists();
        String fileContent = ResourcesUtils.getStringOfFile(file);
        assertThat(fileContent).contains("\"com.tngtech.archunit.visual.testjson.structure\"");
        assertThat(fileContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }
}
