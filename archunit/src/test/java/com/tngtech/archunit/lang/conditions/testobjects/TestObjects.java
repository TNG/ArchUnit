package com.tngtech.archunit.lang.conditions.testobjects;

import java.nio.file.Paths;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public class TestObjects {
    private static final JavaClasses testClasses = importTestClasses();

    private static JavaClasses importTestClasses() {
        try {
            return new ClassFileImporter().importPath(Paths.get(TestObjects.class.getResource(".").toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestObjects() {
    }

    public static final JavaClass CALLER_CLASS = findClass(testClasses, CallerClass.class);
    public static final JavaClass TARGET_CLASS = findClass(testClasses, TargetClass.class);

    private static JavaClass findClass(JavaClasses classes, Class<?> type) {
        for (JavaClass clazz : classes) {
            if (clazz.reflect() == type) {
                return clazz;
            }
        }
        throw new RuntimeException("Couldn't find test class with type " + type);
    }

}
