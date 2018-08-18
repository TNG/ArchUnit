package com.tngtech.archunit.example.controller;

import com.tngtech.archunit.example.service.ServiceHelper;

public class SomeGuiController {
    void callServiceLayer() {
        new ServiceHelper();
        new ServiceHelper("this is okay");
    }
}
