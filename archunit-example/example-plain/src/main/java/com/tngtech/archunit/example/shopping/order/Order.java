package com.tngtech.archunit.example.shopping.order;

import java.util.Set;

import com.tngtech.archunit.example.shopping.customer.Customer;
import com.tngtech.archunit.example.shopping.product.Product;

public class Order {
    public Customer customer;
    public Set<Product> products;

    public void addProducts(Set<Product> products) {
        this.products.addAll(products);
    }
}
