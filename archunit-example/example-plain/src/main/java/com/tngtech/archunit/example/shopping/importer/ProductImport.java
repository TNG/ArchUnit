package com.tngtech.archunit.example.shopping.importer;

import com.tngtech.archunit.example.shopping.catalog.ProductCatalog;
import com.tngtech.archunit.example.shopping.customer.Customer;
import com.tngtech.archunit.example.shopping.xml.processor.XmlProcessor;
import com.tngtech.archunit.example.shopping.xml.types.XmlTypes;

public class ProductImport {
    public ProductCatalog productCatalog;
    public XmlTypes xmlType;
    public XmlProcessor xmlProcessor;

    public Customer getCustomer() {
        return new Customer(); // violates diagram -> product import may not directly know Customer
    }

    ProductCatalog parse(byte[] xml) {
        return new ProductCatalog();
    }
}
