/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;

interface RawCodeUnitDependencyBuilder<CODE_UNIT_DEPENDENCY extends RawCodeUnitDependency<TARGET>, TARGET> {
    RawCodeUnitDependencyBuilder<CODE_UNIT_DEPENDENCY, TARGET> withOrigin(CodeUnit origin);

    RawCodeUnitDependencyBuilder<CODE_UNIT_DEPENDENCY, TARGET> withTarget(TARGET target);

    RawCodeUnitDependencyBuilder<CODE_UNIT_DEPENDENCY, TARGET> withLineNumber(int lineNumber);

    RawCodeUnitDependencyBuilder<CODE_UNIT_DEPENDENCY, TARGET> withDeclaredInLambda(boolean declaredInLambda);

    CODE_UNIT_DEPENDENCY build();
}
