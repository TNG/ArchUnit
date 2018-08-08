package com.tngtech.archunit.exampletest.junit5;

import java.net.URL;
import java.util.Set;

import com.tngtech.archunit.example.shopping.catalog.ProductCatalog;
import com.tngtech.archunit.example.shopping.order.Order;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringAllDependencies;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringOnlyDependenciesInAnyPackage;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringOnlyDependenciesInDiagram;
import static com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.adhereToPlantUmlDiagram;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.shopping")
public class PlantUmlArchitectureTest {
    private static final URL plantUmlDiagram = PlantUmlArchitectureTest.class.getResource("shopping_example.puml");

    @ArchTest
    static final ArchRule classes_should_adhere_to_shopping_example_considering_only_dependencies_in_diagram =
            classes().should(adhereToPlantUmlDiagram(plantUmlDiagram, consideringOnlyDependenciesInDiagram()));

    @ArchTest
    static final ArchRule classes_should_adhere_to_shopping_example_considering_all_dependencies_and_ignoring_some_dependencies =
            classes().should(adhereToPlantUmlDiagram(plantUmlDiagram, consideringAllDependencies())
                    .ignoreDependenciesWithOrigin(equivalentTo(ProductCatalog.class))
                    .ignoreDependenciesWithTarget(equivalentTo(Object.class))
                    .ignoreDependencies(dependency(Order.class, Set.class)
                            .as(String.format("ignoring dependencies from %s to %s", Order.class.getName(), Set.class.getName()))));

    @ArchTest
    static final ArchRule classes_should_adhere_to_shopping_example_considering_only_dependencies_in_any_package =
            classes().should(adhereToPlantUmlDiagram(plantUmlDiagram,
                    consideringOnlyDependenciesInAnyPackage("..catalog")));
}
