package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.visual.testjson.structure.complexinherit.ComplexClass1;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class VisualizerDemo {
    @Test
    public void build_report() throws Exception {
        System.out.println("Building example report...");

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual",
                "java.io", "com.google.common.io");

        ArchRule rule1 = ArchRuleDefinition.noClasses().should().callMethod(Object.class, "toString");
        EvaluationResult evaluationResult1 = rule1.evaluate(classes);

        ArchRule rule2 = ArchRuleDefinition.noClasses().should().callMethod(ComplexClass1.class, "sayHello");
        EvaluationResult evaluationResult2 = rule2.evaluate(classes);

        new Visualizer(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "example-report")
                //VisualizationContext.includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io"));
        ).visualize(Arrays.asList(evaluationResult1, evaluationResult2), false);
    }
}