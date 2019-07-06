package com.tngtech.archunit.example.plantuml.catalog;

import java.util.Set;

import com.tngtech.archunit.example.plantuml.order.Order;
import com.tngtech.archunit.example.plantuml.product.Product;

public class ProductCatalog {
    private Set<Product> allProducts;

    void gonnaDoSomethingIllegalWithOrder() {
        Order order = new Order();
        for (Product product : allProducts) {
            product.register();
        }
        order.addProducts(allProducts);
    }
}
