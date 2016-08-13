package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.lang.DescribedIterable;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaClasses implements DescribedIterable<JavaClass>, Restrictable<JavaClass, JavaClasses> {
    private final ImmutableMap<Class<?>, JavaClass> classes;
    private final String description;

    JavaClasses(Map<Class<?>, JavaClass> classes) {
        this(classes, "classes");
    }

    JavaClasses(Map<Class<?>, JavaClass> classes, String description) {
        this.classes = ImmutableMap.copyOf(classes);
        this.description = description;
    }

    @Override
    public JavaClasses that(DescribedPredicate<JavaClass> predicate) {
        Map<Class<?>, JavaClass> matchingElements = Guava.Maps.filterValues(classes, predicate);
        String newDescription = String.format("%s that %s", description, predicate.getDescription());
        return new JavaClasses(matchingElements, newDescription);
    }

    public JavaClasses as(String description) {
        return new JavaClasses(classes, description);
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
        return classes.values().iterator();
    }

    public boolean contain(Class<?> reflectedType) {
        return classes.containsKey(reflectedType);
    }

    public JavaClass get(Class<?> reflectedType) {
        return checkNotNull(classes.get(reflectedType), "%s don't contain %s of type %s",
                getClass().getSimpleName(), JavaClass.class.getSimpleName(), reflectedType.getName());
    }

    public static JavaClasses of(Iterable<JavaClass> classes) {
        Map<Class<?>, JavaClass> mapping = new HashMap<>();
        for (JavaClass clazz : classes) {
            mapping.put(clazz.reflect(), clazz);
        }
        return new JavaClasses(mapping);
    }

    static JavaClasses of(Map<Class<?>, JavaClass> classes, ClassFileImportContext importContext) {
        CompletionProcess completionProcess = new CompletionProcess(importContext);
        for (JavaClass clazz : new JavaClasses(classes)) {
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
            classCompletionProcesses.add(clazz.completeFrom(context));
        }

        public void finish() {
            AccessCompletion.TopProcess accessCompletionProcess = new AccessCompletion.TopProcess();
            for (JavaClass.CompletionProcess process : classCompletionProcesses) {
                accessCompletionProcess.mergeWith(process.completeCodeUnitsFrom(context));
            }
            accessCompletionProcess.finish();
        }
    }
}
