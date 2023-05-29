package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class JavaFieldTest {
    @Test
    public void offers_all_involved_raw_types() {
        class SomeClass {
            @SuppressWarnings("unused")
            List<? super Map<? extends Serializable, ? super Set<Number[][]>>> field;
        }

        JavaField field = new ClassFileImporter().importClass(SomeClass.class).getField("field");

        assertThatTypes(field.getAllInvolvedRawTypes()).matchInAnyOrder(List.class, Map.class, Serializable.class, Set.class, Number.class);
    }
}