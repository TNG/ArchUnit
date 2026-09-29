package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaRecordComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotations;
import static com.tngtech.archunit.testutil.Assertions.assertThatThrownBy;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteParameterizedType.parameterizedType;

public class ClassFileImporterRecordsTest {
    @Target(ElementType.RECORD_COMPONENT)
    @Retention(RetentionPolicy.RUNTIME)
    @interface RecordComponentAnnotation {
        String value() default "someValue";
    }

    record SimpleRecord(Optional<String> component1, @RecordComponentAnnotation int component2) {
    }
    record EmptyRecord() {
    }

    @ParameterizedTest
    @ValueSource(classes = {SimpleRecord.class/*, EmptyRecord.class*/})
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

    @Test
    public void imports_record_components() {
        JavaClass javaClass = new ClassFileImporter().importClass(SimpleRecord.class);

        assertThat(javaClass.getRecordComponents()).isPresent();
        assertThat(javaClass.getRecordComponents().get())
                .extracting(JavaRecordComponent::getName)
                .containsExactlyInAnyOrder("component1", "component2");

        for (RecordComponent recordComponent : SimpleRecord.class.getRecordComponents()) {
            JavaRecordComponent component = getRecordComponentWithName(javaClass, recordComponent.getName());
            assertThat(component.getFullName())
                    .isEqualTo(SimpleRecord.class.getName() + "." + recordComponent.getName());
            assertThatType(component.getRawType()).matches(recordComponent.getType());
            assertThat(component.getAccessor()).isEquivalentTo(recordComponent.getAccessor());
        }

        JavaRecordComponent component1 = getRecordComponentWithName(javaClass, "component1");
        assertThatType(component1.getType()).matches(parameterizedType(Optional.class).withTypeArguments(String.class));

        JavaRecordComponent component2 = getRecordComponentWithName(javaClass, "component2");
        assertThat(component2.getType().getName()).isEqualTo(int.class.getName());
        assertThat(component2.getType()).isEqualTo(component2.getRawType());
    }

    @Test
    public void imports_record_with_no_components() {
        JavaClass javaClass = new ClassFileImporter().importClass(EmptyRecord.class);

        assertThat(javaClass.getRecordComponents()).isPresent();
        assertThat(javaClass.getRecordComponents().get()).isEmpty();
    }

    @Test
    public void imports_no_record_components_for_class_that_is_no_record() {
        JavaClass javaClass = new ClassFileImporter().importClass(ClassFileImporterRecordsTest.class);

        assertThat(javaClass.getRecordComponents()).isEmpty();
    }

    @Test
    public void imports_record_component_annotations() throws Exception {
        JavaClass javaClass = new ClassFileImporter().importClass(SimpleRecord.class);
        JavaRecordComponent component2 = getRecordComponentWithName(javaClass, "component2");
        RecordComponent reflectedComponent2 = getReflectedRecordComponentWithName(SimpleRecord.class, "component2");

        assertThat(component2.isAnnotatedWith(RecordComponentAnnotation.class)).isTrue();
        assertThat(component2.tryGetAnnotationOfType(RecordComponentAnnotation.class)).isPresent();
        assertThat(component2.getAnnotationOfType(RecordComponentAnnotation.class).value()).isEqualTo("someValue");
        assertThatAnnotations(component2.getAnnotations()).match(Arrays.asList(reflectedComponent2.getAnnotations()));
        assertThatAnnotation(component2.getAnnotationOfType(RecordComponentAnnotation.class.getName()))
                .matches(reflectedComponent2.getAnnotation(RecordComponentAnnotation.class));

        JavaRecordComponent component1 = getRecordComponentWithName(javaClass, "component1");
        assertThat(component1.tryGetAnnotationOfType(RecordComponentAnnotation.class)).isEmpty();
        assertThatThrownBy(() -> component1.getAnnotationOfType(RecordComponentAnnotation.class))
                .isInstanceOf(IllegalArgumentException.class);

        // the annotation is not propagated to the underlying field or the accessor, like the reflection API
        assertThat(javaClass.getField("component2").tryGetAnnotationOfType(RecordComponentAnnotation.class)).isEmpty();
        assertThat(SimpleRecord.class.getDeclaredField("component2").getAnnotations()).isEmpty();
        assertThat(javaClass.getMethod("component2").tryGetAnnotationOfType(RecordComponentAnnotation.class)).isEmpty();
        assertThat(SimpleRecord.class.getMethod("component2").getAnnotations()).isEmpty();
    }

    @Test
    public void record_component_accesses_to_self_delegate_to_the_underlying_field() {
        record RecordToImport(String component1, int component2) {
            int readComponent2() {
                return component2;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(RecordToImport.class);
        JavaRecordComponent component2 = getRecordComponentWithName(javaClass, "component2");

        assertThat(component2.getAccessesToSelf())
                .isEqualTo(javaClass.getField("component2").getAccessesToSelf());
        assertThat(component2.getAccessesToSelf())
                .extracting(access -> access.getOrigin().getName())
                .contains("readComponent2");
    }

    private static JavaRecordComponent getRecordComponentWithName(JavaClass javaClass, String name) {
        return javaClass.getRecordComponents().get().stream()
                .filter(component -> component.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No record component with name '" + name + "'"));
    }

    private static RecordComponent getReflectedRecordComponentWithName(Class<?> record, String name) {
        for (RecordComponent recordComponent : record.getRecordComponents()) {
            if (recordComponent.getName().equals(name)) {
                return recordComponent;
            }
        }
        throw new IllegalStateException("No record component with name '" + name + "'");
    }
}
