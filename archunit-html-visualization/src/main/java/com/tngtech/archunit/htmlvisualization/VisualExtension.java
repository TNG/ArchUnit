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
import java.util.Collection;
import java.util.Properties;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class VisualExtension implements ArchUnitExtension {
    private static final Logger LOG = LoggerFactory.getLogger(VisualExtension.class);
    private static final String VISUALIZATION_FILE_SYSTEM_PROPERTY = "archunit.htmlvisualization.targetfile";
    private static final String UNIQUE_IDENTIFIER = "archunit-html-visualization";

    private static Multimap<JavaClasses, EvaluationResult> evaluatedRules =
            Multimaps.synchronizedMultimap(HashMultimap.<JavaClasses, EvaluationResult>create());

    @Override
    public String getUniqueIdentifier() {
        return UNIQUE_IDENTIFIER;
    }

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
        evaluatedRules.put(evaluatedRule.getClasses(), evaluatedRule.getResult());
    }

    @Override
    public void onFinished(JavaClasses classes) {
        createVisualization(classes);
    }

    /**
     * Triggers this extension to handle the supplied classes, i.e. a report for these classes
     * for all stored {@link EvaluationResult EvaluationResults} will be written and the stored
     * {@link EvaluationResult EvaluationResults} for these classes will then be removed.<br><br>
     *
     * Note that ArchUnit test support (e.g. for JUnit 4 or JUnit 5) will trigger this method
     * automatically at the end of a test class.
     *
     * @param classes the classes that have been checked by some ArchUnit rules.
     */
    @PublicAPI(usage = ACCESS)
    public static void createVisualization(JavaClasses classes) {
        Collection<EvaluationResult> results = evaluatedRules.get(classes);
        Optional<File> visualizationTargetFile = getConfiguredVisualizationTargetFile();
        if (visualizationTargetFile.isPresent()) {
            LOG.info("Creating HTML visualization for {} evaluated rules to {}",
                    results.size(), visualizationTargetFile.get().getAbsolutePath());
            new Visualizer(classes, visualizationTargetFile.get()).visualize(results);
        } else {
            LOG.warn("No target file for HTML visualization was specified, skipping visualization. "
                            + "Provide the respective target HTML file to write the visualization to "
                            + "via system property -D{}=/some/target/file.hml",
                    VISUALIZATION_FILE_SYSTEM_PROPERTY);
        }
    }

    private static Optional<File> getConfiguredVisualizationTargetFile() {
        String configuredVisualizationFile = System.getProperty(VISUALIZATION_FILE_SYSTEM_PROPERTY);
        return !isNullOrEmpty(configuredVisualizationFile)
                ? Optional.of(new File(configuredVisualizationFile))
                : Optional.<File>absent();
    }
}