package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.TestUtils;
import com.tngtech.archunit.visual.testclasses.SomeClass;
import com.tngtech.archunit.visual.testclasses.subpkg.ThirdSubPkgClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonJavaElementTest {

    @Test
    public void testInsertToJsonJavaElement() {
        JavaClasses classes = TestUtils.importClasses(SomeClass.class, SomeClass.InnerClass.class);
        JsonJavaElement jsonJavaClass = new JsonJavaClass(classes.get(SomeClass.class), false);
        JsonJavaElement jsonJavaInnerClass = new JsonJavaClass(classes.get(SomeClass.InnerClass.class), false);
        jsonJavaClass.insert(jsonJavaInnerClass);
        assertThat(jsonJavaClass.getChildren().contains(jsonJavaInnerClass)).isTrue();
    }

    @Test
    public void testInsertToChildOfJsonElement() {
        JavaClasses classes = TestUtils.importClasses(ThirdSubPkgClass.class, ThirdSubPkgClass.InnerClass1.class,
                ThirdSubPkgClass.InnerClass1.InnerClass2.class);
        JsonElement jsonElement = new JsonJavaClass(classes.get(ThirdSubPkgClass.class), false);
        JsonJavaElement innerJsonJavaClass = new JsonJavaClass(classes.get(ThirdSubPkgClass.InnerClass1.class), false);
        jsonElement.insert(innerJsonJavaClass);

        JsonJavaElement innerInnerJsonJavaClass = new JsonJavaClass(classes.get(ThirdSubPkgClass.InnerClass1.InnerClass2.class), false);
        jsonElement.insert(innerInnerJsonJavaClass);

        assertThat(jsonElement.getChildren().iterator().next().getChildren().contains(innerInnerJsonJavaClass)).isTrue();
    }

    /**
     * tests whether the enclosing class of an inner class is generated, if its is not in the tree-structure yet
     */
    @Test
    public void testInsertIsolatedInnerClassToJsonElement() {
        JavaClasses classes = TestUtils.importClasses(ThirdSubPkgClass.class, ThirdSubPkgClass.InnerClass1.InnerClass2.class);
        JsonElement jsonElement = new JsonJavaClass(classes.get(ThirdSubPkgClass.class), false);

        JsonJavaElement isolatedInnerJsonJavaClass = new JsonJavaClass(classes.get(ThirdSubPkgClass.InnerClass1.InnerClass2.class), false);
        jsonElement.insert(isolatedInnerJsonJavaClass);

        JsonElement generatedJsonElement = jsonElement.getChildren().iterator().next();
        assertThat(generatedJsonElement.fullName).isEqualTo("com.tngtech.archunit.visual.testclasses.subpkg.ThirdSubPkgClass$InnerClass1");
        assertThat(generatedJsonElement.name).isEqualTo("InnerClass1");
        assertThat(generatedJsonElement.getChildren().contains(isolatedInnerJsonJavaClass)).isTrue();
    }
}
