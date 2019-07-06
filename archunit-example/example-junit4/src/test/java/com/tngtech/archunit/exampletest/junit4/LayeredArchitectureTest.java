package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.example.layers.SomeMediator;
import com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
public class LayeredArchitectureTest {
    @ArchTest
    public static final ArchRule layer_dependencies_are_respected = layeredArchitecture()

            .layer("Controllers").definedBy("com.tngtech.archunit.example.layers.controller..")
            .layer("Services").definedBy("com.tngtech.archunit.example.layers.service..")
            .layer("Persistence").definedBy("com.tngtech.archunit.example.layers.persistence..")

            .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Services");

    @ArchTest
    public static final ArchRule layer_dependencies_are_respected_with_exception = layeredArchitecture()

            .layer("Controllers").definedBy("com.tngtech.archunit.example.layers.controller..")
            .layer("Services").definedBy("com.tngtech.archunit.example.layers.service..")
            .layer("Persistence").definedBy("com.tngtech.archunit.example.layers.persistence..")

            .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Services")

            .ignoreDependency(SomeMediator.class, ServiceViolatingLayerRules.class);
}
