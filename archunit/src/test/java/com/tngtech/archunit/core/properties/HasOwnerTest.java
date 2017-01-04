package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.core.properties.HasOwner.Functions.Get;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HasOwnerTest {
    @Test
    @SuppressWarnings("unchecked")
    public void function_get_owner() {
        HasOwner<String> hasOwner = mock(HasOwner.class);
        when(hasOwner.getOwner()).thenReturn("owner");

        assertThat(Get.<String>owner().apply(hasOwner)).isEqualTo("owner");
    }
}