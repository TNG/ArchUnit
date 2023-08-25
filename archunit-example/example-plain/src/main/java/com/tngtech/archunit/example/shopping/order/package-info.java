@AppModule(
        name = "Order",
        allowedDependencies = {"Customer", "Product"},
        exposedPackages = "com.tngtech.archunit.example.shopping.order"
)
package com.tngtech.archunit.example.shopping.order;

import com.tngtech.archunit.example.AppModule;
