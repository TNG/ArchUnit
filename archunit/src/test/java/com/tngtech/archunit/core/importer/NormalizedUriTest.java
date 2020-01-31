package com.tngtech.archunit.core.importer;

import java.net.URI;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NormalizedUriTest {
    @Test
    public void normalizes_URI() {
        NormalizedUri uri = NormalizedUri.from(URI.create("prot:/some/.././uri/."));

        assertThat(uri.toURI()).isEqualTo(URI.create("prot:/uri/"));
    }

    @Test
    public void normalizes_URI_from_string() {
        NormalizedUri uri = NormalizedUri.from("prot:/some/../uri/.");

        assertThat(uri.toURI()).isEqualTo(URI.create("prot:/uri/"));
    }

    @Test
    public void parses_first_segment() {
        NormalizedUri uri = NormalizedUri.from("jrt:/java.base/java/io/File.class");

        assertThat(uri.getFirstSegment()).isEqualTo("java.base");

        uri = NormalizedUri.from("jrt:/java.base");

        assertThat(uri.getFirstSegment()).isEqualTo("java.base");
    }

    @Test
    public void parses_tail_segments() {
        NormalizedUri uri = NormalizedUri.from("jrt:/java.base/java/io/File.class");

        assertThat(uri.getTailSegments()).isEqualTo("java/io/File.class");

        uri = NormalizedUri.from("jrt:/java.base");

        assertThat(uri.getTailSegments()).isEmpty();
    }
}