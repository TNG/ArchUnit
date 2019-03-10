package com.tngtech.archunit.library.dependencies;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SliceIdentifierTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_null() {
        thrown.expect(NullPointerException.class);

        SliceIdentifier.of((String[]) null);
    }

    @Test
    public void rejects_null_list() {
        thrown.expect(NullPointerException.class);

        SliceIdentifier.of((List<String>) null);
    }

    @Test
    public void rejects_empty_parts() {
        expectEmptyPartsException();

        SliceIdentifier.of();
    }

    @Test
    public void rejects_empty_parts_list() {
        expectEmptyPartsException();

        SliceIdentifier.of(Collections.<String>emptyList());
    }

    private void expectEmptyPartsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("empty");
        thrown.expectMessage("Use SliceIdentifier.ignore() to ignore a JavaClass");
    }
}