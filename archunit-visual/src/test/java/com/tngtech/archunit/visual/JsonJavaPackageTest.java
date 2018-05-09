package com.tngtech.archunit.visual;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.subpkg.SubPkgClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static com.tngtech.archunit.visual.ResourcesUtils.assertThatOptional;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonJavaPackageTest {
    private static final JavaClasses classes = new ClassFileImporter().importPackagesOf(SomeClass.class);

    @Test
    public void testInsertPackage() {
        JsonJavaPackage rootPackage = new JsonJavaPackage("com", "com");
        rootPackage.insertPackage("com.tngtech");
        File expectedJson = ResourcesUtils.getResource("/testinsertpackage1.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a single package")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));

        rootPackage.insertPackage("com.tngtech.pkg.subpkg");
        expectedJson = ResourcesUtils.getResource("/testinsertpackage2.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a package with sub-package")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testInsertPackageToDefaultRoot() {
        JsonJavaPackage rootPackage = JsonJavaPackage.createPackageStructure(Collections.<String>emptySet());
        rootPackage.insertPackage("com.tngtech");
        File expectedJson = ResourcesUtils.getResource("/testinsertpackageToDefaultRoot.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting a single package")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testInsert() {
        JsonJavaPackage rootPackage = new JsonJavaPackage("com", "com");
        rootPackage.insertPackage(SubPkgClass.class.getPackage().getName());
        rootPackage.insert(new JsonJavaClass(classes.get(SomeClass.class), false));
        rootPackage.insert(new JsonJavaClass(classes.get(SubPkgClass.class), false));

        File expectedJson = ResourcesUtils.getResource("/testinsert.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(rootPackage)))
                .as("structure after inserting two classes")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testNormalizeDefaultRootPackage() {
        JsonJavaPackage pkg = JsonJavaPackage.createPackageStructure(new HashSet<String>(Arrays.asList("com")));
        pkg.normalize();

        File expectedJson = ResourcesUtils.getResource("/testnormalize1.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with unnecessary default package and another package")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
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

        File expectedJson = ResourcesUtils.getResource("/testnormalize2.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with several packages")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
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
        File expectedJson = ResourcesUtils.getResource("/testcreatepackagestructure.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
    }

    @Test
    public void testCreatePackageStructureWithWrongPackageOrder() {
        Set<String> pkgs = new LinkedHashSet<>(Arrays.asList("com.tngtech.pkg1.subpkg1", "com.tngtech.pkg1"));
        JsonJavaPackage act = JsonJavaPackage.createPackageStructure(pkgs);
        File expectedJson = ResourcesUtils.getResource("/testcreatepackagestructurewrongorder.json");
        assertThat(ResourcesUtils.jsonToMap(ResourcesUtils.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(ResourcesUtils.jsonToMap(expectedJson));
    }
}