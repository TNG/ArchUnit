package com.tngtech.archunit.example;

import com.tngtech.archunit.example.service.ServiceViolatingLayerRules;

public class SomeMediator {
    public static final String violateLayerRulesIndirectly = "violateLayerRulesIndirectly";

    private final ServiceViolatingLayerRules service;

    public SomeMediator(ServiceViolatingLayerRules service) {
        this.service = service;
    }

    public void violateLayerRulesIndirectly() {
        service.doSomething();
    }
}
