package com.tngtech.archunit.visual;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class JsonJavaPackageTest {

    @Test
    public void testInsertPackage() throws Exception {
        JsonJavaPackage rootPackage = new JsonJavaPackage("com", "com");
        rootPackage.insertPackage("com.tngtech");
        File expectedJson = JsonTestUtils.getJsonFile("/testinsertpackage1.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a single package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));

        rootPackage.insertPackage("com.tngtech.pkg.subpkg");
        expectedJson = JsonTestUtils.getJsonFile("/testinsertpackage2.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a package with sub-package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testInsert() throws Exception {
        JsonJavaPackage rootPackage = new JsonJavaPackage("com", "com");
        rootPackage.insertPackage("com.tngtech.pkg");
        rootPackage.insert(JsonTestUtils.createJsonJavaClass("class1", "com.tngtech.class1"));
        rootPackage.insert(JsonTestUtils.createJsonJavaClass("class2", "com.tngtech.pkg.class2"));
        rootPackage.insert(JsonTestUtils.createJsonJavaClass("class3", "com.tngtech.notExistingPkg"));

        File expectedJson = JsonTestUtils.getJsonFile("/testinsert.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting two classes")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalizeDefaultRootPackage() throws Exception {
        Method createDefaultPackage = getPrivateMethod(JsonJavaPackage.class, "createDefaultPackage");
        JsonJavaPackage pkg = (JsonJavaPackage) createDefaultPackage.invoke(null);
        pkg.insertPackage("com");
        pkg.normalize();

        File expectedJson = JsonTestUtils.getJsonFile("/testnormalize1.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with unnecessary default package and another package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalize() throws Exception {
        JsonJavaPackage pkg = new JsonJavaPackage("com", "com");
        pkg.insertPackage("com.tngtech.pkg1.subpkg");
        pkg.insertPackage("com.tngtech.pkg2.subpkg2");
        pkg.insert(JsonTestUtils.createJsonJavaClass("class1", "com.tngtech.pkg1.class1"));
        pkg.normalize();

        File expectedJson = JsonTestUtils.getJsonFile("/testnormalize2.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with several packages")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JsonJavaPackage javaPackage = new JsonJavaPackage("pkg", "com.tngtech.pkg");
        javaPackage.insertPackage("com.tngtech.pkg.subpkg");
        javaPackage.insert(JsonTestUtils.createJsonJavaClass("Class1", "com.tngtech.pkg.Class1"));


        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(javaPackage.getChild("com.tngtech.pkg"))).contains("com.tngtech.pkg");
        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(javaPackage.getChild("com.tngtech.pkg.subpkg"))).contains("com.tngtech.pkg.subpkg");
        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(javaPackage.getChild("com.tngtech.pkg.Class1"))).contains("com.tngtech.pkg.Class1");
        JsonTestUtils.assertThatOptional(javaPackage.getChild("com.tngtech.pkg.NotExisting")).isAbsent();
    }

    @Test
    public void testCreatePackage() throws Exception {
        Method createPackage = getPrivateMethod(JsonJavaPackage.class, "createPackage", String.class, String.class, boolean.class);

        JsonJavaPackage act = (JsonJavaPackage) createPackage.invoke(null, "com.tngtech", "com", false);
        assertTrue(act.fullName + "-" + act.name, //"creating new package not working for one subpackage"
                hasNameAndFullName(act, "tngtech", "com.tngtech"));

        act = (JsonJavaPackage) createPackage.invoke(null, "com.tngtech.pkg.subpkg", "com", false);
        assertTrue("creating new package not working for several subpackages",
                hasNameAndFullName(act, "tngtech", "com.tngtech"));

        act = (JsonJavaPackage) createPackage.invoke(null, "com.tngtech", "default", true);
        assertTrue("creating new package not working for one subpackage and default root",
                hasNameAndFullName(act, "com", "com"));
    }

    @Test
    public void testCreatePackageStructure() throws Exception {
        Set<String> pkgs = new HashSet<>(Arrays.asList("com.tngtech.pkg1", "com.tngtech.pkg1.subpkg1",
                "com.tngtech.pkg2", "java.lang"));
        JsonJavaPackage act = JsonJavaPackage.createPackageStructure(pkgs);
        File expectedJson = JsonTestUtils.getJsonFile("/testcreatepackagestructure.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    private boolean hasNameAndFullName(JsonJavaPackage pkg, String name, String fullName) {
        return pkg.name.equals(name) && pkg.fullName.equals(fullName);
    }

    private Method getPrivateMethod(Class clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}