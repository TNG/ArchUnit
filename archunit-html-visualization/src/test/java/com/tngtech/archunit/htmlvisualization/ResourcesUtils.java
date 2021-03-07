package com.tngtech.archunit.htmlvisualization;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

final class ResourcesUtils {

    static String getResourceText(Class<?> resourceRelativeClass, String resourceName) {
        try (InputStream inputStream = resourceRelativeClass.getResourceAsStream(resourceName)) {
            checkNotNull(inputStream, "Can't find resource '%s' relative to class %s", resourceName, resourceRelativeClass);
            return new String(ByteStreams.toByteArray(inputStream), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
