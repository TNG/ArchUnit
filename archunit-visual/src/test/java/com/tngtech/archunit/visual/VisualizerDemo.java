package com.tngtech.archunit.visual;

import java.io.File;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Test;

public class VisualizerDemo {
    @Test
    public void build_report() throws Exception {
        System.out.println("Building example report...");

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual",
                "java.io", "com.google.common.io");

        EvaluationResult evaluationResult = ArchRuleDefinition.noClasses().should().callMethod(Object.class, "toString").evaluate(classes);

        new Visualizer().visualize(classes, evaluationResult,
                new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "example-report"),
                VisualizationContext.includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io"));
    }
}