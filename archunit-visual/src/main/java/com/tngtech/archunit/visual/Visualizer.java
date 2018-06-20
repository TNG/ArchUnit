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

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.list;

@PublicAPI(usage = ACCESS)
public final class Visualizer {
    private static final String DIR = "report";
    private static final String REPORT_FILE_NAME = "report.html";

    private final JavaClasses classes;
    private final File targetDir;
    private final VisualizationContext context;

    @PublicAPI(usage = ACCESS)
    public Visualizer(JavaClasses classes, final File targetDir, VisualizationContext context) {
        checkArgument(targetDir.exists() || targetDir.mkdirs(), "Can't create " + targetDir.getAbsolutePath());
        this.classes = classes;
        this.targetDir = targetDir;
        this.context = context;
    }

    @PublicAPI(usage = ACCESS)
    public Visualizer(JavaClasses classes, final File targetDir) {
        this(classes, targetDir, VisualizationContext.everything());
    }

    //TODO: method for only exporting violations

    @PublicAPI(usage = ACCESS)
    public void visualize() {
        copyFiles();
        ReportFile reportFile = new ReportFile();
        exportJson(reportFile);
        exportViolations(new ArrayList<EvaluationResult>(), reportFile);
        reportFile.write();
    }

    @PublicAPI(usage = ACCESS)
    public void visualize(Iterable<EvaluationResult> evaluationResults) {
        copyFiles();
        ReportFile reportFile = new ReportFile();
        exportJson(reportFile);
        exportViolations(evaluationResults, reportFile);
        reportFile.write();
    }

    private void exportJson(ReportFile reportFile) {
        reportFile.insertJsonRoot(new JsonExporter().exportToJson(classes, context));
    }

    private void exportViolations(Iterable<EvaluationResult> evaluationResults, ReportFile reportFile) {
        reportFile.insertJsonViolations(new JsonViolationExporter().exportToJson(evaluationResults));
    }

    //TODO: only copy report.html
    private void copyFiles() {
        URL url = getClass().getResource(DIR);
        //FIXME: the url null found when using the IntelliJ-Test-Runner
        try {
            createCopyFor(url).copyTo(targetDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ReportFile {
        private static final String JSON_ROOT_MARKER = "\"injectJsonClassesToVisualizeHere\"";
        private static final String JSON_VIOLATION_MARKER = "\"injectJsonViolationsToVisualizeHere\"";
        File file;
        String content;

        ReportFile() {
            file = new File(targetDir, REPORT_FILE_NAME);
            byte[] encodedContent;
            try {
                encodedContent = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            content = new String(encodedContent, Charsets.UTF_8);
            checkContent();
        }

        private int countOccurrencesInContent(String str) {
            return (str.length() - str.replace(str, "").length()) / str.length();
        }

        private void checkContent() {
            if (countOccurrencesInContent(JSON_ROOT_MARKER) > 1) {
                throw new RuntimeException(JSON_ROOT_MARKER + " may exactly occur once in " + REPORT_FILE_NAME);
            }
            if (countOccurrencesInContent(JSON_VIOLATION_MARKER) > 1) {
                throw new RuntimeException(JSON_VIOLATION_MARKER + " may exactly occur once in " + REPORT_FILE_NAME);
            }
        }

        private void insertJson(String stringToInsert, String stringToReplace) {
            content = content.replaceFirst(stringToReplace, "'" + Matcher.quoteReplacement(stringToInsert) + "'");
        }

        void insertJsonRoot(String jsonRoot) {
            insertJson(jsonRoot, JSON_ROOT_MARKER);
        }

        void insertJsonViolations(String jsonViolations) {
            insertJson(jsonViolations, JSON_VIOLATION_MARKER);
        }

        void write() {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8))) {
                writer.write(content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
