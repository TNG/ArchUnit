package com.tngtech.archunit.example;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class ModulePathTest {

    @Test
    public void can_find_class_on_the_modulepath() throws Exception {
        assertNotOnClasspath(SomeClass.class);

        JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

        assertNotNull(classes.get(SomeClass.class));
    }

    @SuppressWarnings("unchecked")
    private void assertNotOnClasspath(Class<?> clazz) throws Exception {
        Class<?> urlSource = Class.forName("com.tngtech.archunit.core.importer.UrlSource$From");
        Method classPathSystemProperties = urlSource.getDeclaredMethod("classPathSystemProperties");
        classPathSystemProperties.setAccessible(true);
        Iterable<URL> classpathUrls = (Iterable<URL>) classPathSystemProperties.invoke(null);
        URL[] classpath = stream(classpathUrls.spliterator(), false).toArray(URL[]::new);
        URLClassLoader classpathClassLoader = new URLClassLoader(classpath, null);
        try {
            classpathClassLoader.loadClass(clazz.getName());
            fail("Class " + clazz.getName() + " can be loaded from the classpath -> the test can't reliably show us, if this class can be found on the modulepath. "
                    + "The reason for this might be a wrongly configured test environment, like IntelliJ does "
                    + "(throwing everything on the classpath instead of setting up the modulepath). "
                    + "Check if this test runs correctly via Gradle!");
        } catch (ClassNotFoundException ignored) {
        }
    }
}