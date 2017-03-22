package com.tngtech.archunit.visual;

import com.tngtech.archunit.core.JavaClasses;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Visualizer {
    public Visualizer() {

    }

    public void visualize(JavaClasses classes, final File targetDir) {
        targetDir.mkdirs();
        new JsonExporter().export(classes,
                new File(targetDir, "classes.json"),
                new VisualizationContextBuilder().includeOnly("com.tngtech.archunit.visual"
                        , "java.io.File").build());

        try {
            Files.walkFileTree(Paths.get(getClass().getResource("./report").toURI()), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    if (!path.toFile().isDirectory()) {
                        com.google.common.io.Files.copy(path.toFile(), new File(targetDir, path.toFile().getName()));
                    }
                    return super.visitFile(path, basicFileAttributes);
                }
            });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
