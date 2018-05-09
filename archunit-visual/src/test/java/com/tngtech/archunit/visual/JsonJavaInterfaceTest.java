package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.visual.testclasses.SomeInterface;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.visual.ResourcesUtils.assertThatOptional;

public class JsonJavaInterfaceTest {
    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JavaClasses classes = importClasses(SomeInterface.class, SomeInterface.InnerInterface.class);

        String root = SomeInterface.class.getPackage().getName();
        JsonJavaPackage jsonPackage = new JsonJavaPackage(root, root);
        JsonJavaInterface jsonClass = new JsonJavaInterface(classes.get(SomeInterface.class));
        jsonPackage.insert(jsonClass);
        JsonJavaInterface jsonInnerClass = new JsonJavaInterface(classes.get(SomeInterface.InnerInterface.class));
        jsonClass.insert(jsonInnerClass);

        assertThatOptional(jsonClass.getChild(SomeInterface.class.getName())).contains(jsonClass);
        assertThatOptional(jsonClass.getChild(SomeInterface.InnerInterface.class.getName())).contains(jsonInnerClass);

        assertThatOptional(jsonClass.getChild("something.not.There")).isAbsent();
    }
}
