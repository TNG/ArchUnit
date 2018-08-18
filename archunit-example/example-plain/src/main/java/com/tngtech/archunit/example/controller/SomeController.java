package com.tngtech.archunit.example.controller;

import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;
import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;

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
