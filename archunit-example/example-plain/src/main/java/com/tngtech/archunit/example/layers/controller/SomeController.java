package com.tngtech.archunit.example.layers.controller;

import com.tngtech.archunit.example.layers.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules;

public class SomeController {
    private ServiceViolatingDaoRules service;
    private ServiceViolatingLayerRules otherService;

    void doSthController() {
        service.doSthService();
    }

    void doSthWithSecuredService() {
        otherService.properlySecured();
    }
}
