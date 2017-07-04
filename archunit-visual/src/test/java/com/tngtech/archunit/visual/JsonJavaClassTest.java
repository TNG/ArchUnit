package com.tngtech.archunit.visual;

import org.junit.Test;

public class JsonJavaClassTest {
    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JsonJavaPackage javaPackage = new JsonJavaPackage("pkg", "com.tngtech.pkg");
        JsonJavaClass jsonJavaClass = JsonTestUtils.createJsonJavaClass("Class1", "com.tngtech.pkg.Class1");
        javaPackage.insert(jsonJavaClass);
        jsonJavaClass.insert(JsonTestUtils.createJsonJavaClass("InnerClass1", "com.tngtech.pkg.Class1$InnerClass1"));

        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(jsonJavaClass.getChild("com.tngtech.pkg.Class1"))).contains("com.tngtech.pkg.Class1");
        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(jsonJavaClass.getChild("com.tngtech.pkg.Class1$InnerClass1"))).contains("com.tngtech.pkg.Class1$InnerClass1");
        JsonTestUtils.assertThatOptional(javaPackage.getChild("com.tngtech.pkg.Class1$NotExistingInnerClass")).isAbsent();
    }
}
