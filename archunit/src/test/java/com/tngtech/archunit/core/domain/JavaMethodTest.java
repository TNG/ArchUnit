package com.tngtech.archunit.core.domain;


import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import java.util.stream.Collectors;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

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
        class GrandChild extends Child {
            void method1() {

            }

            void method1(int x) {

            }

            void method2() {

            }

            void method3() {

            }

        }
        ClassFileImporter importer = new ClassFileImporter();
        JavaClass baseClass = importer.importClass(Base.class);
        JavaClass childClass = importer.importClass(Child.class);
        JavaClass grandChildClass = importer.importClass(GrandChild.class);
        assertThat(baseClass.getMethod("method1").isOverridden()).isFalse();
        assertThat(baseClass.getMethod("method1", int.class).isOverridden()).isFalse();
        assertThat(childClass.getMethod("method1").isOverridden()).isTrue();
        assertThat(childClass.getMethod("method2").isOverridden()).isFalse();
        assertThat(grandChildClass.getMethod("method1").isOverridden()).isTrue();
        assertThat(grandChildClass.getMethod("method1", int.class).isOverridden()).isTrue();
        assertThat(grandChildClass.getMethod("method2").isOverridden()).isTrue();
        assertThat(grandChildClass.getMethod("method3").isOverridden()).isFalse();
        //TODO add testing for methods with generic parameters
    }
    @Test
    public void overridden_generic_methods_are_supported() {
        class Parent<T extends Number> {
            void method(T t) { }
        }
        class Child extends Parent<Integer> {
            @Override
            void method(Integer t) { }

        }
        ClassFileImporter classFileImporter = new ClassFileImporter();
        JavaClass childClass = classFileImporter.importClass(Child.class);
        JavaMethod method = childClass.getMethod("method", Integer.class);
        assertThat(method.isOverridden()).isTrue();
    }
}
