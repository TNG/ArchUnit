@AppModule(
        name = "Customer",
        allowedDependencies = {"Address"},
        exposedPackages = "com.tngtech.archunit.example.shopping.customer"
)
package com.tngtech.archunit.example.shopping.customer;

import com.tngtech.archunit.example.AppModule;
