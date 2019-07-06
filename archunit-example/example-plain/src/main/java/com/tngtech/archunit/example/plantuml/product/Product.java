package com.tngtech.archunit.example.plantuml.product;

import com.tngtech.archunit.example.plantuml.customer.Customer;
import com.tngtech.archunit.example.plantuml.order.Order;

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
