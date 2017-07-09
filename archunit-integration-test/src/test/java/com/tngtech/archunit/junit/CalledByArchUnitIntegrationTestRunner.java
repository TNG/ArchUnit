package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
public @interface CalledByArchUnitIntegrationTestRunner {
}
