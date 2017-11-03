package com.tngtech.archunit.integration;

import com.tngtech.archunit.example.SomeBusinessInterface;
import com.tngtech.archunit.example.service.impl.SomeInterfacePlacedInTheWrongPackage;
import com.tngtech.archunit.exampletest.InterfaceRules;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedViolation.clazz;

public class InterfaceRulesIntegrationTest extends InterfaceRules {
    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void interfaces_should_not_have_the_word_interface_in_the_name() {
        expectedViolation.ofRule("no classes that are interfaces should have name matching '.*Interface'")
                .by(clazz(SomeBusinessInterface.class).havingNameMatching(".*Interface"));

        super.interfaces_should_not_have_the_word_interface_in_the_name();
    }

    @Test
    @Override
    public void interfaces_must_not_be_placed_in_implementation_packages() {
        expectedViolation.ofRule("no classes that reside in a package '..impl..' should be interfaces")
                .by(clazz(SomeInterfacePlacedInTheWrongPackage.class).beingAnInterface());

        super.interfaces_must_not_be_placed_in_implementation_packages();
    }
}
