package com.tngtech.archunit.library.modules;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchModuleTest {

    @Test
    public void identifier_equals_hashcode_and_toString() {
        ArchModule.Identifier original = ArchModule.Identifier.from("one", "two");
        ArchModule.Identifier equal = ArchModule.Identifier.from("one", "two");
        ArchModule.Identifier different = ArchModule.Identifier.from("one", "other");

        assertThat(equal).isEqualTo(original);
        assertThat(equal.hashCode()).isEqualTo(original.hashCode());
        assertThat(equal).isNotEqualTo(different);
    }

    @Test
    public void identifier_parts() {
        ArchModule.Identifier identifier = ArchModule.Identifier.from("one", "two", "three");

        assertThat(identifier).containsExactly("one", "two", "three");
        assertThat(identifier.getNumberOfParts()).as("number of parts").isEqualTo(3);
        assertThat(identifier.getPart(1)).isEqualTo("one");
        assertThat(identifier.getPart(2)).isEqualTo("two");
        assertThat(identifier.getPart(3)).isEqualTo("three");
    }

    static Stream<Arguments> illegal_indices() {
        return Stream.of(
                $(ArchModule.Identifier.from("one"), 0),
                $(ArchModule.Identifier.from("one"), 2)
        );
    }

    @ParameterizedTest
    @MethodSource("illegal_indices")
    void rejects_index_out_of_range(ArchModule.Identifier identifier, int illegalIndex) {
        assertThatThrownBy(() -> identifier.getPart(illegalIndex))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(illegalIndex))
                .hasMessageContaining("out of bounds");
    }
}
