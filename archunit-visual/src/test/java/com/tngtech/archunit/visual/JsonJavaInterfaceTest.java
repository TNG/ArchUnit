package com.tngtech.archunit.visual;

import org.junit.Test;

public class JsonJavaInterfaceTest {
    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JsonJavaPackage javaPackage = new JsonJavaPackage("pkg", "com.tngtech.pkg");
        JsonJavaInterface jsonJavaInterface = JsonTestUtils.createJsonJavaInterface("Interface1", "com.tngtech.pkg.Interface1");
        javaPackage.insert(jsonJavaInterface);
        jsonJavaInterface.insert(JsonTestUtils.createJsonJavaClass("InnerClass1", "com.tngtech.pkg.Interface1$InnerClass1"));

        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(jsonJavaInterface.getChild("com.tngtech.pkg.Interface1"))).contains("com.tngtech.pkg.Interface1");
        JsonTestUtils.assertThatOptional(JsonTestUtils.fullNameOf(jsonJavaInterface.getChild("com.tngtech.pkg.Interface1$InnerClass1"))).contains("com.tngtech.pkg.Interface1$InnerClass1");
        JsonTestUtils.assertThatOptional(javaPackage.getChild("com.tngtech.pkg.Interface1$NotExistingInnerClass")).isAbsent();
    }
}
