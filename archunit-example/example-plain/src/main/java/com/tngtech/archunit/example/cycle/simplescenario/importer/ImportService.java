package com.tngtech.archunit.example.cycle.simplescenario.importer;

import java.util.UUID;

import com.tngtech.archunit.example.cycle.simplescenario.administration.AdministrationService;

public class ImportService {
    private AdministrationService administrationService;

    public void process(String customer) {
        UUID customerId = administrationService.createCustomerId(customer);
        process(customerId, customer);
    }

    private void process(UUID id, String customer) {
        // process whatever
    }
}
