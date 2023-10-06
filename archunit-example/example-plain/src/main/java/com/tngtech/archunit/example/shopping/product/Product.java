package com.tngtech.archunit.example.shopping.product;

import com.tngtech.archunit.example.ModuleApi;
import com.tngtech.archunit.example.shopping.customer.Customer;
import com.tngtech.archunit.example.shopping.order.Order;

@ModuleApi
@SuppressWarnings("unused")
public class Product {
    public Customer customer;

    Order getOrder() {
        return null; // the return type violates the specified UML diagram
    }

    public void register() {
    }

    public void report() {
    }
}
