package com.tngtech.archunit.example.layers.service;

@SuppressWarnings("unused")
public class ServiceViolatingProxyRules {
    public void methodBypassingAsyncProxy() {
        // Do some extra stuff, then
        executeAsync();
    }

    @Async
    public void executeAsync() {
    }
}
