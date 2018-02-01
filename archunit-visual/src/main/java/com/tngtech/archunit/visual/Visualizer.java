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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;

import com.google.common.io.ByteStreams;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.util.Collections.list;

@PublicAPI(usage = PublicAPI.Usage.ACCESS)
public final class Visualizer {

    private static final String JSON_FILE_NAME = "classes.json";
    private static final String VIOLATIONS_FILE_NAME = "violations.json";
    private static final String DIR = "report";

    // FIXME: Ugly, we don't want to create an empty EvaluationResult
    @PublicAPI(usage = PublicAPI.Usage.ACCESS)
    public void visualize(JavaClasses classes, final File targetDir) {
        visualize(classes, new EvaluationResult(classes().should().bePublic(), Priority.MEDIUM), targetDir, VisualizationContext.everything());
    }

    // FIXME: We want to visualize just classes without an EvaluationResult as well
    @PublicAPI(usage = PublicAPI.Usage.ACCESS)
    public void visualize(JavaClasses classes, EvaluationResult evaluationResult, final File targetDir, VisualizationContext context) {
        checkArgument(targetDir.exists() || targetDir.mkdirs(), "Can't create " + targetDir.getAbsolutePath());
        try (FileWriter classesWriter = new FileWriter(new File(targetDir, JSON_FILE_NAME))) {
            new JsonExporter().export(classes, classesWriter, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (FileWriter violationsWriter = new FileWriter(new File(targetDir, VIOLATIONS_FILE_NAME))) {
            new JsonViolationExporter().export(evaluationResult, violationsWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        URL url = getClass().getResource(DIR);
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
