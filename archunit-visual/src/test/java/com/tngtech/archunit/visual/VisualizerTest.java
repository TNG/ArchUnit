package com.tngtech.archunit.visual;

import java.io.File;

import com.tngtech.archunit.core.ClassFileImporter;
import org.junit.Test;

public class VisualizerTest {
    @Test
    public void name() {
        new Visualizer().visualize(new ClassFileImporter().importUrl(Visualizer.class.getResource(
                "/" + VisualizerTest.class.getPackage().getName().replace('.', '/'))),
                new File(new File(Visualizer.class.getResource("/").getFile()), "foo"));

    }
}