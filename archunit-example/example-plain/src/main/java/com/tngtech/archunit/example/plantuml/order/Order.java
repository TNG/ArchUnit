package com.tngtech.archunit.example.plantuml.order;

import java.util.Set;

import com.tngtech.archunit.example.plantuml.address.Address;
import com.tngtech.archunit.example.plantuml.customer.Customer;
import com.tngtech.archunit.example.plantuml.product.Product;

public class Order {
    public Customer customer;
    private Set<Product> products;

    public void addProducts(Set<Product> products) {
        this.products.addAll(products);
    }

    void report() {
        report(customer.getAddress());
        for (Product product : products) {
            product.report();
        }
    }

    private void report(Address address) {
    }
}
