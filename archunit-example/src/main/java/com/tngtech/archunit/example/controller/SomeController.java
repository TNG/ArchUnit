package com.tngtech.archunit.example.controller;

import com.tngtech.archunit.example.service.ServiceViolatingDaoRules;

public class SomeController {
    private ServiceViolatingDaoRules service;

    void doSthController() {
        service.doSthService();
    }
}
