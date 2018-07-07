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
import com.google.common.base.Joiner;

import java.io.*;
import java.nio.file.Files;
import java.util.regex.Matcher;

class ReportFile {
    private static final String[] JSON_ROOT_MARKERS = {
            "\"injectJsonClassesToVisualizeHere\"",
            "'injectJsonClassesToVisualizeHere'"
    };
    private static final String[] JSON_VIOLATION_MARKERS = {
            "\"injectJsonViolationsToVisualizeHere\"",
            "'injectJsonViolationsToVisualizeHere'"
    };
    private File file;
    private String content;
    private String jsonRootMarker;
    private String jsonViolationMarker;

    ReportFile(File targetDir, String reportFileName) {
        file = new File(targetDir, reportFileName);
        byte[] encodedContent;
        try {
            encodedContent = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        content = new String(encodedContent, Charsets.UTF_8);
        defineJsonMarkers();
        checkContent();
    }

    private String findJsonMarker(String[] markers) {
        for (String s : markers) {
            if (content.contains(s)) {
                return s;
            }
        }
        throw new RuntimeException("The " + file.getName() + "-file does not contain one of " + Joiner.on(", ").join(markers));
    }

    private void defineJsonMarkers() {
        jsonRootMarker = findJsonMarker(JSON_ROOT_MARKERS);
        jsonViolationMarker = findJsonMarker(JSON_VIOLATION_MARKERS);
    }

    private int countOccurrencesInContent(String str) {
        return (content.length() - content.replace(str, "").length()) / str.length();
    }

    private void checkContent() {
        if (countOccurrencesInContent(jsonRootMarker) > 1) {
            throw new RuntimeException(jsonRootMarker + " may exactly occur once in " + file.getName());
        }
        if (countOccurrencesInContent(jsonViolationMarker) != 1) {
            throw new RuntimeException(jsonViolationMarker + " may exactly occur once in " + file.getName());
        }
    }

    private void insertJson(String stringToInsert, String stringToReplace) {
        content = content.replaceFirst(stringToReplace, "'" + Matcher.quoteReplacement(stringToInsert) + "'");
    }

    void insertJsonRoot(String jsonRoot) {
        insertJson(jsonRoot, jsonRootMarker);
    }

    void insertJsonViolations(String jsonViolations) {
        insertJson(jsonViolations, jsonViolationMarker);
    }

    void write() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8))) {
            writer.write(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
