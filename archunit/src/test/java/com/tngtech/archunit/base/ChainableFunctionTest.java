package com.tngtech.archunit.base;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChainableFunctionTest {
    @Test
    public void after() {
        Integer result = plus(7).after(parseInteger()).apply("11");

        assertThat(result).as("Adding 7 after parseInt('11')").isEqualTo(18);
    }

    @Test
    public void then() {
        Integer result = parseInteger().then(plus(7)).apply("11");

        assertThat(result).as("parseInt('11') then adding 7").isEqualTo(18);
    }

    @Test
    public void is() {
        assertThat(parseInteger().is(greaterThan(7)).apply("7"))
                .as("7 > 7").isFalse();
        assertThat(parseInteger().is(greaterThan(7)).apply("8"))
                .as("8 > 7").isTrue();
    }

    private DescribedPredicate<Integer> greaterThan(final int number) {
        return new DescribedPredicate<Integer>("greater than " + number) {
            @Override
            public boolean apply(Integer input) {
                return input > number;
            }
        };
    }

    private ChainableFunction<String, Integer> parseInteger() {
        return new ChainableFunction<String, Integer>() {
            @Override
            public Integer apply(String input) {
                return Integer.parseInt(input);
            }
        };
    }

    private ChainableFunction<Integer, Integer> plus(final int number) {
        return new ChainableFunction<Integer, Integer>() {
            @Override
            public Integer apply(Integer input) {
                return input + number;
            }
        };
    }
}