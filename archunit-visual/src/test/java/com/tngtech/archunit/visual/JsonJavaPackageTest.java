package com.tngtech.archunit.visual;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonJavaPackageTest {

    @Test
    public void testInsertPackage() throws Exception {
        Method insertPackage = getInsertPackage();

        JsonJavaPackage pkg = new JsonJavaPackage("com", "com", false);

        insertPackage.invoke(pkg, "com.tngtech");
        File expectedJson = JsonConverter.getJsonFile("/testinsertpackage1.json");
        assertThat(JsonConverter.jsonToMap(JsonConverter.getJsonStringOf(pkg)))
                .as("structure after inserting a single package")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));

        insertPackage.invoke(pkg, "com.tngtech.pkg.subpkg");
        expectedJson = JsonConverter.getJsonFile("/testinsertpackage2.json");
        assertThat(JsonConverter.jsonToMap(JsonConverter.getJsonStringOf(pkg)))
                .as("structure after inserting a package with sub-package")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalize() throws Exception {
        Method insertPackage = getInsertPackage();

        JsonJavaPackage pkg = new JsonJavaPackage("defaultroot", "defaultroot", true);
        insertPackage.invoke(pkg, "com");
        pkg.normalize();

        File expectedJson = JsonConverter.getJsonFile("/testnormalize1.json");
        assertThat(JsonConverter.jsonToMap(JsonConverter.getJsonStringOf(pkg)))
                .as("structure after normalizing with unnecessary default package and another package")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));

        pkg = new JsonJavaPackage("com", "com", false);
        insertPackage.invoke(pkg, "com.tngtech.pkg1.subpkg");
        insertPackage.invoke(pkg, "com.tngtech.pkg2.subpkg2");
        pkg.insertJavaElement(new JsonJavaClazz("class1", "com.tngtech.pkg1.class1", "class", ""));
        pkg.normalize();

        expectedJson = JsonConverter.getJsonFile("/testnormalize2.json");
        assertThat(JsonConverter.jsonToMap(JsonConverter.getJsonStringOf(pkg)))
                .as("structure after normalizing with several packages with only one child")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    private Method getInsertPackage() throws NoSuchMethodException {
        Method insertPackage = JsonJavaPackage.class.getDeclaredMethod("insertPackage", String.class);
        insertPackage.setAccessible(true);
        return insertPackage;
    }
}