package com.tngtech.archunit.visual;

import java.io.File;
import java.io.IOException;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass2;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.visual.ResourcesUtils.getTextOfFile;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class VisualizerTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File visualizationOutput;

    @Before
    public void configureOutputFile() throws IOException {
        visualizationOutput = temporaryFolder.newFile("visualization.html");
    }

    @Test
    public void visualization_of_classes_without_passing_violations() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");

        new Visualizer(classes, visualizationOutput).visualize();

        assertThat(getTextOfFile(visualizationOutput)).contains("\"com.tngtech.archunit.visual.testjson.structure\"");
    }

    @Test
    public void visualization_of_classes_with_violations() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual.testjson.structure");
        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
        EvaluationResult evaluationResult = rule.evaluate(classes);

        new Visualizer(classes, visualizationOutput).visualize(singletonList(evaluationResult));

        String fileContent = getTextOfFile(visualizationOutput);
        assertThat(fileContent).contains("\"com.tngtech.archunit.visual.testjson.structure\"");
        assertThat(fileContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }
}
