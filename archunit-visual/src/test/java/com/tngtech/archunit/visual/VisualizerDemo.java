package com.tngtech.archunit.visual;

import java.io.File;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

//@Ignore
public class VisualizerDemo {
    @Test
    public void build_report() throws Exception {
        System.out.println("Building example report...");

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual",
                "java.io", "com.google.common.io");

        new Visualizer().visualize(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()).getParentFile().getParentFile(), "example-report"),
                new VisualizationContext.Builder()
                        .includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io")
                        .build());
    }
}