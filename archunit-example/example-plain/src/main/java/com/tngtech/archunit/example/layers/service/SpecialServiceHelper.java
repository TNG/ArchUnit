package com.tngtech.archunit.example.layers.service;

import java.util.HashMap;
import java.util.Set;

import com.tngtech.archunit.example.layers.controller.SomeUtility;
import com.tngtech.archunit.example.layers.controller.one.SomeEnum;

public class SpecialServiceHelper extends ServiceHelper<SomeUtility, HashMap<?, Set<? super SomeEnum>>> {
}
