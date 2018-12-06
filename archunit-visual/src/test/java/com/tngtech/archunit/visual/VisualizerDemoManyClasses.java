package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.jar.JarFile;

public class VisualizerDemoManyClasses {
    @Ignore
    @Test
    public void build_report() throws IOException {
        System.out.println("Building large example report...");

        JavaClasses classes = new ClassFileImporter().importJar(new JarFile(ResourcesUtils.getResource("/exampleProjects/hibernate-core-5.2.17.Final.jar")));
        ArchRule rule1 = ArchRuleDefinition.noClasses().should().callMethod(Object.class, "toString");
        EvaluationResult evaluationResult1 = rule1.evaluate(classes);

        new Visualizer(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "example-report-many-classes")
        ).visualize(Arrays.asList(evaluationResult1));
    }
}