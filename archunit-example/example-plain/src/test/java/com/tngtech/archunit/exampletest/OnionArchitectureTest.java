package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.onionarchitecture.domain.model.OrderItem;
import com.tngtech.archunit.example.onionarchitecture.domain.service.OrderQuantity;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@Category(Example.class)
public class OnionArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example.onionarchitecture");

    @Test
    public void onion_architecture_is_respected() {
        onionArchitecture()
                .domainModels("..domain.model..")
                .domainServices("..domain.service..")
                .applicationServices("..application..")
                .adapter("cli", "..adapter.cli..")
                .adapter("persistence", "..adapter.persistence..")
                .adapter("rest", "..adapter.rest..")
                .check(classes);
    }

    @Test
    public void onion_architecture_is_respected_with_exception() {
        onionArchitecture()
                .domainModels("..domain.model..")
                .domainServices("..domain.service..")
                .applicationServices("..application..")
                .adapter("cli", "..adapter.cli..")
                .adapter("persistence", "..adapter.persistence..")
                .adapter("rest", "..adapter.rest..")

                .ignoreDependency(OrderItem.class, OrderQuantity.class)

                .check(classes);
    }
}
