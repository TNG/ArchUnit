package com.tngtech.archunit.core.importer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteTypeVariable.typeVariable;

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

    // The following tests document the constructor parameters of records for #1772: the compiler never adds parameters
    // to the canonical constructor (records are implicitly static and cannot capture local variables), so all three views
    // on the parameters agree with each other and with the Reflection API. Note that javac 21+ nevertheless flags the
    // parameters of the canonical constructor as 'mandated' in the MethodParameters attribute, while listing them
    // in the Signature attribute (compare JDK-8320575).

    @Retention(RetentionPolicy.RUNTIME)
    @interface SomeComponentAnnotation {
    }

    record RecordWithGenericComponent(@SomeComponentAnnotation List<String> component) {
    }

    record GenericRecord<T>(T component) {
    }

    record RecordWithCompactCanonicalConstructor(String component) {
        RecordWithCompactCanonicalConstructor {
            Objects.requireNonNull(component);
        }
    }

    @Test
    public void imports_parameters_of_canonical_constructor_like_Reflection_API() {
        JavaConstructor constructor = importOnlyConstructor(SimpleRecord.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(String.class, int.class);
        assertThatTypes(constructor.getParameterTypes()).matchExactly(String.class, int.class);
        assertThat(constructor.getParameters()).hasSize(2);
        assertThat(constructor.getParameters()).hasSize(constructor.reflect().getParameterCount());
    }

    @Test
    public void imports_parameters_of_canonical_constructor_with_generic_signature_like_Reflection_API() {
        JavaConstructor constructor = importOnlyConstructor(RecordWithGenericComponent.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(List.class);
        assertThatTypes(constructor.getParameterTypes()).matchExactly(parameterizedType(List.class).withTypeArguments(String.class));
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameters()).hasSize(constructor.reflect().getParameterCount());

        // the annotation of the record component is propagated to the parameter of the canonical constructor
        assertThatType(getOnlyElement(constructor.getParameters().get(0).getAnnotations()).getRawType()).matches(SomeComponentAnnotation.class);
        assertThat(constructor.reflect().getParameterAnnotations()[0]).hasSize(1);
    }

    @Test
    public void imports_parameters_of_canonical_constructor_of_generic_record() {
        JavaConstructor constructor = importOnlyConstructor(GenericRecord.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(Object.class);
        assertThatTypes(constructor.getParameterTypes()).matchExactly(typeVariable("T"));
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameters()).hasSize(constructor.reflect().getParameterCount());
    }

    @Test
    public void imports_parameters_of_compact_canonical_constructor_like_Reflection_API() {
        JavaConstructor constructor = importOnlyConstructor(RecordWithCompactCanonicalConstructor.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(String.class);
        assertThatTypes(constructor.getParameterTypes()).matchExactly(String.class);
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameters()).hasSize(constructor.reflect().getParameterCount());
    }

    @Test
    public void imports_parameters_of_canonical_constructor_of_local_record_without_enclosing_instance() {
        String notCapturable = "local records cannot capture local variables";
        record LocalRecord(String component) {
        }

        JavaConstructor constructor = importOnlyConstructor(LocalRecord.class);

        assertThatTypes(constructor.getRawParameterTypes()).matchExactly(String.class);
        assertThatTypes(constructor.getParameterTypes()).matchExactly(String.class);
        assertThat(constructor.getParameters()).hasSize(1);
        assertThat(constructor.getParameters()).hasSize(constructor.reflect().getParameterCount());
    }

    private static JavaConstructor importOnlyConstructor(Class<?> owner) {
        return getOnlyElement(new ClassFileImporter().importClass(owner).getConstructors());
    }
}
