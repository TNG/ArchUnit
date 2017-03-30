package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

public class VisualizerTest {
    @Test
    public void name() throws Exception {
        URL url1 = new File(Visualizer.class.getResource(
                "/" + VisualizerTest.class.getName().replace('.', '/') + ".class")
                .getFile()).getParentFile().toPath().toUri().toURL();
        URL url2 = new File(Visualizer.class.getResource(
                "/" + Visualizer.class.getName().replace('.', '/') + ".class")
                .getFile()).getParentFile().toPath().toUri().toURL();
        JavaClasses classes = new ClassFileImporter().importUrls(Arrays.asList(url1, url2));
        new Visualizer().visualize(classes,
                new File(new File(Visualizer.class.getResource("/").getFile()), "foo"),
                new VisualizationContextBuilder()
                        .ignoreAccessToSuperConstructor()
                        .includeOnly("com.tngtech.archunit.visual", "java.io.File", "com.google.common.io")
                        .build());
    }
}