package com.tngtech.archunit.library.adr.markdown;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

class MdAdrTest {
    @Test
    void testMdGeneration() {
        MatcherAssert.assertThat(
                new ExampleAdr().toString(),
                new IsEqual<>(
                        new ExpectedExampleAdr().toString()
                )
        );
    }
}
