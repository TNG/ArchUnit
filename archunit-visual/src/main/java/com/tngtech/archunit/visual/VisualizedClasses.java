package com.tngtech.archunit.visual;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import java.util.HashMap;
import java.util.Map;

public class VisualizedClasses {
    private static final String INNER_CLASS_SEPARATOR = "$";

    private Map<String, JavaClass> classes = new HashMap<>();
    private Map<String, JavaClass> innerClasses = new HashMap<>();
    private Map<String, JavaClass> dependencies = new HashMap<>();

    public VisualizedClasses(JavaClasses classes, VisualizationContext context) {
        addClasses(classes, context);
        addDependencies(context);
    }

    private void addClasses(JavaClasses classes, VisualizationContext context) {
        for (JavaClass c : classes) {
            if (context.isElementIncluded(c.getName())) {
                if (c.getName().contains(INNER_CLASS_SEPARATOR)) {
                    innerClasses.put(c.getName(), c);
                } else if (!c.getSimpleName().isEmpty()) {
                    this.classes.put(c.getName(), c);
                }
            }
        }
    }

    private void addDependencies(VisualizationContext context) {
        for (JavaClass c : classes.values()) {
            if (context.isElementIncluded(c.getName())) {
                for (Dependency dep : c.getDirectDependencies()) {
                    if (context.isElementIncluded(dep.getTargetClass().getName()) &&
                            !classes.keySet().contains(dep.getTargetClass().getName()) &&
                            !innerClasses.keySet().contains(dep.getTargetClass().getName())) {
                        dependencies.put(dep.getTargetClass().getName(), dep.getTargetClass());
                    }
                }
            }
        }
    }

    Iterable<JavaClass> getClasses() {
        return classes.values();
    }

    Iterable<JavaClass> getInnerClasses() {
        return innerClasses.values();
    }

    Iterable<JavaClass> getDependencies() {
        return dependencies.values();
    }

    Iterable<JavaClass> getAll() {
        return Iterables.concat(classes.values(), innerClasses.values(), dependencies.values());
    }

    static VisualizedClasses from(JavaClasses classes, VisualizationContext context) {
        return new VisualizedClasses(classes, context);
    }
}
