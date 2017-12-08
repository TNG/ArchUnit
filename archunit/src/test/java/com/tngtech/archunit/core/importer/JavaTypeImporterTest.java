package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaType;
import org.junit.Test;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaTypeImporterTest {

    @Test
    public void asm_object_type() {
        JavaType objectType = JavaTypeImporter.createFromAsmObjectTypeName("java/lang/Object");

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @Test
    public void asm_Type() throws NoSuchMethodException {
        Type toStringType = Type.getReturnType(Object.class.getDeclaredMethod("toString"));

        JavaType toStringJavaType = JavaTypeImporter.importAsmType(toStringType);

        assertThat(toStringJavaType.getName()).isEqualTo(String.class.getName());
        assertThat(toStringJavaType.resolveClass()).isEqualTo(String.class);
    }
}