package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.SomeMediator;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@Category(Example.class)
public class LayeredArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

    @Test
    public void layer_dependencies_are_respected() {
        layeredArchitecture()

                .layer("Controllers").definedBy("com.tngtech.archunit.example.controller..")
                .layer("Services").definedBy("com.tngtech.archunit.example.service..")
                .layer("Persistence").definedBy("com.tngtech.archunit.example.persistence..")

                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Services")

                .check(classes);
    }

    @Test
    public void layer_dependencies_are_respected_with_exception() {
        layeredArchitecture()

                .layer("Controllers").definedBy("com.tngtech.archunit.example.controller..")
                .layer("Services").definedBy("com.tngtech.archunit.example.service..")
                .layer("Persistence").definedBy("com.tngtech.archunit.example.persistence..")

                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Services")

                .ignoreDependency(SomeMediator.class, ServiceViolatingLayerRules.class)

                .check(classes);
    }
}
