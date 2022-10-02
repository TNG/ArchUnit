package com.tngtech.archunit.library.dependencies;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SliceIdentifierTest {

    @Test
    public void rejects_null() {
        assertThatThrownBy(() -> SliceIdentifier.of((String[]) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void rejects_null_list() {
        assertThatThrownBy(() -> SliceIdentifier.of((List<String>) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void rejects_empty_parts() {
        expectEmptyPartsException(SliceIdentifier::of);
    }

    @Test
    public void rejects_empty_parts_list() {
        expectEmptyPartsException(() -> SliceIdentifier.of(emptyList()));
    }

    private void expectEmptyPartsException(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty")
                .hasMessageContaining("Use SliceIdentifier.ignore() to ignore a JavaClass");
    }
}
