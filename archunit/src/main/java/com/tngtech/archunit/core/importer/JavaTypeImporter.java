/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.core.domain.JavaType;
import org.objectweb.asm.Type;

class JavaTypeImporter {
    /**
     * Takes an 'internal' ASM object type name, i.e. the class name but with slashes instead of periods,
     * i.e. java/lang/Object (note that this is not a descriptor like Ljava/lang/Object;)
     */
    static JavaType createFromAsmObjectTypeName(String objectTypeName) {
        return importAsmType(Type.getObjectType(objectTypeName));
    }

    static JavaType importAsmType(Type type) {
        return JavaType.From.name(type.getClassName());
    }
}
