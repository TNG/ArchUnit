package com.tngtech.archunit.core.importer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.core.importer.ConstructorParametersAssertion.assertThatParametersOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.TestUtils.currentJavaMajorVersion;
import static com.tngtech.archunit.testutil.assertion.ExpectedConcreteType.ExpectedConcreteClass.concreteClass;
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
    // on the parameters agree with each other and with the Reflection API. The only trace the compiler leaves is the 'mandated'
    // flag that javac 21+ writes into the MethodParameters attribute for the parameters of a compact canonical constructor
    // (compare JDK-8320575), which the Reflection API reports via Parameter.isImplicit(); javac 16 to 20 write no flags at all.

    // the annotation is applicable to parameters, so it is propagated from the record component to the parameter of the canonical constructor
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
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

    record RecordWithExplicitCanonicalConstructor(String component) {
        RecordWithExplicitCanonicalConstructor(String component) {
            this.component = Objects.requireNonNull(component);
        }
    }

    @Test
    public void imports_parameters_of_implicit_canonical_constructor_like_Reflection_API() {
        assertThatParametersOf(SimpleRecord.class)
                .haveRawTypes(String.class, int.class)
                .haveTypes(concreteClass(String.class), concreteClass(int.class))
                .haveCount(2)
                .haveSameCountAsReflection()
                .reflectionReportsParametersAsImplicit(false, false);
    }

    @Test
    public void imports_parameters_of_canonical_constructor_with_generic_signature_like_Reflection_API() {
        assertThatParametersOf(RecordWithGenericComponent.class)
                .haveRawTypes(List.class)
                .haveTypes(parameterizedType(List.class).withTypeArguments(String.class))
                .haveCount(1)
                .haveSameCountAsReflection()
                .haveAnnotationOnlyOnParameter(0, SomeComponentAnnotation.class)
                .haveAnnotationOnlyOnParameterLikeReflection(0);
    }

    @Test
    public void imports_parameters_of_canonical_constructor_of_generic_record_like_Reflection_API() {
        assertThatParametersOf(GenericRecord.class)
                .haveRawTypes(Object.class)
                .haveTypes(typeVariable("T"))
                .haveCount(1)
                .haveSameCountAsReflection();
    }

    @Test
    public void imports_parameters_of_compact_canonical_constructor_like_Reflection_API() {
        // the tests are compiled and run with the same JDK; javac 21+ flags the parameters of a compact canonical constructor
        // as mandated, while javac 16 to 20 write no flags at all
        boolean compactConstructorParametersAreFlaggedMandated = currentJavaMajorVersion() >= 21;

        assertThatParametersOf(RecordWithCompactCanonicalConstructor.class)
                .haveRawTypes(String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveSameCountAsReflection()
                .reflectionReportsParametersAsImplicit(compactConstructorParametersAreFlaggedMandated);
    }

    @Test
    public void imports_parameters_of_explicit_canonical_constructor_like_Reflection_API() {
        assertThatParametersOf(RecordWithExplicitCanonicalConstructor.class)
                .haveRawTypes(String.class)
                .haveTypes(concreteClass(String.class))
                .haveCount(1)
                .haveSameCountAsReflection()
                .reflectionReportsParametersAsImplicit(false);
    }
}
