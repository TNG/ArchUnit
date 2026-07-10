package com.tngtech.archunit.lang.extension;

import java.io.File;

import com.tngtech.archunit.testutil.ReplaceFileExtension;

import static com.tngtech.archunit.testutil.TestUtils.toUri;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestServicesFile extends ReplaceFileExtension {
    public void addService(Class<? extends ArchUnitExtension> extensionClass) {
        File root = new File(toUri(getClass().getResource("/")));
        File metaInfServices = new File(new File(root, "META-INF"), "services");
        File extensionSPI = new File(metaInfServices, ArchUnitExtension.class.getName());
        appendLine(extensionSPI, extensionClass.getName(), UTF_8);
    }
}
