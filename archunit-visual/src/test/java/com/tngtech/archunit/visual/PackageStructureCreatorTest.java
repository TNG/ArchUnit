package com.tngtech.archunit.visual;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class PackageStructureCreatorTest {
    @Test
    public void testCreatePackage() throws Exception {
        JsonJavaPackage act = PackageStructureCreator.createPackage("com", false, "com.tngtech");
        assertTrue("creating new package not working for one subpackage",
                hasNameAndFullname(act, "tngtech", "com.tngtech"));

        act = PackageStructureCreator.createPackage("com", false, "com.tngtech.pkg.subpkg");
        assertTrue("creating new package not working for several subpackages",
                hasNameAndFullname(act, "tngtech", "com.tngtech"));

        act = PackageStructureCreator.createPackage("default", true, "com.tngtech");
        assertTrue("creating new package not working for several subpackages and default root",
                hasNameAndFullname(act, "com", "com"));
    }

    @Test
    public void testCreatePackageStructure() throws Exception {
        // FIXME: Don't test private methods, it's shady ;-)
        Method createPackageStructure = PackageStructureCreator.class.getDeclaredMethod("createPackageStructure", Set.class);
        createPackageStructure.setAccessible(true);

        Set<String> pkgs = new HashSet<>(Arrays.asList("com.tngtech.pkg1", "com.tngtech.pkg1.subpkg1",
                "com.tngtech.pkg2", "java.lang"));
        JsonJavaPackage act = (JsonJavaPackage) createPackageStructure.invoke(null, pkgs);
        File expectedJson = JsonConverter.getJsonFile("/testcreatepackagestructure.json");
        assertThat(JsonConverter.jsonToMap(JsonConverter.getJsonStringOf(act)))
                .as("created package structure")
                .isEqualTo(JsonConverter.jsonToMap(expectedJson));
    }

    private boolean hasNameAndFullname(JsonJavaPackage pkg, String name, String fullname) {
        return pkg.name.equals(name) && pkg.fullname.equals(fullname);
    }
}