package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.importer.testexamples.referencedclassobjects.ReferencingClassObjectsFromLocalVariable;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FilterInputStream;
import java.util.Set;

import static com.tngtech.archunit.testutil.Assertions.assertThatReferencedClassObjects;
import static com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion.referencedClassObject;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterLocalVariableDependenciesTest {

    @Test
    public void imports_referenced_class_object_in_LocalVariable() {
        JavaClasses classes = new ClassFileImporter().importClasses(ReferencingClassObjectsFromLocalVariable.class);
        Set<ReferencedClassObject> referencedClassObjects = classes.get(ReferencingClassObjectsFromLocalVariable.class).getReferencedClassObjects();

        assertThatReferencedClassObjects(referencedClassObjects).containReferencedClassObjects(
                referencedClassObject(FilterInputStream.class, 16)
                ,                referencedClassObject(FilterInputStream.class, 18)
                ,                referencedClassObject(FilterInputStream.class, 14)
                ,                referencedClassObject(Double.class, 14)
        );
    }

}
