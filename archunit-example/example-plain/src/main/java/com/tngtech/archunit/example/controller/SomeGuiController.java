package com.tngtech.archunit.example.controller;

import com.tngtech.archunit.example.service.ServiceHelper;

public class SomeGuiController {
    void callServiceLayer() {
        ServiceHelper helper = new ServiceHelper();
        new ServiceHelper("this is okay");

        process(helper.insecure);
        process(helper.properlySecured);
    }

    private void process(Object object) {
    }
}
