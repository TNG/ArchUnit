package com.tngtech.archunit.example.shopping.address;

import com.tngtech.archunit.example.ModuleApi;
import com.tngtech.archunit.example.shopping.catalog.ProductCatalog;

@ModuleApi
@SuppressWarnings("unused")
public class Address {
    private ProductCatalog productCatalog;
}
