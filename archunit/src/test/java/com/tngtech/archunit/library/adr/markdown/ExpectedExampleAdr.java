package com.tngtech.archunit.library.adr.markdown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class ExpectedExampleAdr {
    private final String resource;

    public ExpectedExampleAdr(final String resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream(this.resource)) {
            assert is != null;
            return new BufferedReader(
                    new InputStreamReader(is)
            ).lines().collect(
                    Collectors.joining("\n")
            );
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
