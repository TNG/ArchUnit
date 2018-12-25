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
package com.tngtech.archunit.htmlvisualization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@PublicAPI(usage = ACCESS)
public final class Visualizer {
    private static final String VISUALIZATION_FILE_NAME = "visualization.html";

    private final JsonExporter jsonExporter = new JsonExporter();

    private final JavaClasses classes;
    private final File targetFile;

    @PublicAPI(usage = ACCESS)
    public Visualizer(JavaClasses classes, final File targetFile) {
        File targetDir = targetFile.getParentFile();
        checkArgument(targetDir.exists() || targetDir.mkdirs(), "Can't create %s", targetDir.getAbsolutePath());

        this.classes = classes;
        this.targetFile = targetFile;
    }

    @PublicAPI(usage = ACCESS)
    public void visualize() {
        visualize(new ArrayList<EvaluationResult>());
    }

    @PublicAPI(usage = ACCESS)
    public void visualize(Iterable<EvaluationResult> evaluationResults) {
        copyFiles();
        VisualizationFile visualizationFile = new VisualizationFile(targetFile);
        visualizationFile.insertJsonRoot(jsonExporter.exportToJson(classes));
        visualizationFile.insertJsonViolations(jsonExporter.exportToJson(evaluationResults));
        visualizationFile.write();
    }

    private void copyFiles() {
        URL url = getClass().getResource(VISUALIZATION_FILE_NAME);
        //FIXME: the url null found when using the IntelliJ-Test-Runner
        // -> hard to fix out of the box, the bundled visualization is missing from IntelliJ's out folder
        //    either copy it manually or set up JUnit execution to copy it before, but no matter what it's not working without manual tweaking
        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, targetFile.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new VisualizationCreationException(e);
        }
    }
}
