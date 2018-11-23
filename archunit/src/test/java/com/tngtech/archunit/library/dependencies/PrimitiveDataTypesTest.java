package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.library.dependencies.PrimitiveDataTypes.IntStack;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveDataTypesTest {
    @Test
    public void stack_can_be_popped() {
        IntStack intStack = new IntStack(2);
        intStack.push(1);
        intStack.push(2);

        assertThat(intStack.pop()).isEqualTo(2);
        assertThat(intStack.pop()).isEqualTo(1);
    }

    @Test
    public void conversion_to_array_gets_all_elements() {
        IntStack intStack = new IntStack(10);
        intStack.push(1);
        intStack.push(2);
        intStack.push(3);
        intStack.pop();
        intStack.push(4);

        int[] expected = {1, 2, 4};
        assertThat(intStack.asArray()).isEqualTo(expected);
    }
}