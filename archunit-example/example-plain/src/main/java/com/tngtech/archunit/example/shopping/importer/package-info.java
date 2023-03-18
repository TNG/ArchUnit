@AppModule(
        name = "Importer",
        allowedDependencies = {"Catalog", "XML"},
        exposedPackages = "com.tngtech.archunit.example.shopping.importer"
)
package com.tngtech.archunit.example.shopping.importer;

import com.tngtech.archunit.example.AppModule;
