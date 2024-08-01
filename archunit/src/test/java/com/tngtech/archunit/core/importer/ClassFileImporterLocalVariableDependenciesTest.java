package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.importer.testexamples.referencedclassobjects.ReferencingClassObjectsFromLocalVariable;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static com.tngtech.archunit.testutil.Assertions.assertThatReferencedClassObjects;
import static com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion.referencedClassObject;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterLocalVariableDependenciesTest {

    @Test
    public void imports_referenced_class_object_in_LocalVariable() {
        ArchConfiguration.get().setAnalyzeLocalVariableInstantiations(true);

        JavaClasses classes = new ClassFileImporter().importClasses(ReferencingClassObjectsFromLocalVariable.class);
        Set<ReferencedClassObject> referencedClassObjects = classes.get(ReferencingClassObjectsFromLocalVariable.class).getReferencedClassObjects();

        assertThatReferencedClassObjects(referencedClassObjects).containReferencedClassObjects(
                referencedClassObject(FilterInputStream.class, 14),
                referencedClassObject(List.class, 15),
                referencedClassObject(Double.class, 15),
                referencedClassObject(FilterInputStream.class, 21),
                referencedClassObject(InputStream.class, 22),
                referencedClassObject(FilterInputStream.class, 26),
                referencedClassObject(InputStream.class, 27),
                referencedClassObject(FilterInputStream.class, 32),
                referencedClassObject(InputStream.class, 33),
                referencedClassObject(FilterInputStream.class, 40),
                referencedClassObject(InputStream.class, 41),
                referencedClassObject(Map.class, 43),
                referencedClassObject(FilterInputStream.class, 43),
                referencedClassObject(InputStream.class, 43),
                referencedClassObject(Map.class, 45),
                referencedClassObject(Set.class, 45),
                referencedClassObject(FilterInputStream.class, 45),
                referencedClassObject(InputStream.class, 45),
                referencedClassObject(Number.class, 53)
        );
    }

}
