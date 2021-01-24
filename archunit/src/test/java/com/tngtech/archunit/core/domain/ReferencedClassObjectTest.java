package com.tngtech.archunit.core.domain;

import java.io.File;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.ReferencedClassObject.Functions.GET_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

public class ReferencedClassObjectTest {

    @Test
    public void function_getValue() {
        class SomeClass {
            @SuppressWarnings("unused")
            Class<?> call() {
                return File.class;
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(SomeClass.class, File.class);
        JavaMethod owner = classes.get(SomeClass.class).getMethod("call");

        ReferencedClassObject referencedClassObject = getOnlyElement(owner.getReferencedClassObjects());

        assertThat(GET_VALUE.apply(referencedClassObject)).isEqualTo(classes.get(File.class));
    }
}
