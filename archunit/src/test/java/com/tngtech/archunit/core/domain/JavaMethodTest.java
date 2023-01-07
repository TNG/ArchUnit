package com.tngtech.archunit.core.domain;


import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class JavaMethodTest {
    @Test
    public void isOverriddenTest() {
        class Base {
            void method1() {
            }

            void method1(int x) {
            }
        }
        class Child extends Base {
            void method1() {
            }

            void method2() {
            }
        }

        JavaClass childClass = new ClassFileImporter().importClass(Child.class);
        JavaClass baseClass = new ClassFileImporter().importClass(Base.class);
        JavaMethod childMethod1 = childClass.getMethod("method1");
        JavaMethod baseMethod1 = baseClass.getMethod("method1");
        assertNotEquals(childMethod1, baseMethod1);
        assertEquals(childMethod1.getName(), baseMethod1.getName());
        assertThat(childClass.getMethod("method1").isOverridden()).isTrue();
        assertThat(childClass.getMethod("method1", int.class).isOverridden()).isFalse();
        assertThat(childClass.getMethod("method2").isOverridden()).isFalse();
    }
}