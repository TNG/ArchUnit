package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.List;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class JavaGenericArrayTypeTest {

    @Test
    public void all_involved_raw_types_of_generic_array() {
        class SampleClass<T extends String & List<Serializable>> {
            @SuppressWarnings("unused")
            private T[][] field;
        }

        JavaGenericArrayType typeVariable = (JavaGenericArrayType) new ClassFileImporter().importClass(SampleClass.class).getField("field").getType();

        assertThatTypes(typeVariable.getAllInvolvedRawTypes()).matchInAnyOrder(String.class, List.class, Serializable.class);
    }
}