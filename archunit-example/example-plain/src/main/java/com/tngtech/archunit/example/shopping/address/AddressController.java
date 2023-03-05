package com.tngtech.archunit.example.shopping.address;

import com.tngtech.archunit.example.ModuleApi;
import com.tngtech.archunit.example.layers.AbstractController;

@ModuleApi
@SuppressWarnings("unused")
public class AddressController extends AbstractController {
    void handleAddress(Address address) {
        // do something
    }
}
