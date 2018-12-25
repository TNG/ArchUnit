package com.tngtech.archunit.htmlvisualization;

import java.io.File;
import java.io.IOException;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.htmlvisualization.testjson.structure.complexinherit.ComplexClass2;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.tngtech.archunit.htmlvisualization.ResourcesUtils.getTextOfFile;
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
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.htmlvisualization.testjson.structure");

        new Visualizer(classes, visualizationOutput).visualize();

        assertThat(getTextOfFile(visualizationOutput)).contains("\"com.tngtech.archunit.htmlvisualization.testjson.structure\"");
    }

    @Test
    public void visualization_of_classes_with_violations() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.htmlvisualization.testjson.structure");
        ArchRule rule = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass2.class, "sayHelloAndBye");
        EvaluationResult evaluationResult = rule.evaluate(classes);

        new Visualizer(classes, visualizationOutput).visualize(singletonList(evaluationResult));

        String fileContent = getTextOfFile(visualizationOutput);
        assertThat(fileContent).contains("\"com.tngtech.archunit.htmlvisualization.testjson.structure\"");
        assertThat(fileContent).contains("\"no classes should call method ComplexClass2.sayHelloAndBye()\"");
    }
}
