package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import org.junit.Test;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaClassDescriptorImporterTest {

    @Test
    public void asm_object_type() {
        JavaClassDescriptor objectType = JavaClassDescriptorImporter.createFromAsmObjectTypeName("java/lang/Object");

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @Test
    public void asm_Type() throws NoSuchMethodException {
        Type toStringType = Type.getReturnType(Object.class.getDeclaredMethod("toString"));

        JavaClassDescriptor toStringDescriptor = JavaClassDescriptorImporter.importAsmType(toStringType);

        assertThat(toStringDescriptor.getFullyQualifiedClassName()).isEqualTo(String.class.getName());
        assertThat(toStringDescriptor.resolveClass()).isEqualTo(String.class);
    }
}