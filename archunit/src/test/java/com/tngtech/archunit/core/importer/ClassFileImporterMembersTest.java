package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.FilterInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.importer.testexamples.FirstCheckedException;
import com.tngtech.archunit.core.importer.testexamples.OtherClass;
import com.tngtech.archunit.core.importer.testexamples.SecondCheckedException;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.BaseClass;
import com.tngtech.archunit.core.importer.testexamples.classhierarchyimport.Subclass;
import com.tngtech.archunit.core.importer.testexamples.complexmethodimport.ClassWithComplexMethod;
import com.tngtech.archunit.core.importer.testexamples.constructorimport.ClassWithComplexConstructor;
import com.tngtech.archunit.core.importer.testexamples.constructorimport.ClassWithSimpleConstructors;
import com.tngtech.archunit.core.importer.testexamples.constructorimport.ClassWithThrowingConstructor;
import com.tngtech.archunit.core.importer.testexamples.fieldimport.ClassWithIntAndObjectFields;
import com.tngtech.archunit.core.importer.testexamples.fieldimport.ClassWithStringField;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.ChecksInstanceofInConstructor;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.ChecksInstanceofInMethod;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.ChecksInstanceofInStaticInitializer;
import com.tngtech.archunit.core.importer.testexamples.instanceofcheck.InstanceofChecked;
import com.tngtech.archunit.core.importer.testexamples.methodimport.ClassWithMultipleMethods;
import com.tngtech.archunit.core.importer.testexamples.methodimport.ClassWithObjectVoidAndIntIntSerializableMethod;
import com.tngtech.archunit.core.importer.testexamples.methodimport.ClassWithStringStringMethod;
import com.tngtech.archunit.core.importer.testexamples.methodimport.ClassWithThrowingMethod;
import com.tngtech.archunit.core.importer.testexamples.referencedclassobjects.ReferencingClassObjects;
import com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion;
import org.assertj.core.util.Objects;
import org.junit.Test;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.domain.JavaModifier.TRANSIENT;
import static com.tngtech.archunit.core.domain.JavaModifier.VOLATILE;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.findAnyByName;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.getCodeUnits;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.getFields;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.getMethods;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatReferencedClassObjects;
import static com.tngtech.archunit.testutil.Assertions.assertThatThrowsClause;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.assertion.ReferencedClassObjectsAssertion.referencedClassObject;

public class ClassFileImporterMembersTest {

    @Test
    public void imports_fields() {
        Set<JavaField> fields = getFields(new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldimport")));

        assertThat(namesOf(fields)).containsOnly("stringField", "serializableField", "objectField");
        assertThat(findAnyByName(fields, "stringField"))
                .isEquivalentTo(field(ClassWithStringField.class, "stringField"));
        assertThat(findAnyByName(fields, "serializableField"))
                .isEquivalentTo(field(ClassWithIntAndObjectFields.class, "serializableField"));
        assertThat(findAnyByName(fields, "objectField"))
                .isEquivalentTo(field(ClassWithIntAndObjectFields.class, "objectField"));
    }

    @Test
    public void imports_primitive_fields() {
        Set<JavaField> fields = getFields(new ClassFileImporter().importUrl(getClass().getResource("testexamples/primitivefieldimport")));

        assertThatType(findAnyByName(fields, "aBoolean").getRawType()).matches(boolean.class);
        assertThatType(findAnyByName(fields, "anInt").getRawType()).matches(int.class);
        assertThatType(findAnyByName(fields, "aByte").getRawType()).matches(byte.class);
        assertThatType(findAnyByName(fields, "aChar").getRawType()).matches(char.class);
        assertThatType(findAnyByName(fields, "aShort").getRawType()).matches(short.class);
        assertThatType(findAnyByName(fields, "aLong").getRawType()).matches(long.class);
        assertThatType(findAnyByName(fields, "aFloat").getRawType()).matches(float.class);
        assertThatType(findAnyByName(fields, "aDouble").getRawType()).matches(double.class);
    }

    @Test
    public void attaches_correct_owner_to_fields() {
        Iterable<JavaClass> classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldimport"));

        for (JavaClass clazz : classes) {
            for (JavaField field : clazz.getFields()) {
                assertThat(field.getOwner()).isSameAs(clazz);
            }
        }
    }

    @Test
    public void imports_fields_with_correct_modifiers() {
        Set<JavaField> fields = getFields(new ClassFileImporter().importUrl(getClass().getResource("testexamples/modifierfieldimport")));

        assertThat(findAnyByName(fields, "privateField").getModifiers()).containsOnly(PRIVATE);
        assertThat(findAnyByName(fields, "defaultField").getModifiers()).isEmpty();
        assertThat(findAnyByName(fields, "privateFinalField").getModifiers()).containsOnly(PRIVATE, FINAL);
        assertThat(findAnyByName(fields, "privateStaticField").getModifiers()).containsOnly(PRIVATE, STATIC);
        assertThat(findAnyByName(fields, "privateStaticFinalField").getModifiers()).containsOnly(PRIVATE, STATIC, FINAL);
        assertThat(findAnyByName(fields, "staticDefaultField").getModifiers()).containsOnly(STATIC);
        assertThat(findAnyByName(fields, "protectedField").getModifiers()).containsOnly(PROTECTED);
        assertThat(findAnyByName(fields, "protectedFinalField").getModifiers()).containsOnly(PROTECTED, FINAL);
        assertThat(findAnyByName(fields, "publicField").getModifiers()).containsOnly(PUBLIC);
        assertThat(findAnyByName(fields, "publicStaticFinalField").getModifiers()).containsOnly(PUBLIC, STATIC, FINAL);
        assertThat(findAnyByName(fields, "volatileField").getModifiers()).containsOnly(VOLATILE);
        assertThat(findAnyByName(fields, "synchronizedField").getModifiers()).containsOnly(TRANSIENT);
    }

    @Test
    public void imports_simple_methods_with_correct_parameters() throws Exception {
        Set<JavaMethod> methods = getMethods(new ClassFileImporter().importUrl(getClass().getResource("testexamples/methodimport")));
        assertThat(methods).extractingResultOf("getDefaultValue").containsOnly(Optional.empty());

        assertThat(findAnyByName(methods, "createString")).isEquivalentTo(
                ClassWithStringStringMethod.class.getDeclaredMethod("createString", String.class));
        assertThat(findAnyByName(methods, "consume")).isEquivalentTo(
                ClassWithObjectVoidAndIntIntSerializableMethod.class.getDeclaredMethod("consume", Object.class));
        assertThat(findAnyByName(methods, "createSerializable")).isEquivalentTo(
                ClassWithObjectVoidAndIntIntSerializableMethod.class
                        .getDeclaredMethod("createSerializable", int.class, int.class));
    }

    @Test
    public void imports_complex_method_with_correct_parameters() throws Exception {
        JavaClass clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/complexmethodimport")).get(ClassWithComplexMethod.class);

        assertThat(clazz.getMethods()).as("Methods of %s", ClassWithComplexMethod.class.getSimpleName()).hasSize(1);

        Class<?>[] parameterTypes = {String.class, long.class, long.class, Serializable.class, Serializable.class};
        Method expectedMethod = ClassWithComplexMethod.class.getDeclaredMethod("complex", parameterTypes);

        assertThat(clazz.getMethod("complex", parameterTypes)).isEquivalentTo(expectedMethod);
        assertThat(clazz.tryGetMethod("complex", parameterTypes).get()).isEquivalentTo(expectedMethod);
        assertThat(clazz.getMethod("complex", Objects.namesOf(parameterTypes))).isEquivalentTo(expectedMethod);
        assertThat(clazz.tryGetMethod("complex", Objects.namesOf(parameterTypes)).get()).isEquivalentTo(expectedMethod);
    }

    @Test
    public void imports_methods_with_correct_return_types() {
        Set<JavaCodeUnit> methods = getCodeUnits(new ClassFileImporter().importUrl(getClass().getResource("testexamples/methodimport")));

        assertThatType(findAnyByName(methods, "createString").getRawReturnType())
                .as("Return type of method 'createString'").matches(String.class);
        assertThatType(findAnyByName(methods, "consume").getRawReturnType())
                .as("Return type of method 'consume'").matches(void.class);
        assertThatType(findAnyByName(methods, "createSerializable").getRawReturnType())
                .as("Return type of method 'createSerializable'").matches(Serializable.class);
    }

    @Test
    public void imports_methods_with_correct_throws_declarations() {
        JavaMethod method = new ClassFileImporter().importUrl(getClass().getResource("testexamples/methodimport")).get(ClassWithThrowingMethod.class).getMethod("throwExceptions");

        assertThatThrowsClause(method.getThrowsClause())
                .as("Throws types of method 'throwsExceptions'")
                .matches(FirstCheckedException.class, SecondCheckedException.class);
        assertThatTypes(method.getExceptionTypes()).matchExactly(FirstCheckedException.class, SecondCheckedException.class);
    }

    @Test
    public void imports_members_with_sourceCodeLocation() {
        JavaClasses importedClasses = new ClassFileImporter().importUrl(getClass().getResource("testexamples/methodimport"));
        String sourceFileName = "ClassWithMultipleMethods.java";

        JavaClass javaClass = importedClasses.get(ClassWithMultipleMethods.class);
        assertThat(javaClass.getField("usage").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":0)");  // the byte code has no line number associated with a field
        assertThat(javaClass.getConstructor().getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":3)");  // auto-generated constructor seems to get line of class definition
        assertThat(javaClass.getStaticInitializer().get().getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":5)");  // auto-generated static initializer seems to get line of first static variable definition
        assertThat(javaClass.getMethod("methodDefinedInLine7").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":7)");
        assertThat(javaClass.getMethod("methodWithBodyStartingInLine10").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":10)");
        assertThat(javaClass.getMethod("emptyMethodDefinedInLine15").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":15)");
        assertThat(javaClass.getMethod("emptyMethodEndingInLine19").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":19)");

        javaClass = importedClasses.get(ClassWithMultipleMethods.InnerClass.class);
        assertThat(javaClass.getMethod("methodWithBodyStartingInLine24").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":24)");

        javaClass = importedClasses.get(ClassWithMultipleMethods.InnerClass.class.getName() + "$1");
        assertThat(javaClass.getMethod("run").getSourceCodeLocation())
                .hasToString("(" + sourceFileName + ":27)");
    }

    @Test
    public void imports_simple_constructors_with_correct_parameters() throws Exception {
        JavaClass clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/constructorimport")).get(ClassWithSimpleConstructors.class);

        assertThat(clazz.getConstructors()).as("Constructors").hasSize(3);

        Constructor<ClassWithSimpleConstructors> expectedConstructor = ClassWithSimpleConstructors.class.getDeclaredConstructor();
        assertThat(clazz.getConstructor()).isEquivalentTo(expectedConstructor);
        assertThat(clazz.tryGetConstructor().get()).isEquivalentTo(expectedConstructor);

        Class<?>[] parameterTypes = {Object.class};
        expectedConstructor = ClassWithSimpleConstructors.class.getDeclaredConstructor(parameterTypes);
        assertThat(clazz.getConstructor(parameterTypes)).isEquivalentTo(expectedConstructor);
        assertThat(clazz.getConstructor(Objects.namesOf(parameterTypes))).isEquivalentTo(expectedConstructor);
        assertThat(clazz.tryGetConstructor(parameterTypes).get()).isEquivalentTo(expectedConstructor);
        assertThat(clazz.tryGetConstructor(Objects.namesOf(parameterTypes)).get()).isEquivalentTo(expectedConstructor);

        parameterTypes = new Class[]{int.class, int.class};
        expectedConstructor = ClassWithSimpleConstructors.class.getDeclaredConstructor(parameterTypes);
        assertThat(clazz.getConstructor(parameterTypes)).isEquivalentTo(expectedConstructor);
        assertThat(clazz.getConstructor(Objects.namesOf(parameterTypes))).isEquivalentTo(expectedConstructor);
        assertThat(clazz.tryGetConstructor(parameterTypes).get()).isEquivalentTo(expectedConstructor);
        assertThat(clazz.tryGetConstructor(Objects.namesOf(parameterTypes)).get()).isEquivalentTo(expectedConstructor);
    }

    @Test
    public void imports_complex_constructor_with_correct_parameters() throws Exception {
        JavaClass clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/constructorimport")).get(ClassWithComplexConstructor.class);

        assertThat(clazz.getConstructors()).as("Constructors").hasSize(1);
        assertThat(clazz.getConstructor(String.class, long.class, long.class, Serializable.class, Serializable.class))
                .isEquivalentTo(ClassWithComplexConstructor.class.getDeclaredConstructor(
                        String.class, long.class, long.class, Serializable.class, Serializable.class));
    }

    @Test
    public void imports_constructor_with_correct_throws_declarations() {
        JavaClass clazz = new ClassFileImporter().importUrl(getClass().getResource("testexamples/constructorimport")).get(ClassWithThrowingConstructor.class);

        JavaConstructor constructor = getOnlyElement(clazz.getConstructors());
        assertThatThrowsClause(constructor.getThrowsClause()).as("Throws types of sole constructor")
                .matches(FirstCheckedException.class, SecondCheckedException.class);
        assertThatTypes(constructor.getExceptionTypes()).matchExactly(FirstCheckedException.class, SecondCheckedException.class);
    }

    @Test
    public void imports_overridden_methods_correctly() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/classhierarchyimport"));
        JavaClass baseClass = classes.get(BaseClass.class);
        JavaClass subClass = classes.get(Subclass.class);

        assertThat(baseClass.getCodeUnitWithParameterTypes("getSomeField").getModifiers()).containsOnly(PROTECTED);
        assertThat(subClass.getCodeUnitWithParameterTypes("getSomeField").getModifiers()).containsOnly(PUBLIC);
    }

    @Test
    public void imports_referenced_class_objects() {
        JavaClass javaClass = new ClassFileImporter().importClass(ReferencingClassObjects.class);

        Set<ReferencedClassObjectsAssertion.ExpectedReferencedClassObject> expectedInConstructor =
                ImmutableSet.of(referencedClassObject(File.class, 19), referencedClassObject(Path.class, 19));
        Set<ReferencedClassObjectsAssertion.ExpectedReferencedClassObject> expectedInMethod =
                ImmutableSet.of(referencedClassObject(FileSystem.class, 22), referencedClassObject(Charset.class, 22));
        Set<ReferencedClassObjectsAssertion.ExpectedReferencedClassObject> expectedInStaticInitializer =
                ImmutableSet.of(referencedClassObject(FilterInputStream.class, 16), referencedClassObject(Buffer.class, 16));

        assertThatReferencedClassObjects(javaClass.getConstructor().getReferencedClassObjects())
                .hasSize(2)
                .containReferencedClassObjects(expectedInConstructor);
        assertThatReferencedClassObjects(javaClass.getMethod("referencedClassObjectsInMethod").getReferencedClassObjects())
                .hasSize(2)
                .containReferencedClassObjects(expectedInMethod);
        assertThatReferencedClassObjects(javaClass.getStaticInitializer().get().getReferencedClassObjects())
                .hasSize(2)
                .containReferencedClassObjects(expectedInStaticInitializer);
        assertThatReferencedClassObjects(javaClass.getReferencedClassObjects())
                .hasSize(6)
                .containReferencedClassObjects(concat(expectedInConstructor, expectedInMethod, expectedInStaticInitializer));
    }

    @Test
    public void classes_know_which_fields_have_their_type() {
        JavaClasses classes = new ClassFileImporter().importClasses(SomeClass.class, OtherClass.class, SomeEnum.class);

        assertThat(classes.get(SomeEnum.class).getFieldsWithTypeOfSelf())
                .extracting("name").contains("other", "someEnum");
    }

    @Test
    public void classes_know_which_methods_have_their_type_as_parameter() {
        JavaClasses classes = new ClassFileImporter().importClasses(SomeClass.class, OtherClass.class, SomeEnum.class);

        assertThat(classes.get(SomeEnum.class).getMethodsWithParameterTypeOfSelf())
                .extracting("name").contains("methodWithSomeEnumParameter", "otherMethodWithSomeEnumParameter");
    }

    @Test
    public void classes_know_which_methods_have_their_type_as_return_type() {
        JavaClasses classes = new ClassFileImporter().importClasses(SomeClass.class, OtherClass.class, SomeEnum.class);

        assertThat(classes.get(SomeEnum.class).getMethodsWithReturnTypeOfSelf())
                .extracting("name").contains("methodWithSomeEnumReturnType", "otherMethodWithSomeEnumReturnType");
    }

    @Test
    public void classes_know_which_method_throws_clauses_contain_their_type() {
        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithThrowingMethod.class, FirstCheckedException.class);

        Set<ThrowsDeclaration<JavaMethod>> throwsDeclarations = classes.get(FirstCheckedException.class).getMethodThrowsDeclarationsWithTypeOfSelf();
        assertThatType(getOnlyElement(throwsDeclarations).getDeclaringClass()).matches(ClassWithThrowingMethod.class);
        assertThat(classes.get(FirstCheckedException.class).getConstructorsWithParameterTypeOfSelf()).isEmpty();
    }

    @Test
    public void classes_know_which_constructors_have_their_type_as_parameter() {
        JavaClasses classes = new ClassFileImporter().importClasses(SomeClass.class, OtherClass.class, SomeEnum.class);

        assertThat(classes.get(SomeEnum.class).getConstructorsWithParameterTypeOfSelf())
                .extracting("owner").extracting("name")
                .contains(SomeClass.class.getName(), OtherClass.class.getName());
    }

    @Test
    public void classes_know_which_constructor_throws_clauses_contain_their_type() {
        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithThrowingConstructor.class, FirstCheckedException.class);

        Set<ThrowsDeclaration<JavaConstructor>> throwsDeclarations =
                classes.get(FirstCheckedException.class).getConstructorsWithThrowsDeclarationTypeOfSelf();
        assertThatType(getOnlyElement(throwsDeclarations).getDeclaringClass()).matches(ClassWithThrowingConstructor.class);
        assertThat(classes.get(FirstCheckedException.class).getMethodThrowsDeclarationsWithTypeOfSelf()).isEmpty();
    }

    @Test
    public void classes_know_which_instanceof_checks_check_their_type() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(InstanceofChecked.class).get(InstanceofChecked.class);

        Set<JavaClass> origins = new HashSet<>();
        for (InstanceofCheck instanceofCheck : clazz.getInstanceofChecksWithTypeOfSelf()) {
            origins.add(instanceofCheck.getOwner().getOwner());
        }
        assertThatTypes(origins).matchInAnyOrder(ChecksInstanceofInMethod.class, ChecksInstanceofInConstructor.class, ChecksInstanceofInStaticInitializer.class);
    }
}
