package com.tngtech.archunit.htmlvisualization;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.htmlvisualization.testclasses.SomeInterface;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JsonJavaInterfaceTest {
    @Test
    public void getChild_returns_self_or_child_by_full_name() {
        JavaClasses classes = importClasses(SomeInterface.class, SomeInterface.InnerInterface.class);

        String root = SomeInterface.class.getPackage().getName();
        JsonJavaPackage jsonPackage = new JsonJavaPackage(root, root);
        JsonJavaInterface jsonClass = new JsonJavaInterface(classes.get(SomeInterface.class));
        jsonPackage.insert(jsonClass);
        JsonJavaInterface jsonInnerClass = new JsonJavaInterface(classes.get(SomeInterface.InnerInterface.class));
        jsonClass.insert(jsonInnerClass);

        assertThat(jsonClass.getChild(SomeInterface.class.getName())).contains(jsonClass);
        assertThat(jsonClass.getChild(SomeInterface.InnerInterface.class.getName())).contains(jsonInnerClass);

        assertThat(jsonClass.getChild("something.not.There")).isAbsent();
    }
}
