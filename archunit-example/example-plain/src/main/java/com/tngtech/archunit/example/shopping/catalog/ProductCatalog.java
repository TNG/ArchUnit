package com.tngtech.archunit.example.shopping.catalog;

import java.util.Set;

import com.tngtech.archunit.example.shopping.order.Order;
import com.tngtech.archunit.example.shopping.product.Product;

public class ProductCatalog {
    public Set<Product> allProducts;

    void gonnaDoSomethingIllegalWithOrder() {
        Order order = new Order();
        order.addProducts(allProducts);
    }
}
