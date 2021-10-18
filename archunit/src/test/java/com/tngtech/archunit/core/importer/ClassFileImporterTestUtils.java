package com.tngtech.archunit.core.importer;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;
import static com.tngtech.archunit.testutil.TestUtils.urlOf;

class ClassFileImporterTestUtils {

    static <T extends HasName> Set<T> getByName(Iterable<T> thingsWithName, String name) {
        Set<T> result = new HashSet<>();
        for (T hasName : thingsWithName) {
            if (name.equals(hasName.getName())) {
                result.add(hasName);
            }
        }
        return result;
    }

    static <T extends HasName> T findAnyByName(Iterable<T> thingsWithName, String name) {
        T result = getFirst(getByName(thingsWithName, name), null);
        return checkNotNull(result, "No object with name '" + name + "' is present in " + thingsWithName);
    }

    static Set<JavaField> getFields(Iterable<JavaClass> classes) {
        Set<JavaField> fields = new HashSet<>();
        for (JavaClass clazz : classes) {
            fields.addAll(clazz.getFields());
        }
        return fields;
    }

    static Set<JavaMethod> getMethods(Iterable<JavaClass> classes) {
        Set<JavaMethod> methods = new HashSet<>();
        for (JavaClass clazz : classes) {
            methods.addAll(clazz.getMethods());
        }
        return methods;
    }

    static Set<JavaCodeUnit> getCodeUnits(Iterable<JavaClass> classes) {
        Set<JavaCodeUnit> codeUnits = new HashSet<>();
        for (JavaClass clazz : classes) {
            codeUnits.addAll(clazz.getCodeUnits());
        }
        return codeUnits;
    }

    static JarFile jarFileOf(Class<?> clazzInJar) throws IOException {
        URLConnection connection = urlOf(clazzInJar).openConnection();
        checkArgument(connection instanceof JarURLConnection, "Class %s is not contained in a JAR", clazzInJar.getName());
        return ((JarURLConnection) connection).getJarFile();
    }
}
