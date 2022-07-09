package com.tngtech.archunit.exampletest;

import org.junit.Test;

import static com.tngtech.archunit.ArchUnit.Predicates.ForJavaClass.simpleName;
import static com.tngtech.archunit.ArchUnit.RuleDefinitions.classes;

public class DemoTest {
    @Test
    public void name() {
        classes().that(simpleName("sadf")).should().bePublic();

    }
}
