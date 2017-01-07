package com.tngtech.archunit.lang.conditions;

import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetTypeResidesIn;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchPredicatesTest {
    @Test
    public void descriptions() {
        assertThat(ownerAndNameAre(System.class, "out").getDescription())
                .isEqualTo("owner is java.lang.System and name is 'out'");

        assertThat(targetTypeResidesIn("..any..").getDescription())
                .isEqualTo("target type resides in '..any..'");
    }
}