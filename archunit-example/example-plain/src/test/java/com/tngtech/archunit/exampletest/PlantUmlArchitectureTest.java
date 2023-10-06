package com.tngtech.archunit.exampletest;

import java.net.URL;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatchers;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.shopping.catalog.ProductCatalog;
import com.tngtech.archunit.example.shopping.order.Order;
import com.tngtech.archunit.example.shopping.product.Product;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_PACKAGE_NAME;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringAllDependencies;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInDiagram;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.adhereToPlantUmlDiagram;

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
                .ignoreDependenciesWithTarget(GET_PACKAGE_NAME.is(PackageMatchers.of("", "java..")).as("that is part of JDK"))
                .ignoreDependencies(dependency(Product.class, Order.class)
                        .as(String.format("ignoring dependencies from %s to %s", Product.class.getName(), Order.class.getName()))))
                .check(classes);
    }

    @Test
    public void classes_should_adhere_to_shopping_example_considering_only_dependencies_in_any_package() {
        classes().should(adhereToPlantUmlDiagram(plantUmlDiagram,
                consideringOnlyDependenciesInAnyPackage("..catalog")))
                .check(classes);
    }
}
