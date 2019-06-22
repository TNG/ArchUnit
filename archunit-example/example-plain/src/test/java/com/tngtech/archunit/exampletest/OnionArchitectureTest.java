package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@Category(Example.class)
public class OnionArchitectureTest {
    private static final String BASE_PACKAGE = "com.tngtech.archunit.onionarchitecture.example";

    private final JavaClasses classes = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    public void onion_architecture_is_respected() {
        onionArchitecture()
                .domainModel(String.format("%s.domain.model..", BASE_PACKAGE))
                .domainService(String.format("%s.domain.service..", BASE_PACKAGE))
                .application(String.format("%s.application..", BASE_PACKAGE))
                .adapter("cli", String.format("%s.adapter.cli..", BASE_PACKAGE))
                .adapter("persistence", String.format("%s.adapter.persistence..", BASE_PACKAGE))
                .adapter("rest", String.format("%s.adapter.rest..", BASE_PACKAGE))
                .check(classes);
    }
}
