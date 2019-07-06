package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.onionarchitecture")
public class OnionArchitectureTest {
    private static final String BASE_PACKAGE = "com.tngtech.archunit.example.onionarchitecture";

    @ArchTest
    static final ArchRule onion_architecture_is_respected = onionArchitecture()
            .domainModel(String.format("%s.domain.model..", BASE_PACKAGE))
            .domainService(String.format("%s.domain.service..", BASE_PACKAGE))
            .application(String.format("%s.application..", BASE_PACKAGE))
            .adapter("cli", String.format("%s.adapter.cli..", BASE_PACKAGE))
            .adapter("persistence", String.format("%s.adapter.persistence..", BASE_PACKAGE))
            .adapter("rest", String.format("%s.adapter.rest..", BASE_PACKAGE));
}
