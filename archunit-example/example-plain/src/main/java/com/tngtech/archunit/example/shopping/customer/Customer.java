package com.tngtech.archunit.example.shopping.customer;

import com.tngtech.archunit.example.shopping.address.Address;
import com.tngtech.archunit.example.shopping.order.Order;

public class Customer {
    private Address address;

    void addOrder(Order order) {
        // simply having such a parameter violates the specified UML diagram
    }

    public Address getAddress() {
        return address;
    }
}
