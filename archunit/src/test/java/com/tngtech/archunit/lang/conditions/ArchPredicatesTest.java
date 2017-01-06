package com.tngtech.archunit.lang.conditions;

import org.junit.Test;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.accessType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerIs;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetTypeResidesIn;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchPredicatesTest {
    @Test
    public void descriptions() {
        assertThat(ownerAndNameAre(System.class, "out").getDescription())
                .isEqualTo("owner is java.lang.System and name is 'out'");

        assertThat(ownerIs(System.class).getDescription())
                .isEqualTo("owner is java.lang.System");

        assertThat(accessType(SET).getDescription())
                .isEqualTo("access type " + SET);

        assertThat(targetTypeResidesIn("..any..").getDescription())
                .isEqualTo("target type resides in '..any..'");
    }
}