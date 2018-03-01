package com.tngtech.archunit.visual;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.visual.JsonTestUtils.assertThatOptional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

//FIXME: remove ignore again
@Ignore
public class JsonJavaPackageTest {
    private static final JavaClasses classes = new ClassFileImporter().importPackagesOf(SomeClass.class);

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
        rootPackage.insertPackage(SubPkgClass.class.getPackage().getName());
        rootPackage.insert(new JsonJavaClass(classes.get(SomeClass.class), false));
        rootPackage.insert(new JsonJavaClass(classes.get(SubPkgClass.class), false));
        rootPackage.insert(new JsonJavaClass(importClassWithContext(File.class), false));

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
        List<String> packageParts = Splitter.on(".").splitToList(classes.get(SomeClass.class).getPackage());
        for (int i = 1; i < packageParts.size(); i++) {
            pkg.insertPackage(Joiner.on(".").join(packageParts.subList(0, i + 1)));
        }
        pkg.insert(new JsonJavaClass(classes.get(SomeClass.class), false));
        pkg.insertPackage(classes.get(SubPkgClass.class).getPackage());
        pkg.insert(new JsonJavaClass(classes.get(SubPkgClass.class), false));
        pkg.normalize();

        File expectedJson = JsonTestUtils.getJsonFile("/testnormalize2.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with several packages")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JsonJavaPackage javaPackage = new JsonJavaPackage("com", "com");
        JavaClass someClass = classes.get(SomeClass.class);
        String aPackage = someClass.getPackage();
        javaPackage.insertPackage(aPackage);
        JsonJavaClass jsonSomeClass = new JsonJavaClass(someClass, false);
        javaPackage.insert(jsonSomeClass);

        assertThatOptional(javaPackage.getChild("com")).contains(javaPackage);
        assertThat(javaPackage.getChild(aPackage).get().fullName).isEqualTo(aPackage);
        assertThatOptional(javaPackage.getChild(someClass.getName())).contains(jsonSomeClass);

        assertThatOptional(javaPackage.getChild("com.tngtech.pkg.NotExisting")).isAbsent();
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

    // FIXME: This is bad practice, don't test private methods!!!! Test through public API
    private Method getPrivateMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}