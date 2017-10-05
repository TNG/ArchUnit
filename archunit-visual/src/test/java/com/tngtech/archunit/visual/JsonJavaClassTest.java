package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.visual.JsonTestUtils.assertThatOptional;

public class JsonJavaClassTest {
    @Test
    public void getChild_returns_self_or_child_by_full_name() throws Exception {
        JavaClasses classes = importClasses(SomeClass.class, SomeClass.InnerClass.class);

        String root = SomeClass.class.getPackage().getName();
        JsonJavaPackage jsonPackage = new JsonJavaPackage(root, root);
        JsonJavaClass jsonClass = new JsonJavaClass(classes.get(SomeClass.class), false);
        jsonPackage.insert(jsonClass);
        JsonJavaClass jsonInnerClass = new JsonJavaClass(classes.get(SomeClass.InnerClass.class), false);
        jsonClass.insert(jsonInnerClass);

        assertThatOptional(jsonClass.getChild(SomeClass.class.getName())).contains(jsonClass);
        assertThatOptional(jsonClass.getChild(SomeClass.InnerClass.class.getName())).contains(jsonInnerClass);

        assertThatOptional(jsonClass.getChild("something.not.There")).isAbsent();
    }
}
