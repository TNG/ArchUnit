package com.tngtech.archunit.lang.extension;

import java.io.File;

import com.tngtech.archunit.testutil.ReplaceFileRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestServicesFile implements TestRule {
    private final ReplaceFileRule replaceFile = new ReplaceFileRule();

    @Override
    public Statement apply(Statement base, Description description) {
        return replaceFile.apply(base, description);
    }

    public void addService(Class<? extends ArchUnitExtension> extensionClass) {
        File root = new File(getClass().getResource("/").getFile());
        File metaInfServices = new File(new File(root, "META-INF"), "services");
        File extensionSPI = new File(metaInfServices, ArchUnitExtension.class.getName());
        replaceFile.appendLine(extensionSPI, extensionClass.getName(), UTF_8);
    }
}
