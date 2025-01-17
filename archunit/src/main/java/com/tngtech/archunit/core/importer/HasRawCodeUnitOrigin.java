/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

interface HasRawCodeUnitOrigin {
    RawAccessRecord.CodeUnit getOrigin();

    boolean isDeclaredInLambda();

    interface Builder<HAS_RAW_CODE_UNIT_ORIGIN> {

        Builder<HAS_RAW_CODE_UNIT_ORIGIN> withOrigin(RawAccessRecord.CodeUnit origin);

        Builder<HAS_RAW_CODE_UNIT_ORIGIN> withDeclaredInLambda(boolean declaredInLambda);

        HAS_RAW_CODE_UNIT_ORIGIN build();
    }
}
