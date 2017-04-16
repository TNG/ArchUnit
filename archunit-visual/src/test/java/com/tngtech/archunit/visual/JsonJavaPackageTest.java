package com.tngtech.archunit.visual;

import java.io.File;
import java.lang.reflect.Method;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import org.assertj.guava.api.Assertions;
import org.assertj.guava.api.OptionalAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonJavaPackageTest {

    @Test
    public void insertPackage() throws Exception {
        // FIXME: Don't test private methods
        Method insertPackage = getInsertPackage();

        JsonJavaPackage pkg = new JsonJavaPackage("com", "com");

        insertPackage.invoke(pkg, "com.tngtech");
        File expectedJson = JsonTestUtils.getJsonFile("/testinsertpackage1.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after inserting a single package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));

        insertPackage.invoke(pkg, "com.tngtech.pkg.subpkg");
        expectedJson = JsonTestUtils.getJsonFile("/testinsertpackage2.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after inserting a package with sub-package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void normalize() throws Exception {
        // FIXME: Don't test private methods
        Method insertPackage = getInsertPackage();

        JsonJavaPackage pkg = JsonJavaPackage.getDefaultPackage();
        insertPackage.invoke(pkg, "com");
        pkg.normalize();

        File expectedJson = JsonTestUtils.getJsonFile("/testnormalize1.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with unnecessary default package and another package")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));

        pkg = new JsonJavaPackage("com", "com");
        insertPackage.invoke(pkg, "com.tngtech.pkg1.subpkg");
        insertPackage.invoke(pkg, "com.tngtech.pkg2.subpkg2");
        pkg.insertJavaElement(new JsonJavaClass("class1", "com.tngtech.pkg1.class1", "class", ""));
        pkg.normalize();

        expectedJson = JsonTestUtils.getJsonFile("/testnormalize2.json");
        assertThat(JsonTestUtils.jsonToMap(JsonTestUtils.getJsonStringOf(pkg)))
                .as("structure after normalizing with several packages with only one child")
                .isEqualTo(JsonTestUtils.jsonToMap(expectedJson));
    }

    @Test
    public void getChild_returns_self_or_child_by_full_name() {
        JsonJavaPackage javaPackage = new JsonJavaPackage("pkg", "com.tngtech.pkg");

        javaPackage.insertPackage("com.tngtech.pkg.subpkg");
        javaPackage.insertJavaElement(new JsonJavaClass("Class1", "com.tngtech.pkg.Class1", "class", ""));

        assertThatOptional(fullNameOf(javaPackage.getChild("com.tngtech.pkg"))).contains("com.tngtech.pkg");
        assertThatOptional(fullNameOf(javaPackage.getChild("com.tngtech.pkg.Class1"))).contains("com.tngtech.pkg.Class1");
        assertThatOptional(fullNameOf(javaPackage.getChild("com.tngtech.pkg.subpkg"))).contains("com.tngtech.pkg.subpkg");
        assertThatOptional(javaPackage.getChild("com.tngtech.pkg.NotExisting")).isAbsent();
    }

    // FIXME: We have to make a shadow Jar of archunit-visual to use the test support, but for that we need to refactor the shrinking process within archunit-junit and make it reusable
    <T> OptionalAssert<T> assertThatOptional(Optional<T> optional) {
        return Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    private Optional<String> fullNameOf(Optional<? extends JsonElement> element) {
        return element.transform(new Function<JsonElement, String>() {
            @Override
            public String apply(JsonElement input) {
                return input.fullname;
            }
        });
    }

    private Method getInsertPackage() throws NoSuchMethodException {
        Method insertPackage = JsonJavaPackage.class.getDeclaredMethod("insertPackage", String.class);
        insertPackage.setAccessible(true);
        return insertPackage;
    }
}