package com.tngtech.archunit.exampletest;

import java.net.URL;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.shopping.catalog.ProductCatalog;
import com.tngtech.archunit.example.shopping.order.Order;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringAllDependencies;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringOnlyDependenciesInDiagram;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.adhereToPlantUmlDiagram;

@Category(Example.class)
public class PlantUmlArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.shopping");
    private final URL plantUmlDiagram = PlantUmlArchitectureTest.class.getResource("shopping_example.puml");

    @Test
    public void classes_should_adhere_to_shopping_example_considering_only_dependencies_in_diagram() {
        classes().should(adhereToPlantUmlDiagram(plantUmlDiagram, consideringOnlyDependenciesInDiagram()))
                .check(classes);
    }

    @Test
    public void classes_should_adhere_to_shopping_example_considering_all_dependencies_and_ignoring_some_dependencies() {
        classes().should(adhereToPlantUmlDiagram(plantUmlDiagram, consideringAllDependencies())
                .ignoreDependenciesWithOrigin(equivalentTo(ProductCatalog.class))
                .ignoreDependenciesWithTarget(equivalentTo(Object.class))
                .ignoreDependencies(dependency(Order.class, Set.class)
                        .as(String.format("ignoring dependencies from %s to %s", Order.class.getName(), Set.class.getName()))))
                .check(classes);
    }

    @Test
    public void classes_should_adhere_to_shopping_example_considering_only_dependencies_in_any_package() {
        classes().should(adhereToPlantUmlDiagram(plantUmlDiagram,
                consideringOnlyDependenciesInAnyPackage("..catalog")))
                .check(classes);
    }
}
