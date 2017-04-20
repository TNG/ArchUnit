package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import java.io.File;

public class VisualizerTest {
    @Test
    public void name() throws Exception {
        System.out.println("Starting test...");
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual",
                "java.io", "com.google.common.io");
        new Visualizer().visualize(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()), "foo"),
                new VisualizationContext.Builder()
                        .includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io")
                        .build());
        //FIXME: die bei includeOnly angegebenen Pfade, die nicht in den importierten Packages enthalten sind,
        // sollen ignoriert werden
    }
}