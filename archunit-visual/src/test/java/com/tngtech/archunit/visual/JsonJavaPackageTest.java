package com.tngtech.archunit.visual;

import java.io.File;
import java.util.*;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import org.junit.Test;

import static com.tngtech.archunit.visual.JsonTestUtils.assertThatOptional;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonJavaPackageTest {
    private static final JavaClasses classes = new ClassFileImporter().importPackagesOf(SomeClass.class);

    @Test
    public void testInsertPackage() {
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
    public void testInsertPackageToDefaultRoot() {
        JsonJavaPackage rootPackage = JsonJavaPackage.createPackageStructure(Collections.<String>emptySet());
        rootPackage.insertPackage("com.tngtech");
        File expectedJson = JsonTestUtils.getJsonFile("/testinsertpackageToDefaultRoot.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a single package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testInsert() {
        JsonJavaPackage rootPackage = new JsonJavaPackage("com", "com");
        rootPackage.insertPackage(SubPkgClass.class.getPackage().getName());
        rootPackage.insert(new JsonJavaClass(classes.get(SomeClass.class), false));
        rootPackage.insert(new JsonJavaClass(classes.get(SubPkgClass.class), false));

        File expectedJson = JsonTestUtils.getJsonFile("/testinsert.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting two classes")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalizeDefaultRootPackage() {
        JsonJavaPackage pkg = JsonJavaPackage.createPackageStructure(new HashSet<String>(Arrays.asList("com")));
        pkg.normalize();

        File expectedJson = JsonTestUtils.getJsonFile("/testnormalize1.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with unnecessary default package and another package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalize() {
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
    public void getChild_returns_self_or_child_by_full_name() {
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
    public void testCreatePackageStructure() {
        Set<String> pkgs = new HashSet<>(Arrays.asList("com.tngtech.pkg1", "com.tngtech.pkg1.subpkg1",
                "com.tngtech.pkg2", "java.lang"));
        JsonJavaPackage act = JsonJavaPackage.createPackageStructure(pkgs);
        File expectedJson = JsonTestUtils.getJsonFile("/testcreatepackagestructure.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testCreatePackageStructureWithWrongPackageOrder() {
        Set<String> pkgs = new LinkedHashSet<>(Arrays.asList("com.tngtech.pkg1.subpkg1", "com.tngtech.pkg1"));
        JsonJavaPackage act = JsonJavaPackage.createPackageStructure(pkgs);
        File expectedJson = JsonTestUtils.getJsonFile("/testcreatepackagestructurewrongorder.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }
}