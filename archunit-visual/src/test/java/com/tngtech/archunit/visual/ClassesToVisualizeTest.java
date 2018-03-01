package com.tngtech.archunit.visual;

import java.io.File;
import java.util.*;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testclasses.OtherClass;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.SomeInterface;
import com.tngtech.archunit.visual.testclasses.ThirdClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SecondSubPkgClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import com.tngtech.archunit.visual.testclasses.subpkg.ThirdSubPkgClass;
import com.tngtech.archunit.visual.testdependencies.TestDependencyClass;
import com.tngtech.archunit.visual.testdependencies.TestDependencyClassWithInnerClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassesToVisualizeTest {
    private static final Class<?> SUB_PKG_CLASS_DEPENDENCY = File.class;
    private static final Set<Class<?>> EXTRA_DEPENDENCIES_OF_TEST_CLASSES = ImmutableSet.of(
            Object.class, String.class, SUB_PKG_CLASS_DEPENDENCY, TestDependencyClass.class,
            TestDependencyClassWithInnerClass.TestDependencyInnerClass.class);

    @Test
    public void contain_non_filtered_classes() {
        JavaClasses classes = new ClassFileImporter().importPackages(SomeClass.class.getPackage().getName());

        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, VisualizationContext.everything());
        Iterable<String> expected = Iterables.concat(namesOf(classes), namesOf(EXTRA_DEPENDENCIES_OF_TEST_CLASSES));
        assertThat(namesOf(classesToVisualize.getAll())).containsOnlyElementsOf(expected);

        classesToVisualize = ClassesToVisualize.from(classes,
                VisualizationContext.includeOnly(SubPkgClass.class.getPackage().getName()));
        assertThat(classesToVisualize.getAll()).doesNotContain(classes.get(SomeClass.class));
        assertThat(namesOf(classesToVisualize.getAll())).doesNotContain(SUB_PKG_CLASS_DEPENDENCY.getName());
    }

    @Test
    public void contain_classes_in_order_of_inner_class_depth() {
        JavaClasses classes = new ClassFileImporter().importPackages(SomeClass.class.getPackage().getName());

        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, VisualizationContext.everything());
        SortedMap<Integer, ImmutableSet<JavaClass>> sortedMap = getClassesSortedByDepth(classes);

        Iterator<JavaClass> iterator = classesToVisualize.getClasses().iterator();
        JavaClass currentJavaClass = iterator.next();
        while (iterator.hasNext()) {
            JavaClass nextJavaClass = iterator.next();
            assertThat(getDepthOfClass(currentJavaClass, sortedMap)).isLessThanOrEqualTo(getDepthOfClass(nextJavaClass, sortedMap));
            currentJavaClass = nextJavaClass;
        }
    }

    @Test
    public void contain_dependencies() {
        JavaClasses classes = new ClassFileImporter().importPackages(SomeClass.class.getPackage().getName());

        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, VisualizationContext.everything());

        assertThat(namesOf(classesToVisualize.getDependenciesClasses()))
                .containsOnlyElementsOf(namesOf(EXTRA_DEPENDENCIES_OF_TEST_CLASSES));
    }

    @Test
    public void contain_packages() {
        JavaClasses classes = new ClassFileImporter().importPackages(SomeClass.class.getPackage().getName());

        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, VisualizationContext.everything());

        Set<String> expectedPackages = Sets.union(
                ImmutableSet.of(SomeClass.class.getPackage().getName(), SubPkgClass.class.getPackage().getName()),
                packagesOf(EXTRA_DEPENDENCIES_OF_TEST_CLASSES));
        assertThat(classesToVisualize.getPackages()).containsOnlyElementsOf(expectedPackages);
    }

    private Set<String> namesOf(Set<Class<?>> classes) {
        return ImmutableSet.copyOf(JavaClass.namesOf(classes.toArray(new Class<?>[classes.size()])));
    }

    private Iterable<String> namesOf(Iterable<JavaClass> all) {
        Set<String> result = new HashSet<>();
        for (JavaClass javaClass : all) {
            result.add(javaClass.getName());
        }
        return result;
    }

    private Set<String> packagesOf(Set<Class<?>> classes) {
        Set<String> result = new HashSet<>();
        for (Class<?> clazz : classes) {
            result.add(clazz.getPackage().getName());
        }
        return result;
    }

    private int getDepthOfClass(JavaClass javaClass, SortedMap<Integer, ImmutableSet<JavaClass>> classesSortedByDepth) {
        int i = 0;
        for (ImmutableSet<JavaClass> set : classesSortedByDepth.values()) {
            if (set.contains(javaClass)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private SortedMap<Integer, ImmutableSet<JavaClass>> getClassesSortedByDepth(JavaClasses classes){
        SortedMap<Integer, ImmutableSet<JavaClass>> map = new TreeMap<>();
        map.put(0, ImmutableSet.of(
                classes.get(SomeClass.class), classes.get(OtherClass.class), classes.get(ThirdClass.class),
                classes.get(SubPkgClass.class), classes.get(SecondSubPkgClass.class), classes.get(ThirdSubPkgClass.class),
                classes.get(SomeInterface.class)));
        map.put(1, ImmutableSet.of(
                classes.get(SomeClass.InnerClass.class), classes.get(SubPkgClass.InnerSubPkgClass.class),
                classes.get(SomeInterface.InnerInterface.class), classes.get(ThirdSubPkgClass.InnerClass1.class)));
        map.put(2, ImmutableSet.of(classes.get(ThirdSubPkgClass.InnerClass1.InnerClass2.class)));
        map.put(3, ImmutableSet.of(classes.get(ThirdSubPkgClass.InnerClass1.InnerClass2.InnerClass3.class)));
        return map;
    }
}