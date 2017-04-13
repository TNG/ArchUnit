package com.tngtech.archunit.visual;

import java.io.File;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

public class VisualizerTest {
    @Test
    public void name() throws Exception {
        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.visual");
        new Visualizer().visualize(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()), "foo"),
                new VisualizationContext.Builder()
                        .ignoreAccessToSuperConstructor()
                        .includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io")
                        .build());
    }
}