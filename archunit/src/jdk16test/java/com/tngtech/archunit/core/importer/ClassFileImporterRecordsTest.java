package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ClassFileImporterRecordsTest {
    record SimpleRecord(String component1, int component2) {
    }
    record EmptyRecord() {
    }

    @ParameterizedTest
    @ValueSource(classes = {SimpleRecord.class, EmptyRecord.class})
    void imports_record(Class<?> recordToImport) {
        JavaClass javaClass = new ClassFileImporter().importClasses(recordToImport, Record.class).get(recordToImport);

        assertThat(javaClass)
                .matches(recordToImport)
                .hasRawSuperclassMatching(Record.class)
                .hasNoInterfaces()
                .isInterface(false)
                .isEnum(false)
                .isAnnotation(false)
                .isRecord(true);
    }

    @Test
    public void imports_constructor() throws Exception {
        record RecordToImport(String component1, int component2) {
        }

        JavaClass javaClass = new ClassFileImporter().importClass(RecordToImport.class);
        JavaConstructor constructor = javaClass.getConstructor(String.class, int.class);
        assertThat(constructor).isEquivalentTo(RecordToImport.class.getDeclaredConstructor(String.class, int.class));
    }

    @Test
    public void imports_record_component_fields() throws Exception {
        record RecordToImport(String component1, int component2) {
        }

        JavaClass javaClass = new ClassFileImporter().importClass(RecordToImport.class);
        JavaField component1Field = javaClass.getField("component1");
        assertThat(component1Field).isEquivalentTo(RecordToImport.class.getDeclaredField("component1"));
    }

    @Test
    public void imports_record_component_getters() throws Exception {
        record RecordToImport(String component1, int component2) {
        }

        JavaClass javaClass = new ClassFileImporter().importClass(RecordToImport.class);
        JavaMethod component1Method = javaClass.getMethod("component1");
        assertThat(component1Method).isEquivalentTo(RecordToImport.class.getMethod("component1"));
    }
}
