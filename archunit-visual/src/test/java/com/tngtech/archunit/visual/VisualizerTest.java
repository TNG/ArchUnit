package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import org.junit.Test;

import java.io.File;

public class VisualizerTest {
    @Test
    public void name() throws Exception {
        JavaClasses classes = new ClassFileImporter().importUrl(new File(Visualizer.class.getResource(
                "/" + VisualizerTest.class.getName().replace('.', '/') + ".class")
                .getFile()).getParentFile().toPath().toUri().toURL());
        new Visualizer().visualize(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()), "foo"),
                new VisualizationContextBuilder()
                        .ignoreAccessToSuperConstructor()
                        .includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io")
                        .build());
    }
}