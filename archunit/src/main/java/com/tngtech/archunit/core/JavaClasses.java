package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.Iterables.filter;

public class JavaClasses implements Iterable<JavaClass>, Restrictable<JavaClass, JavaClasses>, HasDescription {
    private final Set<JavaClass> classes;
    private final String description;

    JavaClasses(Collection<JavaClass> classes) {
        this(classes, "classes");
    }

    JavaClasses(Collection<JavaClass> classes, String description) {
        this.classes = ImmutableSet.copyOf(classes);
        this.description = description;
    }

    @Override
    public JavaClasses that(DescribedPredicate<JavaClass> predicate) {
        Set<JavaClass> matchingElements = ImmutableSet.copyOf(filter(classes, predicate));
        String newDescription = predicate.getDescription().or(description);
        return new JavaClasses(matchingElements, newDescription);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{classes=" + classes + '}';
    }

    @Override
    public Iterator<JavaClass> iterator() {
        return classes.iterator();
    }

    static JavaClasses of(Collection<JavaClass> classes, ClassFileImportContext importContext) {
        CompletionProcess completionProcess = new CompletionProcess(importContext);
        for (JavaClass clazz : classes) {
            completionProcess.completeClass(clazz);
        }
        completionProcess.finish();
        return new JavaClasses(classes);
    }

    private static class CompletionProcess {
        private final Set<JavaClass.CompletionProcess> classCompletionProcesses = new HashSet<>();
        private final ClassFileImportContext context;

        public CompletionProcess(ClassFileImportContext context) {
            this.context = context;
        }

        void completeClass(JavaClass clazz) {
            classCompletionProcesses.add(clazz.completeClassHierarchyFrom(context));
        }

        public void finish() {
            for (JavaClass.CompletionProcess process : classCompletionProcesses) {
                process.completeMethodsFrom(context);
            }
        }
    }
}
