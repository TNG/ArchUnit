/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.importer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState;

import static com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState.HAD_TO_BE_IMPORTED;

class DependencyResolutionProcess {
    private static final int maxRunsForMemberTypes = 1;
    private static final int maxRunsForAccessesToTypes = 1;
    private static final int maxRunsForSupertypes = -1;
    private static final int maxRunsForAnnotationTypes = -1;

    private Set<String> currentTypeNames = new HashSet<>();
    private int runNumber = 1;
    private boolean shouldContinue;

    void registerMemberType(String typeName) {
        if (runNumberHasNotExceeded(maxRunsForMemberTypes)) {
            currentTypeNames.add(typeName);
        }
    }

    void registerMemberTypes(Collection<String> typeNames) {
        for (String typeName : typeNames) {
            registerMemberType(typeName);
        }
    }

    void registerAccessToType(String typeName) {
        if (runNumberHasNotExceeded(maxRunsForAccessesToTypes)) {
            currentTypeNames.add(typeName);
        }
    }

    void registerSupertype(String typeName) {
        if (runNumberHasNotExceeded(maxRunsForSupertypes)) {
            currentTypeNames.add(typeName);
        }
    }

    void registerSupertypes(Collection<String> typeNames) {
        for (String typeName : typeNames) {
            registerSupertype(typeName);
        }
    }

    void registerAnnotationType(String typeName) {
        if (runNumberHasNotExceeded(maxRunsForAnnotationTypes)) {
            currentTypeNames.add(typeName);
        }
    }

    void resolve(ImportedClasses classes) {
        do {
            executeRun(classes);
        } while (shouldContinue);
    }

    private void executeRun(ImportedClasses classes) {
        runNumber++;
        Set<String> typeNamesToResolve = this.currentTypeNames;
        currentTypeNames = new HashSet<>();
        shouldContinue = false;
        for (String typeName : typeNamesToResolve) {
            ImportedClassState classState = classes.ensurePresent(typeName);
            shouldContinue = shouldContinue || (classState == HAD_TO_BE_IMPORTED);
        }
    }

    private boolean runNumberHasNotExceeded(int maxRuns) {
        return maxRuns < 0 || runNumber <= maxRuns;
    }
}
