package com.tngtech.archunit.core;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SourceTest {
    @DataProvider
    public static Object[][] classes() {
        return $$($(fileUrl()), $(jarUrl()));
    }

    @Test
    @UseDataProvider("classes")
    public void calculates_md5_correctly(URL url) throws Exception {
        Source source = new Source(url.toURI());

        assertThat(source.getUri()).as("source URI").isEqualTo(url.toURI());
        assertThat(source.getMd5sum()).isEqualTo(expectedMd5sumOf(url));
    }

    @Test
    @UseDataProvider("classes")
    public void equals_hashcode_and_toString(URL url) throws Exception {
        Source source = new Source(url.toURI());
        Source equalSource = new Source(url.toURI());

        assertThat(source).as("source").isEqualTo(equalSource);
        assertThat(source.hashCode()).as("hashcode").isEqualTo(equalSource.hashCode());
        assertThat(source).as("source").isNotEqualTo(new Source(urlOf(Object.class).toURI()));
        String expectedToString = String.format("%s [md5='%s']", url, hexStringOf(expectedMd5sumOf(url)));
        assertThat(source.toString()).as("source.toString()").isEqualTo(expectedToString);
    }

    private String hexStringOf(byte[] bytes) {
        return HashCode.fromBytes(bytes).toString();
    }

    static byte[] expectedMd5sumOf(URL url) throws IOException, NoSuchAlgorithmException {
        byte[] bytes = ByteStreams.toByteArray(url.openStream());
        return MessageDigest.getInstance("MD5").digest(bytes);
    }

    private static Object fileUrl() {
        return urlOf(SourceTest.class);
    }

    private static Object jarUrl() {
        return urlOf(Rule.class);
    }

    static URL urlOf(Class<?> clazz) {
        return checkNotNull(SourceTest.class.getResource("/" + clazz.getName().replace('.', '/') + ".class"),
                "Can't determine url of %s", clazz.getName());
    }
}