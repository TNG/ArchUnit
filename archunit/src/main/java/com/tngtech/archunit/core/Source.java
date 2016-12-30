package com.tngtech.archunit.core;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

public class Source {
    private static final MessageDigest md5Digest = getMd5Digest();

    private static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private final URI uri;
    private final byte[] md5sum;

    Source(URI uri) {
        this.uri = uri;
        md5sum = createMd5sum(uri);
    }

    private byte[] createMd5sum(URI uri) {
        if (md5Digest == null) {
            return new byte[0];
        }

        Optional<byte[]> bytesFromUri = read(uri);
        return bytesFromUri.isPresent() ? md5Digest.digest(bytesFromUri.get()) : new byte[0];
    }

    private Optional<byte[]> read(URI uri) {
        try {
            return Optional.of(ByteStreams.toByteArray(uri.toURL().openStream()));
        } catch (IOException e) {
            return Optional.absent();
        }
    }

    public URI getUri() {
        return uri;
    }

    public byte[] getMd5sum() {
        return md5sum;
    }

    @Override
    public int hashCode() {
        return uri.hashCode() + 31 * Arrays.hashCode(md5sum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Source other = (Source) obj;
        return Objects.equals(this.uri, other.uri)
                && Arrays.equals(this.md5sum, other.md5sum);
    }

    @Override
    public String toString() {
        return uri + " [md5='" + HashCode.fromBytes(md5sum).toString() + "']";
    }
}
