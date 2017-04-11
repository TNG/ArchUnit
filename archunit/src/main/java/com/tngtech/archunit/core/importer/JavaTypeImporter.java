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
