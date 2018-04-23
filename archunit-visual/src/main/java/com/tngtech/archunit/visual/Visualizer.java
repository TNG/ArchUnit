/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.visual;

import com.google.common.io.ByteStreams;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.list;

@PublicAPI(usage = ACCESS)
public final class Visualizer {

    private static final String ENCODING = "UTF-8";

    private static final String JSON_FILE_NAME = "classes.json";
    private static final String VIOLATIONS_FILE_NAME = "violations.json";
    private static final String DIR = "report";

    @PublicAPI(usage = ACCESS)
    public Visualizer() {
    }

    //TODO: method for only exporting violations

    @PublicAPI(usage = ACCESS)
    public void visualize(JavaClasses classes, final File targetDir, VisualizationContext context) {
        exportJson(classes, targetDir, context);
        copyFiles(targetDir);
    }

    @PublicAPI(usage = ACCESS)
    public void visualize(JavaClasses classes, final File targetDir) {
        visualize(classes, targetDir, VisualizationContext.everything());
    }

    @PublicAPI(usage = ACCESS)
    public void visualize(JavaClasses classes, final File targetDir, VisualizationContext context, Iterable<EvaluationResult> evaluationResults) {
        exportJson(classes, targetDir, context);
        exportViolations(targetDir, evaluationResults);
        copyFiles(targetDir);
    }

    private void exportJson(JavaClasses classes, final File targetDir, VisualizationContext context) {
        checkArgument(targetDir.exists() || targetDir.mkdirs(), "Can't create " + targetDir.getAbsolutePath());
        try (Writer classesWriter = new OutputStreamWriter(new FileOutputStream(new File(targetDir, JSON_FILE_NAME)), ENCODING)) {
            new JsonExporter().export(classes, classesWriter, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void exportViolations(final File targetDir, Iterable<EvaluationResult> evaluationResults) {
        File violationsFile = new File(targetDir, VIOLATIONS_FILE_NAME);
        try (Writer violationsWriter = new OutputStreamWriter(new FileOutputStream(violationsFile, false), ENCODING)) {
            if (violationsFile.exists()) {
                try (Reader violationsReader = new InputStreamReader(new FileInputStream(violationsFile), ENCODING)) {
                    new JsonViolationExporter().export(evaluationResults, violationsReader, violationsWriter);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                new JsonViolationExporter().export(evaluationResults, violationsWriter);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyFiles(final File targetDir) {
        URL url = getClass().getResource(DIR);
        //Problem: beim Ausführen der example-tests wird die Resource report nicht gefunden und deshalb das
        //ganze Zeug nicht kopiert...mit dem Gradle-Runner werden die tests nicht einmal ausgeführt (außer man
        //entfernt den test-Task im build.gradle, aber dann wird die Extension auch nicht ausgeführt...)
        System.out.println("Res url: " + url);
        try {
            createCopyFor(url).copyTo(targetDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Copy createCopyFor(URL url) {
        if (url.getProtocol().equals("file")) {
            return new Copy.FromFile(url);
        } else {
            return new Copy.FromJar(url);
        }
    }

    private static abstract class Copy {
        final URL url;

        Copy(URL url) {
            this.url = url;
        }

        abstract void copyTo(File targetDir) throws IOException;

        private static class FromFile extends Copy {
            FromFile(URL url) {
                super(url);
            }

            @Override
            public void copyTo(final File targetDir) throws IOException {
                Files.walkFileTree(Paths.get(url.getFile()),
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                                if (!path.toFile().isDirectory()) {
                                    com.google.common.io.Files.copy(path.toFile(), new File(targetDir, path.toFile().getName()));
                                }
                                return super.visitFile(path, basicFileAttributes);
                            }
                        });
            }
        }

        private static class FromJar extends Copy {
            FromJar(URL url) {
                super(url);
            }

            @Override
            public void copyTo(File targetDir) throws IOException {
                String folder = getClass().getPackage().getName().replace(".", "/") + "/" + "report";
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                for (JarEntry entry : list(connection.getJarFile().entries())) {
                    if (entry.getName().startsWith(folder) && !entry.isDirectory()) {
                        try (FileOutputStream to = new FileOutputStream(new File(targetDir, entry.getName().replaceAll(".*/", "")))) {
                            ByteStreams.copy(connection.getJarFile().getInputStream(entry), to);
                        }
                    }
                }
            }
        }
    }
}
