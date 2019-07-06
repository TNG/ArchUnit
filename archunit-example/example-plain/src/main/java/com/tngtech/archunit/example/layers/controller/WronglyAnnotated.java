package com.tngtech.archunit.example.layers.controller;

import com.tngtech.archunit.example.layers.MyController;
import com.tngtech.archunit.example.layers.core.HighSecurity;

@HighSecurity
@MyController
public class WronglyAnnotated {
}
