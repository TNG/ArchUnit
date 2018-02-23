package com.tngtech.archunit.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class InitialConfigurationTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void can_set_and_retrieve_an_initial_value() {
        InitialConfiguration<String> configuration = new InitialConfiguration<>();

        configuration.set("initial");
        assertThat(configuration.get()).isEqualTo("initial");
    }

    @Test
    public void throws_exception_if_object_is_set_more_than_once() {
        InitialConfiguration<Object> configuration = new InitialConfiguration<>();

        configuration.set("old");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("set once");
        thrown.expectMessage("old");
        thrown.expectMessage("changed");
        configuration.set("changed");
    }

    @Test
    public void throws_exception_if_no_value_is_present_on_access() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("No value was ever set");
        new InitialConfiguration<>().get();
    }
}