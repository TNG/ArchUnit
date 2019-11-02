package com.tngtech.archunit.core.domain;

import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaAnnotationTest {
    @Test
    public void description_of_annotation_on_class() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<JavaClass> annotation = javaClass.getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SomeAnnotation.class.getName()
                        + "> on class <" + SomeClass.class.getName() + ">");
    }

    @Test
    public void description_of_annotation_on_method() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<JavaMethod> annotation = javaClass.getMethod("method").getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SomeAnnotation.class.getName()
                        + "> on method <" + SomeClass.class.getName() + ".method()>");
    }

    @Test
    public void description_of_class_annotation_parameter() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<?> annotation = ((JavaAnnotation<?>[]) javaClass.getAnnotationOfType(SomeAnnotation.class.getName()).get("sub").get())[0];

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SubAnnotation.class.getName()
                        + "> on class <" + SomeClass.class.getName() + ">");
    }

    @Test
    public void description_of_method_annotation_parameter() {
        JavaClass javaClass = importClasses(SomeAnnotation.class, SomeClass.class).get(SomeClass.class);

        JavaAnnotation<?> annotation = ((JavaAnnotation<?>[]) javaClass.getMethod("method")
                .getAnnotationOfType(SomeAnnotation.class.getName()).get("sub").get())[0];

        assertThat(annotation.getDescription()).isEqualTo(
                "Annotation <" + SubAnnotation.class.getName()
                        + "> on method <" + SomeClass.class.getName() + ".method()>");
    }

    private @interface SomeAnnotation {
        SubAnnotation[] sub() default {};
    }

    private @interface SubAnnotation {
    }

    @SomeAnnotation(sub = @SubAnnotation)
    private static class SomeClass {
        @SomeAnnotation(sub = @SubAnnotation)
        void method() {
        }
    }
}
