package com.tngtech.archunit.example.shopping.order;

import java.util.Set;

import com.tngtech.archunit.example.ModuleApi;
import com.tngtech.archunit.example.shopping.address.Address;
import com.tngtech.archunit.example.shopping.customer.Customer;
import com.tngtech.archunit.example.shopping.product.Product;

@ModuleApi
@SuppressWarnings("unused")
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
