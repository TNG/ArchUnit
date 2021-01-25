package com.tngtech.archunit.testutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkArgument;

public class TransientCopyRule extends ExternalResource {
    private final List<File> created = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    public void copy(File from, File targetDir) {
        if (from.isDirectory()) {
            for (File file : from.listFiles()) {
                copyFile(file, targetDir);
            }
        } else {
            copyFile(from, targetDir);
        }
    }

    private void copyFile(File from, File targetDir) {
        checkArgument(from.isFile(), "source must be a file");

        File target = targetDir.toPath().resolve(from.getName()).toFile();
        try {
            Files.copy(from, target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        created.add(target);
    }

    @Override
    protected void after() {
        for (File file : Lists.reverse(created)) {
            file.delete();
        }
        created.clear();
    }
}
