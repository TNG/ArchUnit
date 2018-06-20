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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.JarEntry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

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
        visualize(new ArrayList<EvaluationResult>());
    }

    @PublicAPI(usage = ACCESS)
    public void visualize(Iterable<EvaluationResult> evaluationResults) {
        copyFiles();
        ReportFile reportFile = new ReportFile(targetDir, REPORT_FILE_NAME);
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

    private void copyFiles() {
        URL url = getClass().getResource(DIR + "/" + REPORT_FILE_NAME);
        //FIXME: the url null found when using the IntelliJ-Test-Runner
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
                File file = new File(url.getFile());
                com.google.common.io.Files.copy(file, new File(targetDir, file.getName()));
            }
        }

        //TODO: test this
        private static class FromJar extends Copy {
            FromJar(URL url) {
                super(url);
            }

            @Override
            public void copyTo(File targetDir) throws IOException {
                String folder = getClass().getPackage().getName().replace(".", "/") + "/" + "report";
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                JarEntry entry = connection.getJarEntry();
                if (entry.getName().startsWith(folder)) {
                    try (FileOutputStream to = new FileOutputStream(new File(targetDir, entry.getName().replaceAll(".*/", "")))) {
                        ByteStreams.copy(connection.getJarFile().getInputStream(entry), to);
                    }
                }
            }
        }
    }
}
