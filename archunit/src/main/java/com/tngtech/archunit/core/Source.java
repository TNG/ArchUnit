package com.tngtech.archunit.core;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;

public class Source {
    private final URI uri;
    private final Md5sum md5sum;

    Source(URI uri) {
        this.uri = uri;
        md5sum = createMd5sum(uri);
    }

    private Md5sum createMd5sum(URI uri) {
        Optional<byte[]> bytesFromUri = read(uri);
        return bytesFromUri.isPresent() ? Md5sum.of(bytesFromUri.get()) : Md5sum.UNDETERMINED;
    }

    private Optional<byte[]> read(URI uri) {
        try {
            return Optional.of(ByteStreams.toByteArray(uri.toURL().openStream()));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public URI getUri() {
        return uri;
    }

    public Md5sum getMd5sum() {
        return md5sum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, md5sum);
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
                && Objects.equals(this.md5sum, other.md5sum);
    }

    @Override
    public String toString() {
        return uri + " [md5='" + md5sum + "']";
    }

    public static class Md5sum {
        /**
         * We can't determine the md5 sum, because the platform is missing the digest algorithm
         */
        static final Md5sum NOT_SUPPORTED = new Md5sum("NOT_SUPPORTED");
        /**
         * We can't determine the md5 sum, due to an error while digesting the source
         */
        static final Md5sum UNDETERMINED = new Md5sum("UNDETERMINED");

        private static final MessageDigest MD5_DIGEST = getMd5Digest();

        private static MessageDigest getMd5Digest() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        static Md5sum of(byte[] input) {
            return MD5_DIGEST != null ? new Md5sum(input, MD5_DIGEST) : NOT_SUPPORTED;
        }

        private final byte[] md5Bytes;
        private final String text;

        private Md5sum(String text) {
            this.md5Bytes = new byte[0];
            this.text = text;
        }

        private Md5sum(byte[] input, MessageDigest md5Digest) {
            this.md5Bytes = md5Digest.digest(input);
            text = HashCode.fromBytes(md5Bytes).toString();
        }

        public byte[] asBytes() {
            return md5Bytes;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(md5Bytes) + 31 * text.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Md5sum other = (Md5sum) obj;
            return Arrays.equals(this.md5Bytes, other.md5Bytes)
                    && Objects.equals(this.text, other.text);
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
