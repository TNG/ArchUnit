package com.tngtech.archunit.testutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.rules.ExternalResource;

public class TransientCopyRule extends ExternalResource {
    private List<File> created = new ArrayList<>();

    public void copy(File from, File targetDir) throws IOException {
        File target = targetDir.toPath().resolve(from.getName()).toFile();
        Files.copy(from, target);
        created.add(target);
    }

    @Override
    protected void after() {
        for (File file : Lists.reverse(created)) {
            file.delete();
        }
    }
}
