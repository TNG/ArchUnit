package com.tngtech.archunit.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InitialConfigurationTest {

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

        assertThatThrownBy(
                () -> configuration.set("changed")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("set once")
                .hasMessageContaining("old")
                .hasMessageContaining("changed");
    }

    @Test
    public void throws_exception_if_no_value_is_present_on_access() {
        assertThatThrownBy(
                () -> new InitialConfiguration<>().get()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No value was ever set");
    }
}
