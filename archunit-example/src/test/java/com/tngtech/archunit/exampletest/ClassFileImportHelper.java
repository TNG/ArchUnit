package com.tngtech.archunit.exampletest;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

class ClassFileImportHelper {
    private final ClassFileImporter importer = new ClassFileImporter();

    JavaClasses importTreesOf(Class<?>... rootClasses) {
        try {
            return uncheckedImportTreesOf(rootClasses);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JavaClasses uncheckedImportTreesOf(Class<?>... rootClasses) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (Class<?> rootClass : rootClasses) {
            String packageDirName = String.format("/%s", rootClass.getPackage().getName().replace('.', '/'));
            urls.add(rootClass.getResource(packageDirName));
        }
        return importer.importUrls(urls);
    }
}
