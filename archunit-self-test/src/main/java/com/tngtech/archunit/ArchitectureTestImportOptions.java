package com.tngtech.archunit;

import java.util.regex.Pattern;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

public final class ArchitectureTestImportOptions {

    public static String sourceRootOf(Class<?> marker) {
        String classFile = "/" + marker.getName().replace('.', '/') + ".class";
        String file = marker.getResource(classFile).getFile();
        return file.substring(0, file.indexOf(classFile));
    }

    public static final class DoNotIncludeSelfTests implements ImportOption {
        private static final Pattern ARCH_TEST_OUTPUT = Pattern.compile(".*/build/classes/([^/]+/)?archTest/.*");
        private static final String SOURCE_ROOT = sourceRootOf(ArchitectureTestImportOptions.class);

        @Override
        public boolean includes(Location location) {
            return !location.contains(SOURCE_ROOT) && !location.matches(ARCH_TEST_OUTPUT);
        }
    }
}
