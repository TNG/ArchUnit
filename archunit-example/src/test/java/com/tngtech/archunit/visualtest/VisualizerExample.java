package com.tngtech.archunit.visualtest;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.visual.Visualizer;

public class VisualizerExample {
    public static void main(String[] args) {
        JavaClasses classes = new ClassFileImporter().importUrl(VisualizerExample.class.getResource("/com/tngtech/archunit/example"));

        new Visualizer(classes).write();
    }
}
