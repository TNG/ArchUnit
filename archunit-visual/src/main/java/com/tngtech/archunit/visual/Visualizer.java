package com.tngtech.archunit.visual;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.tngtech.archunit.core.domain.JavaClasses;

public class Visualizer {

    private static final String JSONFILENAME = "classes.json";
    private static final String DIR = "./report";

    public Visualizer() {

    }

    public void visualize(JavaClasses classes, final File targetDir, VisualizationContext context) {
        targetDir.mkdirs();
        new JsonExporter().export(classes, new File(targetDir, JSONFILENAME), context);

        try {
            Files.walkFileTree(Paths.get(getClass().getResource(DIR).toURI()),
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                            if (!path.toFile().isDirectory()) {
                                com.google.common.io.Files.copy(path.toFile(), new File(targetDir, path.toFile().getName()));
                            }
                            return super.visitFile(path, basicFileAttributes);
                        }
                    });
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
