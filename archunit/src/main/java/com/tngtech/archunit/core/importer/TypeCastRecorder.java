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

class TypeCastRecorder {
    private static final String CLASS_INTERNAL_NAME = "java/lang/Class";
    private static final String METHOD_DESCRIPTOR = "(Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String CAST_METHOD_NAME = "cast";

    private boolean implicit;
    
    void reset() {
        implicit = false;
    }

    void registerMethodInstruction(String owner, String name, String desc) {
        implicit = !CLASS_INTERNAL_NAME.equals(owner) || !CAST_METHOD_NAME.equals(name) || !METHOD_DESCRIPTOR.equals(desc);
    }

    boolean isImplicit() {
        return implicit;
    }
}
