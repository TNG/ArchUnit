package com.tngtech.archunit.example.layers.persistence.layerviolation;

import com.tngtech.archunit.example.layers.SomeMediator;
import com.tngtech.archunit.example.layers.service.ServiceInterface;
import com.tngtech.archunit.example.layers.service.ServiceViolatingLayerRules;

public class DaoCallingService implements ServiceInterface {
    public static final String violateLayerRules = "violateLayerRules";
    public static final String violateLayerRulesTrickily = "violateLayerRulesTrickily";

    ServiceViolatingLayerRules service;

    void violateLayerRules() {
        service.doSomething();
    }

    void violateLayerRulesTrickily() {
        new SomeMediator(service).violateLayerRulesIndirectly();
    }
}
