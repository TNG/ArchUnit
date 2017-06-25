package com.tngtech.archunit.lang;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectsLinesToJoinTest {
    @Test
    public void ToJoin_joins_lines_on_given_separator() {
        CollectsLinesToJoin linesToJoin = new CollectsLinesToJoin();
        linesToJoin.add("one");
        linesToJoin.add("two");
        linesToJoin.add("three");
        assertThat(linesToJoin.joinOn("#")).isEqualTo("one#two#three");
    }
}