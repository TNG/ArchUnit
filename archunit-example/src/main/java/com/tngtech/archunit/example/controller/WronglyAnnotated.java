package com.tngtech.archunit.example.controller;

import com.tngtech.archunit.example.MyController;
import com.tngtech.archunit.example.core.HighSecurity;

@HighSecurity
@MyController
public class WronglyAnnotated {
}
