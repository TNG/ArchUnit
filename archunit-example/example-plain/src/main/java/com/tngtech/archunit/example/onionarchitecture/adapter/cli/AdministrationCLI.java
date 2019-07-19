package com.tngtech.archunit.example.onionarchitecture.adapter.cli;

import com.tngtech.archunit.example.onionarchitecture.adapter.persistence.ProductRepository;
import com.tngtech.archunit.example.onionarchitecture.application.AdministrationPort;
import com.tngtech.archunit.example.onionarchitecture.application.ShoppingApplication;

@SuppressWarnings("unused")
public class AdministrationCLI {
    public static void main(String[] args) {
        AdministrationPort port = ShoppingApplication.openAdministrationPort();
        handle(args, port);
    }

    private static void handle(String[] args, AdministrationPort port) {
        // violates the pairwise independence of adapters
        ProductRepository repository = port.getInstanceOf(ProductRepository.class);
        long count = repository.getTotalCount();
        // parse arguments and re-configure application according to count through port
    }
}
