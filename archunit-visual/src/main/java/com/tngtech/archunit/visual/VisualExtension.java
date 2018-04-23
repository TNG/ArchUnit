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
import java.util.Arrays;
import java.util.Properties;

import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

public class VisualExtension implements ArchUnitExtension {
    private static final String REPORT_DIR_SYSTEM_PROPERTY = "archunit.visual.report.dir";
    private static final String UNIQUE_IDENTIFIER = "archunit-visual";

    private File targetDirectory;

    @Override
    public String getUniqueIdentifier() {
        return UNIQUE_IDENTIFIER;
    }

    @Override
    public void configure(Properties properties) {
        String configuredReportDir = System.getProperty(REPORT_DIR_SYSTEM_PROPERTY);
        targetDirectory = configuredReportDir != null
                ? new File(configuredReportDir)
                : new File(getClass().getResource("/").getFile(), "archunit-report");
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
        System.out.println("export violations...");
        new Visualizer().visualize(evaluatedRule.getClasses(), targetDirectory,
                VisualizationContext.everything(), Arrays.asList(evaluatedRule.getResult()));
    }
}
