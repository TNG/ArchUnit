package com.tngtech.archunit.core.importer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsExternalMethod;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsMethodReturningArray;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsOtherConstructor;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsOtherMethod;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsOwnConstructor;
import com.tngtech.archunit.core.importer.testexamples.callimport.CallsOwnMethod;
import com.tngtech.archunit.core.importer.testexamples.callimport.ExternalInterfaceMethodCall;
import com.tngtech.archunit.core.importer.testexamples.callimport.ExternalOverriddenMethodCall;
import com.tngtech.archunit.core.importer.testexamples.callimport.ExternalSubtypeConstructorCall;
import com.tngtech.archunit.core.importer.testexamples.complexexternal.ChildClass;
import com.tngtech.archunit.core.importer.testexamples.complexexternal.ParentClass;
import com.tngtech.archunit.core.importer.testexamples.dependents.ClassHoldingDependencies;
import com.tngtech.archunit.core.importer.testexamples.dependents.FirstClassWithDependency;
import com.tngtech.archunit.core.importer.testexamples.dependents.SecondClassWithDependency;
import com.tngtech.archunit.core.importer.testexamples.diamond.ClassCallingDiamond;
import com.tngtech.archunit.core.importer.testexamples.diamond.ClassImplementingD;
import com.tngtech.archunit.core.importer.testexamples.diamond.InterfaceB;
import com.tngtech.archunit.core.importer.testexamples.diamond.InterfaceC;
import com.tngtech.archunit.core.importer.testexamples.diamond.InterfaceD;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ExternalFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ExternalShadowedFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ForeignFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ForeignFieldAccessFromConstructor;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ForeignFieldAccessFromStaticInitializer;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.ForeignStaticFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.MultipleFieldAccessInSameMethod;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.OwnFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccessimport.OwnStaticFieldAccess;
import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.ClassAccessingInterfaceFields;
import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.InterfaceWithFields;
import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.OtherInterfaceWithFields;
import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.ParentInterfaceWithFields;
import com.tngtech.archunit.core.importer.testexamples.fieldimport.ClassWithIntAndObjectFields;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalfieldaccess.AccessToSuperAndSubclassField;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalfieldaccess.SubclassWithAccessedField;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalfieldaccess.SuperclassWithAccessedField;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall.CallOfSuperAndSubclassMethod;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall.SubclassWithCalledMethod;
import com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall.SuperclassWithCalledMethod;
import com.tngtech.archunit.core.importer.testexamples.integration.ClassA;
import com.tngtech.archunit.core.importer.testexamples.integration.ClassBDependingOnClassA;
import com.tngtech.archunit.core.importer.testexamples.integration.ClassCDependingOnClassB_SuperclassOfX;
import com.tngtech.archunit.core.importer.testexamples.integration.ClassD;
import com.tngtech.archunit.core.importer.testexamples.integration.ClassXDependingOnClassesABCD;
import com.tngtech.archunit.core.importer.testexamples.integration.InterfaceOfClassX;
import com.tngtech.archunit.core.importer.testexamples.specialtargets.ClassCallingSpecialTarget;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.domain.JavaModifier.BRIDGE;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.asClasses;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.findAnyByName;
import static com.tngtech.archunit.core.importer.ClassFileImporterTestUtils.getByName;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAccess;
import static com.tngtech.archunit.testutil.Assertions.assertThatCall;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.constructor;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;

public class ClassFileImporterAccessesTest {

    @Test
    public void imports_own_get_field_access() {
        JavaClass classWithOwnFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(OwnFieldAccess.class);

        JavaMethod getStringValue = classWithOwnFieldAccess.getMethod("getStringValue");

        JavaFieldAccess access = getOnlyElement(getStringValue.getFieldAccesses());
        assertThatAccess(access)
                .isOfType(GET)
                .isFrom(getStringValue)
                .isTo("stringValue")
                .inLineNumber(8);
    }

    @Test
    public void imports_own_set_field_access() {
        JavaClass classWithOwnFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(OwnFieldAccess.class);

        JavaMethod setStringValue = classWithOwnFieldAccess.getMethod("setStringValue", String.class);

        JavaFieldAccess access = getOnlyElement(setStringValue.getFieldAccesses());
        assertThatAccess(access)
                .isOfType(SET)
                .isFrom(setStringValue)
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(12);
    }

    @Test
    public void imports_multiple_own_accesses() {
        JavaClass classWithOwnFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(OwnFieldAccess.class);

        Set<JavaFieldAccess> fieldAccesses = classWithOwnFieldAccess.getFieldAccessesFromSelf();

        assertThat(fieldAccesses).hasSize(4);
        assertThat(getOnly(fieldAccesses, "stringValue", GET).getLineNumber())
                .as("Line number of get stringValue").isEqualTo(8);
        assertThat(getOnly(fieldAccesses, "stringValue", SET).getLineNumber())
                .as("Line number of set stringValue").isEqualTo(12);
        assertThat(getOnly(fieldAccesses, "intValue", GET).getLineNumber())
                .as("Line number of get intValue").isEqualTo(16);
        assertThat(getOnly(fieldAccesses, "intValue", SET).getLineNumber())
                .as("Line number of set intValue").isEqualTo(20);
    }

    @Test
    public void imports_own_static_field_accesses() {
        JavaClass classWithOwnFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(OwnStaticFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithOwnFieldAccess.getFieldAccessesFromSelf();

        assertThat(accesses).hasSize(2);

        JavaFieldAccess getAccess = getOnly(accesses, "staticStringValue", GET);

        assertThatAccess(getAccess)
                .isFrom("getStaticStringValue")
                .isTo("staticStringValue")
                .inLineNumber(7);

        JavaFieldAccess setAccess = getOnly(accesses, "staticStringValue", SET);

        assertThatAccess(setAccess)
                .isFrom("setStaticStringValue", String.class)
                .isTo("staticStringValue")
                .inLineNumber(11);
    }

    @Test
    public void imports_other_field_accesses() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport"));
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass classWithForeignFieldAccess = classes.get(ForeignFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithForeignFieldAccess.getFieldAccessesFromSelf();

        assertThat(accesses).hasSize(4);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("getStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "stringValue", SET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("setStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(9);

        assertThatAccess(getOnly(accesses, "intValue", GET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("getIntFromOther"))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(13);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("setIntFromOther"))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(17);
    }

    @Test
    public void imports_other_static_field_accesses() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport"));
        JavaClass classWithOwnFieldAccess = classes.get(OwnStaticFieldAccess.class);
        JavaClass classWithForeignFieldAccess = classes.get(ForeignStaticFieldAccess.class);

        Set<JavaFieldAccess> accesses = classWithForeignFieldAccess.getFieldAccessesFromSelf();

        assertThat(accesses).as("Number of field accesses from " + classWithForeignFieldAccess.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "staticStringValue", GET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("getStaticStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("staticStringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "staticStringValue", SET))
                .isFrom(classWithForeignFieldAccess.getCodeUnitWithParameterTypes("setStaticStringFromOther"))
                .isTo(classWithOwnFieldAccess.getField("staticStringValue"))
                .inLineNumber(9);
    }

    @Test
    public void imports_multiple_accesses_from_same_method() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport"));
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass multipleFieldAccesses = classes.get(MultipleFieldAccessInSameMethod.class);

        Set<JavaFieldAccess> accesses = multipleFieldAccesses.getFieldAccessesFromSelf();

        assertThat(accesses).as("Number of field accesses from " + multipleFieldAccesses.getName()).hasSize(5);

        Set<JavaFieldAccess> setStringValues = getByNameAndAccessType(accesses, "stringValue", SET);
        assertThat(setStringValues).hasSize(2);
        assertThat(targetsOf(setStringValues)).containsOnly(targetFrom(classWithOwnFieldAccess.getField("stringValue")));
        assertThat(lineNumbersOf(setStringValues)).containsOnly(6, 8);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(7);

        assertThatAccess(getOnly(accesses, "intValue", GET))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(10);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(11);
    }

    @Test
    public void imports_other_field_accesses_from_constructor() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport"));
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass fieldAccessFromConstructor = classes.get(ForeignFieldAccessFromConstructor.class);

        Set<JavaFieldAccess> accesses = fieldAccessFromConstructor.getFieldAccessesFromSelf();

        assertThat(accesses).as("Number of field accesses from " + fieldAccessFromConstructor.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFrom(fieldAccessFromConstructor.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFrom(fieldAccessFromConstructor.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(6);
    }

    @Test
    public void imports_other_field_accesses_from_static_initializer() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport"));
        JavaClass classWithOwnFieldAccess = classes.get(OwnFieldAccess.class);
        JavaClass fieldAccessFromInitializer = classes.get(ForeignFieldAccessFromStaticInitializer.class);

        Set<JavaFieldAccess> accesses = fieldAccessFromInitializer.getFieldAccessesFromSelf();

        assertThat(accesses).as("Number of field accesses from " + fieldAccessFromInitializer.getName()).hasSize(2);

        assertThatAccess(getOnly(accesses, "stringValue", GET))
                .isFrom(fieldAccessFromInitializer.getCodeUnitWithParameterTypes(STATIC_INITIALIZER_NAME))
                .isTo(classWithOwnFieldAccess.getField("stringValue"))
                .inLineNumber(5);

        assertThatAccess(getOnly(accesses, "intValue", SET))
                .isFrom(fieldAccessFromInitializer.getCodeUnitWithParameterTypes(STATIC_INITIALIZER_NAME))
                .isTo(classWithOwnFieldAccess.getField("intValue"))
                .inLineNumber(6);
    }

    @Test
    public void imports_external_field_access() {
        JavaClass classWithExternalFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(ExternalFieldAccess.class);

        JavaFieldAccess access = getOnlyElement(classWithExternalFieldAccess.getMethod("access").getFieldAccesses());

        assertThatAccess(access)
                .isFrom(classWithExternalFieldAccess.getCodeUnitWithParameterTypes("access"))
                .inLineNumber(8);

        assertThat(access.getTarget()).isEquivalentTo(field(ClassWithIntAndObjectFields.class, "objectField"));

        access = getOnlyElement(classWithExternalFieldAccess.getMethod("accessInheritedExternalField").getFieldAccesses());

        assertThatAccess(access)
                .isFrom(classWithExternalFieldAccess.getCodeUnitWithParameterTypes("accessInheritedExternalField"))
                .inLineNumber(12);

        assertThat(access.getTarget()).isEquivalentTo(field(ParentClass.class, "someParentField"));
    }

    @Test
    public void imports_external_field_access_with_shadowed_field() {
        JavaClass classWithExternalFieldAccess = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccessimport")).get(ExternalShadowedFieldAccess.class);

        JavaFieldAccess access = getOnlyElement(classWithExternalFieldAccess.getFieldAccessesFromSelf());

        assertThatAccess(access)
                .isFrom(classWithExternalFieldAccess.getCodeUnitWithParameterTypes("accessField"))
                .inLineNumber(7);

        assertThat(access.getTarget()).isEquivalentTo(field(ChildClass.class, "someField"));
    }

    @Test
    public void imports_shadowed_and_superclass_field_access() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/hierarchicalfieldaccess"));
        JavaClass classThatAccessesFieldOfSuperclass = classes.get(AccessToSuperAndSubclassField.class);
        JavaClass superclassWithAccessedField = classes.get(SuperclassWithAccessedField.class);
        JavaClass subClassWithAccessedField = classes.get(SubclassWithAccessedField.class);

        Set<JavaFieldAccess> accesses = classThatAccessesFieldOfSuperclass.getFieldAccessesFromSelf();

        assertThat(accesses).hasSize(2);
        JavaField field = superclassWithAccessedField.getField("field");
        AccessTarget.FieldAccessTarget expectedSuperclassFieldAccess = new DomainBuilders.FieldAccessTargetBuilder()
                .withOwner(subClassWithAccessedField)
                .withName(field.getName())
                .withType(field.getRawType())
                .withField(Suppliers.ofInstance(Optional.of(field)))
                .build();
        assertThatAccess(getOnly(accesses, "field", GET))
                .isFrom("accessSuperclassField")
                .isTo(expectedSuperclassFieldAccess)
                .inLineNumber(5);
        assertThatAccess(getOnly(accesses, "maskedField", GET))
                .isFrom("accessSubclassField")
                .isTo(subClassWithAccessedField.getField("maskedField"))
                .inLineNumber(9);
    }

    @Test
    public void imports_field_accesses_to_fields_from_interfaces() {
        Set<JavaFieldAccess> accesses = new ClassFileImporter().importUrl(getClass().getResource("testexamples/fieldaccesstointerfaces"))
                .get(ClassAccessingInterfaceFields.class).getFieldAccessesFromSelf();

        assertThat(findAnyByName(accesses, "" + InterfaceWithFields.objectFieldOne).getTarget().resolveField().get())
                .isEquivalentTo(field(InterfaceWithFields.class, "" + InterfaceWithFields.objectFieldOne));
        assertThat(findAnyByName(accesses, "" + InterfaceWithFields.objectFieldTwo).getTarget().resolveField().get())
                .isEquivalentTo(field(InterfaceWithFields.class, "" + InterfaceWithFields.objectFieldTwo));
        assertThat(findAnyByName(accesses, "" + OtherInterfaceWithFields.otherObjectFieldOne).getTarget().resolveField().get())
                .isEquivalentTo(field(OtherInterfaceWithFields.class, "" + OtherInterfaceWithFields.otherObjectFieldOne));
        assertThat(findAnyByName(accesses, "" + OtherInterfaceWithFields.otherObjectFieldTwo).getTarget().resolveField().get())
                .isEquivalentTo(field(OtherInterfaceWithFields.class, "" + OtherInterfaceWithFields.otherObjectFieldTwo));
        assertThat(findAnyByName(accesses, "" + ParentInterfaceWithFields.parentObjectFieldOne).getTarget().resolveField().get())
                .isEquivalentTo(field(ParentInterfaceWithFields.class, "" + ParentInterfaceWithFields.parentObjectFieldOne));
        assertThat(findAnyByName(accesses, "" + ParentInterfaceWithFields.parentObjectFieldTwo).getTarget().resolveField().get())
                .isEquivalentTo(field(ParentInterfaceWithFields.class, "" + ParentInterfaceWithFields.parentObjectFieldTwo));
    }

    @Test
    public void imports_shadowed_and_superclass_method_calls() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/hierarchicalmethodcall"));
        JavaClass classThatCallsMethodOfSuperclass = classes.get(CallOfSuperAndSubclassMethod.class);
        JavaClass superclassWithCalledMethod = classes.get(SuperclassWithCalledMethod.class);
        JavaClass subClassWithCalledMethod = classes.get(SubclassWithCalledMethod.class);

        Set<JavaMethodCall> calls = classThatCallsMethodOfSuperclass.getMethodCallsFromSelf();

        assertThat(calls).hasSize(2);

        JavaCodeUnit callSuperclassMethod = classThatCallsMethodOfSuperclass
                .getCodeUnitWithParameterTypes(CallOfSuperAndSubclassMethod.callSuperclassMethod);
        JavaMethod expectedSuperclassMethod = superclassWithCalledMethod.getMethod(SuperclassWithCalledMethod.method);
        AccessTarget.MethodCallTarget expectedSuperclassCall = new DomainBuilders.MethodCallTargetBuilder()
                .withOwner(subClassWithCalledMethod)
                .withName(expectedSuperclassMethod.getName())
                .withParameters(expectedSuperclassMethod.getRawParameterTypes())
                .withReturnType(expectedSuperclassMethod.getRawReturnType())
                .withMethods(Suppliers.ofInstance(Collections.singleton(expectedSuperclassMethod)))
                .build();
        assertThatCall(getOnlyByCaller(calls, callSuperclassMethod))
                .isFrom(callSuperclassMethod)
                .isTo(expectedSuperclassCall)
                .inLineNumber(CallOfSuperAndSubclassMethod.callSuperclassLineNumber);

        JavaCodeUnit callSubclassMethod = classThatCallsMethodOfSuperclass
                .getCodeUnitWithParameterTypes(CallOfSuperAndSubclassMethod.callSubclassMethod);
        assertThatCall(getOnlyByCaller(calls, callSubclassMethod))
                .isFrom(callSubclassMethod)
                .isTo(subClassWithCalledMethod.getMethod(SubclassWithCalledMethod.maskedMethod))
                .inLineNumber(CallOfSuperAndSubclassMethod.callSubclassLineNumber);
    }

    @Test
    public void imports_origin_and_target_of_call_from_bridge_method_correctly() {
        class Parent {
            @SuppressWarnings("unused")
            Object covariantlyOverriddenCausingBridgeMethod() {
                return null;
            }
        }
        class Child extends Parent {
            @Override
            String covariantlyOverriddenCausingBridgeMethod() {
                return null;
            }
        }

        JavaMethodCall callFromBridgeMethodToOverriddenOne = getOnlyElement(
                new ClassFileImporter().importClasses(Parent.class, Child.class)
                        .get(Child.class)
                        .getMethodCallsFromSelf());
        JavaCodeUnit bridgeMethod = callFromBridgeMethodToOverriddenOne.getOrigin();

        assertThat(bridgeMethod.getName()).isEqualTo("covariantlyOverriddenCausingBridgeMethod");
        assertThatType(bridgeMethod.getRawReturnType()).as("Return type of bridge method").matches(Object.class);
        assertThat(bridgeMethod.getModifiers()).as("modifiers of bridge method").contains(BRIDGE, SYNTHETIC);
    }

    @Test
    public void imports_constructor_calls_on_self() {
        JavaClass classThatCallsOwnConstructor = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(CallsOwnConstructor.class);
        JavaCodeUnit caller = classThatCallsOwnConstructor.getCodeUnitWithParameterTypes("copy");

        Set<JavaConstructorCall> calls = classThatCallsOwnConstructor.getConstructorCallsFromSelf();

        assertThatCall(getOnlyByCaller(calls, caller))
                .isFrom(caller)
                .isTo(classThatCallsOwnConstructor.getConstructor(String.class))
                .inLineNumber(8);
    }

    @Test
    public void imports_method_calls_on_self() {
        JavaClass classThatCallsOwnMethod = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(CallsOwnMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsOwnMethod.getMethodCallsFromSelf());

        assertThatCall(call)
                .isFrom(classThatCallsOwnMethod.getCodeUnitWithParameterTypes("getString"))
                .isTo(classThatCallsOwnMethod.getMethod("string"))
                .inLineNumber(6);
    }

    @Test
    public void imports_constructor_calls_on_other() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport"));
        JavaClass classThatCallsOtherConstructor = classes.get(CallsOtherConstructor.class);
        JavaClass otherClass = classes.get(CallsOwnConstructor.class);
        JavaCodeUnit caller = classThatCallsOtherConstructor.getCodeUnitWithParameterTypes("createOther");

        Set<JavaConstructorCall> calls = classThatCallsOtherConstructor.getConstructorCallsFromSelf();

        assertThatCall(getOnlyByCaller(calls, caller))
                .isFrom(caller)
                .isTo(otherClass.getConstructor(String.class))
                .inLineNumber(5);
    }

    @Test
    public void imports_method_calls_on_other() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport"));
        JavaClass classThatCallsOtherMethod = classes.get(CallsOtherMethod.class);
        JavaClass other = classes.get(CallsOwnMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsOtherMethod.getMethodCallsFromSelf());

        assertThatCall(call)
                .isFrom(classThatCallsOtherMethod.getCodeUnitWithParameterTypes("getFromOther"))
                .isTo(other.getMethod("getString"))
                .inLineNumber(7);
    }

    @Test
    public void imports_constructor_calls_on_external_class() throws Exception {
        JavaClass classThatCallsOwnConstructor = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(CallsOwnConstructor.class);
        JavaCodeUnit constructorCallingObjectInit = classThatCallsOwnConstructor.getConstructor(String.class);

        JavaConstructorCall objectInitCall = getOnlyElement(constructorCallingObjectInit.getConstructorCallsFromSelf());

        assertThatCall(objectInitCall)
                .isFrom(constructorCallingObjectInit)
                .inLineNumber(4);

        AccessTarget.ConstructorCallTarget target = objectInitCall.getTarget();
        assertThat(target.getFullName()).isEqualTo(Object.class.getName() + ".<init>()");
        assertThat(reflect(target)).isEqualTo(Object.class.getConstructor());
    }

    @Test
    public void imports_constructor_calls_to_sub_type_constructor_on_external_class() {
        JavaClass classWithExternalConstructorCall = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(ExternalSubtypeConstructorCall.class);

        assertConstructorCall(classWithExternalConstructorCall.getCodeUnitWithParameterTypes("call"), ChildClass.class, 9);
        assertConstructorCall(classWithExternalConstructorCall.getCodeUnitWithParameterTypes("newHashMap"), HashMap.class, 13);
    }

    private void assertConstructorCall(JavaCodeUnit call, Class<?> constructorOwner, int lineNumber) {
        JavaConstructorCall callToExternalClass =
                getOnlyElement(getByTargetOwner(call.getConstructorCallsFromSelf(), constructorOwner));

        assertThatCall(callToExternalClass)
                .isFrom(call)
                .inLineNumber(lineNumber);

        AccessTarget.ConstructorCallTarget target = callToExternalClass.getTarget();
        assertThat(target.getFullName()).isEqualTo(constructorOwner.getName() + ".<init>()");
        assertThat(reflect(target)).isEqualTo(constructor(constructorOwner));
    }

    @Test
    public void imports_method_calls_on_external_class() {
        JavaClass classThatCallsExternalMethod = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(CallsExternalMethod.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getMethodCallsFromSelf());

        assertThatCall(call)
                .isFrom(classThatCallsExternalMethod.getCodeUnitWithParameterTypes("getString"))
                .inLineNumber(7);

        AccessTarget.MethodCallTarget target = call.getTarget();
        assertThat(target.getOwner().reflect()).isEqualTo(ArrayList.class);
        assertThat(target.getFullName()).isEqualTo(ArrayList.class.getName() + ".toString()");
    }

    @Test
    public void imports_method_calls_on_overridden_external_class() {
        JavaClass classThatCallsExternalMethod = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(ExternalOverriddenMethodCall.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getMethodCallsFromSelf());

        assertThatCall(call)
                .isFrom(classThatCallsExternalMethod.getCodeUnitWithParameterTypes("call"))
                .inLineNumber(9);

        AccessTarget.MethodCallTarget target = call.getTarget();
        assertThat(target.getFullName()).isEqualTo(ChildClass.class.getName() + ".overrideMe()");
        assertThat(getOnlyElement(target.resolve()).getFullName()).isEqualTo(ChildClass.class.getName() + ".overrideMe()");
        assertThat(reflect(target)).isEqualTo(method(ChildClass.class, "overrideMe"));
    }

    @Test
    public void imports_method_calls_on_external_interface_hierarchies() {
        JavaClass classThatCallsExternalMethod = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(ExternalInterfaceMethodCall.class);

        JavaMethodCall call = getOnlyElement(classThatCallsExternalMethod.getMethodCallsFromSelf());

        assertThatCall(call)
                .isFrom(classThatCallsExternalMethod.getCodeUnitWithParameterTypes("call"))
                .inLineNumber(9);

        AccessTarget.MethodCallTarget target = call.getTarget();
        assertThat(reflect(target)).isEqualTo(method(Map.class, "put", Object.class, Object.class));
    }

    @Test
    public void imports_non_unique_targets_for_diamond_scenarios() {
        JavaClasses diamondScenario = new ClassFileImporter().importUrl(getClass().getResource("testexamples/diamond"));
        JavaClass classCallingDiamond = diamondScenario.get(ClassCallingDiamond.class);
        JavaClass diamondLeftInterface = diamondScenario.get(InterfaceB.class);
        JavaClass diamondRightInterface = diamondScenario.get(InterfaceC.class);
        JavaClass diamondPeakInterface = diamondScenario.get(InterfaceD.class);
        JavaClass diamondPeakClass = diamondScenario.get(ClassImplementingD.class);

        Set<JavaMethodCall> calls = classCallingDiamond.getMethodCallsFromSelf();

        assertThat(calls).hasSize(2);

        JavaCodeUnit callInterface = classCallingDiamond
                .getCodeUnitWithParameterTypes(ClassCallingDiamond.callInterface);
        JavaMethodCall callToInterface = getOnlyByCaller(calls, callInterface);
        assertThatCall(callToInterface)
                .isFrom(callInterface)
                .inLineNumber(ClassCallingDiamond.callInterfaceLineNumber);
        // NOTE: There is no java.lang.reflect.Method InterfaceD.implementMe(), because the method is inherited
        assertThat(callToInterface.getTarget().getName()).isEqualTo(InterfaceD.implementMe);
        assertThat(callToInterface.getTarget().getOwner()).isEqualTo(diamondPeakInterface);
        assertThat(callToInterface.getTarget().getRawParameterTypes()).isEmpty();
        assertThat(callToInterface.getTarget().resolve()).extracting("fullName")
                .containsOnly(
                        diamondLeftInterface.getMethod(InterfaceB.implementMe).getFullName(),
                        diamondRightInterface.getMethod(InterfaceB.implementMe).getFullName());

        JavaCodeUnit callImplementation = classCallingDiamond
                .getCodeUnitWithParameterTypes(ClassCallingDiamond.callImplementation);
        assertThatCall(getOnlyByCaller(calls, callImplementation))
                .isFrom(callImplementation)
                .isTo(diamondPeakClass.getMethod(InterfaceD.implementMe))
                .inLineNumber(ClassCallingDiamond.callImplementationLineNumber);
    }

    @Test
    public void imports_method_calls_that_return_Arrays() {
        JavaClass classThatCallsMethodReturningArray = new ClassFileImporter().importUrl(getClass().getResource("testexamples/callimport")).get(CallsMethodReturningArray.class);

        AccessTarget.MethodCallTarget target = getOnlyElement(classThatCallsMethodReturningArray.getMethodCallsFromSelf()).getTarget();
        assertThatType(target.getOwner()).matches(CallsMethodReturningArray.SomeEnum.class);
        assertThatType(target.getRawReturnType()).matches(CallsMethodReturningArray.SomeEnum[].class);
    }

    @Test
    public void dependency_target_classes_are_derived_correctly() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/integration"));
        JavaClass javaClass = classes.get(ClassXDependingOnClassesABCD.class);
        Set<JavaClass> expectedTargetClasses = ImmutableSet.of(
                classes.get(ClassA.class),
                classes.get(ClassBDependingOnClassA.class),
                classes.get(ClassCDependingOnClassB_SuperclassOfX.class),
                classes.get(ClassD.class),
                classes.get(InterfaceOfClassX.class)
        );

        Set<JavaClass> targetClasses = new HashSet<>();
        for (Dependency dependency : withoutJavaLangTargets(javaClass.getDirectDependenciesFromSelf())) {
            targetClasses.add(dependency.getTargetClass());
        }

        assertThat(targetClasses).isEqualTo(expectedTargetClasses);
    }

    @Test
    public void getDirectDependencies_does_not_return_transitive_dependencies() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/integration"));
        JavaClass javaClass = classes.get(ClassCDependingOnClassB_SuperclassOfX.class);
        JavaClass expectedTargetClass = classes.get(ClassBDependingOnClassA.class);

        Set<JavaClass> targetClasses = new HashSet<>();
        for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
            if (dependency.getTargetClass().getPackageName().contains("testexamples")) {
                targetClasses.add(dependency.getTargetClass());
            }
        }

        assertThat(targetClasses).containsOnly(expectedTargetClass);
    }

    @Test
    public void fields_know_their_accesses() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaFieldAccess> accesses = classHoldingDependencies.getField("someInt").getAccessesToSelf();
        Set<JavaFieldAccess> expected = ImmutableSet.<JavaFieldAccess>builder()
                .addAll(getByName(classHoldingDependencies.getFieldAccessesFromSelf(), "someInt"))
                .addAll(getByName(firstClassWithDependency.getFieldAccessesFromSelf(), "someInt"))
                .addAll(getByName(secondClassWithDependency.getFieldAccessesFromSelf(), "someInt"))
                .build();
        assertThat(accesses).as("Field Accesses to someInt").isEqualTo(expected);
    }

    @Test
    public void classes_know_the_field_accesses_to_them() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaFieldAccess> accesses = classHoldingDependencies.getFieldAccessesToSelf();
        Set<JavaFieldAccess> expected = ImmutableSet.<JavaFieldAccess>builder()
                .addAll(classHoldingDependencies.getFieldAccessesFromSelf())
                .addAll(firstClassWithDependency.getFieldAccessesFromSelf())
                .addAll(secondClassWithDependency.getFieldAccessesFromSelf())
                .build();
        assertThat(accesses).as("Field Accesses to class").isEqualTo(expected);
    }

    @Test
    public void classes_know_shadowed_field_accesses_to_themselves() {
        @SuppressWarnings("unused")
        class Base {
            String shadowed;
            String nonShadowed;
        }
        class Child extends Base {
            String shadowed;
        }
        @SuppressWarnings("unused")
        class Accessor {
            void access(Child child) {
                consume(child.shadowed);
                consume(child.nonShadowed);
            }

            void consume(String string) {
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Accessor.class, Base.class, Child.class);

        JavaFieldAccess access = getOnlyByCaller(
                classes.get(Base.class).getFieldAccessesToSelf(), classes.get(Accessor.class).getMethod("access", Child.class));
        assertThatAccess(access).isFrom("access", Child.class).isTo("nonShadowed");
        access = getOnlyByCaller(
                classes.get(Child.class).getFieldAccessesToSelf(), classes.get(Accessor.class).getMethod("access", Child.class));
        assertThatAccess(access).isFrom("access", Child.class).isTo("shadowed");
    }

    @Test
    public void methods_know_callers() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaMethodCall> calls = classHoldingDependencies.getMethod("setSomeInt", int.class).getCallsOfSelf();
        Set<JavaMethodCall> expected = ImmutableSet.<JavaMethodCall>builder()
                .addAll(getByName(classHoldingDependencies.getMethodCallsFromSelf(), "setSomeInt"))
                .addAll(getByName(firstClassWithDependency.getMethodCallsFromSelf(), "setSomeInt"))
                .addAll(getByName(secondClassWithDependency.getMethodCallsFromSelf(), "setSomeInt"))
                .build();
        assertThat(calls).as("Method calls to setSomeInt").isEqualTo(expected);
    }

    @Test
    public void classes_know_method_calls_to_themselves() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaMethodCall> calls = classHoldingDependencies.getMethodCallsToSelf();
        Set<JavaMethodCall> expected = ImmutableSet.<JavaMethodCall>builder()
                .addAll(classHoldingDependencies.getMethodCallsFromSelf())
                .addAll(getByTargetOwner(firstClassWithDependency.getMethodCallsFromSelf(), classHoldingDependencies))
                .addAll(getByTargetOwner(secondClassWithDependency.getMethodCallsFromSelf(), classHoldingDependencies))
                .build();
        assertThat(calls).as("Method calls to class").isEqualTo(expected);
    }

    @Test
    public void classes_know_overridden_method_calls_to_themselves() {
        @SuppressWarnings("unused")
        class Base {
            void overridden() {
            }

            void nonOverridden() {
            }
        }
        class Child extends Base {
            @Override
            void overridden() {
            }
        }
        @SuppressWarnings("unused")
        class Caller {
            void call(Child child) {
                child.overridden();
                child.nonOverridden();
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Caller.class, Base.class, Child.class);

        assertThatCall(getOnlyElement(classes.get(Base.class).getMethodCallsToSelf())).isFrom("call", Child.class).isTo("nonOverridden");
        assertThatCall(getOnlyElement(classes.get(Child.class).getMethodCallsToSelf())).isFrom("call", Child.class).isTo("overridden");
    }

    @Test
    public void constructors_know_callers() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        JavaConstructor targetConstructur = classHoldingDependencies.getConstructor();
        Set<JavaConstructorCall> calls = targetConstructur.getCallsOfSelf();
        Set<JavaConstructorCall> expected = ImmutableSet.<JavaConstructorCall>builder()
                .addAll(getByTarget(classHoldingDependencies.getConstructorCallsFromSelf(), targetConstructur))
                .addAll(getByTarget(firstClassWithDependency.getConstructorCallsFromSelf(), targetConstructur))
                .addAll(getByTarget(secondClassWithDependency.getConstructorCallsFromSelf(), targetConstructur))
                .build();
        assertThat(calls).as("Default Constructor calls to ClassWithDependents").isEqualTo(expected);
    }

    @Test
    public void classes_know_constructor_calls_to_themselves() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaConstructorCall> calls = classHoldingDependencies.getConstructorCallsToSelf();
        Set<JavaConstructorCall> expected = ImmutableSet.<JavaConstructorCall>builder()
                .addAll(getByTargetOwner(classHoldingDependencies.getConstructorCallsFromSelf(), classHoldingDependencies))
                .addAll(getByTargetOwner(firstClassWithDependency.getConstructorCallsFromSelf(), classHoldingDependencies))
                .addAll(getByTargetOwner(secondClassWithDependency.getConstructorCallsFromSelf(), classHoldingDependencies))
                .build();
        assertThat(calls).as("Constructor calls to ClassWithDependents").isEqualTo(expected);
    }

    @Test
    public void classes_know_constructor_calls_to_themselves_for_subclass_default_constructors() {
        // For constructors it's impossible to be accessed via a subclass,
        // since the byte code always holds an explicitly declared constructor.
        // Thus we do expect a call to the constructor of the subclass and one from subclass to super class
        @SuppressWarnings("unused")
        class Base {
            Base() {
            }
        }
        class Child extends Base {
        }
        @SuppressWarnings("unused")
        class Caller {
            void call() {
                new Child();
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Caller.class, Base.class, Child.class);

        assertThatCall(getOnlyElement(classes.get(Base.class).getConstructorCallsToSelf())).isFrom(Child.class, CONSTRUCTOR_NAME, getClass()).isTo(Base.class);
        assertThatCall(getOnlyElement(classes.get(Child.class).getConstructorCallsToSelf())).isFrom(Caller.class, "call").isTo(Child.class);
    }

    @Test
    public void classes_know_accesses_to_themselves() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/dependents"));
        JavaClass classHoldingDependencies = classes.get(ClassHoldingDependencies.class);
        JavaClass firstClassWithDependency = classes.get(FirstClassWithDependency.class);
        JavaClass secondClassWithDependency = classes.get(SecondClassWithDependency.class);

        Set<JavaAccess<?>> accesses = classHoldingDependencies.getAccessesToSelf();
        Set<JavaAccess<?>> expected = ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getByTargetOwner(classHoldingDependencies.getAccessesFromSelf(), classHoldingDependencies))
                .addAll(getByTargetOwner(firstClassWithDependency.getAccessesFromSelf(), classHoldingDependencies))
                .addAll(getByTargetOwner(secondClassWithDependency.getAccessesFromSelf(), classHoldingDependencies))
                .build();
        assertThat(accesses).as("Accesses to ClassWithDependents").isEqualTo(expected);
    }

    // NOTE: This provokes the scenario where the target type can't be determined uniquely due to a diamond
    //       scenario and thus a fallback to (primitive and array) type names by ASM descriptors occurs.
    //       Unfortunately those ASM type names for example are the canonical name instead of the class name.
    @Test
    public void imports_special_target_parameters() {
        JavaClasses classes = new ClassFileImporter().importUrl(getClass().getResource("testexamples/specialtargets"));
        Set<JavaMethodCall> calls = classes.get(ClassCallingSpecialTarget.class).getMethodCallsFromSelf();

        assertThatTypes(targetParametersOf(calls, "primitiveArgs")).matchExactly(byte.class, long.class);
        assertThatType(returnTypeOf(calls, "primitiveReturnType")).matches(byte.class);
        assertThatTypes(targetParametersOf(calls, "arrayArgs")).matchExactly(byte[].class, Object[].class);
        assertThatType(returnTypeOf(calls, "primitiveArrayReturnType")).matches(short[].class);
        assertThatType(returnTypeOf(calls, "objectArrayReturnType")).matches(String[].class);
        assertThatTypes(targetParametersOf(calls, "twoDimArrayArgs")).matchExactly(float[][].class, Object[][].class);
        assertThatType(returnTypeOf(calls, "primitiveTwoDimArrayReturnType")).matches(double[][].class);
        assertThatType(returnTypeOf(calls, "objectTwoDimArrayReturnType")).matches(String[][].class);
    }

    @Test
    public void imports_parameter_types_of_generic_call_target_as_raw_types() {
        // We cannot determine generic types of a call target from the bytecode, so the only options are to resolve the target
        // and check for generic types or to treat call target parameters always as raw, same as they are in the bytecode.
        // For now I have decided for the the latter.

        class Target {
            @SuppressWarnings({"unused", "SameParameterValue"})
            <T> void genericMethod(T param) {
            }
        }
        class Origin {
            @SuppressWarnings("unused")
            void call(Target target) {
                target.genericMethod(null);
            }
        }

        JavaMethodCall call = getOnlyElement(new ClassFileImporter()
                .importClasses(Origin.class, Target.class)
                .get(Origin.class)
                .getMethodCallsFromSelf());

        assertThat(call.getTarget().getParameterTypes())
                .isEqualTo(call.getTarget().getRawParameterTypes());
    }

    private Set<Dependency> withoutJavaLangTargets(Set<Dependency> dependencies) {
        Set<Dependency> result = new HashSet<>();
        for (Dependency dependency : dependencies) {
            if (!dependency.getTargetClass().getPackageName().startsWith("java.lang")) {
                result.add(dependency);
            }
        }
        return result;
    }

    private Constructor<?> reflect(AccessTarget.ConstructorCallTarget target) {
        return reflect(target.resolveConstructor().get());
    }

    private Constructor<?> reflect(JavaConstructor javaConstructor) {
        try {
            return javaConstructor.getOwner().reflect().getConstructor(asClasses(javaConstructor.getRawParameterTypes()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Method reflect(AccessTarget.MethodCallTarget target) {
        return reflect(getOnlyElement(target.resolve()));
    }

    private Method reflect(JavaMethod javaMethod) {
        try {
            return javaMethod.getOwner().reflect().getMethod(javaMethod.getName(), asClasses(javaMethod.getRawParameterTypes()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private List<JavaClass> targetParametersOf(Set<JavaMethodCall> calls, String name) {
        return findAnyByName(calls, name).getTarget().getRawParameterTypes();
    }

    private JavaClass returnTypeOf(Set<JavaMethodCall> calls, String name) {
        return findAnyByName(calls, name).getTarget().getRawReturnType();
    }

    private JavaFieldAccess getOnly(Set<JavaFieldAccess> fieldAccesses, String name, JavaFieldAccess.AccessType accessType) {
        return getOnlyElement(getByNameAndAccessType(fieldAccesses, name, accessType));
    }

    private Set<JavaFieldAccess> getByNameAndAccessType(Set<JavaFieldAccess> fieldAccesses, String name, JavaFieldAccess.AccessType accessType) {
        Set<JavaFieldAccess> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            if (name.equals(access.getName()) && access.getAccessType() == accessType) {
                result.add(access);
            }
        }
        return result;
    }

    private <T extends HasOwner<JavaCodeUnit>> T getOnlyByCaller(Set<T> calls, JavaCodeUnit caller) {
        return getOnlyElement(getByCaller(calls, caller));
    }

    private <T extends JavaAccess<?>> Set<T> getByTarget(Set<T> calls, final JavaConstructor target) {
        return getBy(calls, new Predicate<JavaAccess<?>>() {
            @Override
            public boolean apply(JavaAccess<?> input) {
                return targetFrom(target).getFullName().equals(input.getTarget().getFullName());
            }
        });
    }

    private <T extends JavaAccess<?>> Set<T> getByTargetOwner(Set<T> calls, Class<?> targetOwner) {
        return getByTargetOwner(calls, targetOwner.getName());
    }

    private <T extends JavaAccess<?>> Set<T> getByTargetOwner(Set<T> calls, final String targetOwnerName) {
        return getBy(calls, targetOwnerNameEquals(targetOwnerName));
    }

    private Predicate<JavaAccess<?>> targetOwnerNameEquals(final String targetFqn) {
        return new Predicate<JavaAccess<?>>() {
            @Override
            public boolean apply(JavaAccess<?> input) {
                return targetFqn.equals(input.getTarget().getOwner().getName());
            }
        };
    }

    private <T extends JavaAccess<?>> Set<T> getByTargetOwner(Set<T> calls, final JavaClass targetOwner) {
        return getBy(calls, new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return targetOwner.equals(input.getTarget().getOwner());
            }
        });
    }

    private <T extends HasOwner<JavaCodeUnit>> Set<T> getByCaller(Set<T> calls, final JavaCodeUnit caller) {
        return getBy(calls, new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return caller.equals(input.getOwner());
            }
        });
    }

    private <T extends HasOwner<JavaCodeUnit>> Set<T> getBy(Set<T> calls, Predicate<? super T> predicate) {
        return FluentIterable.from(calls).filter(predicate).toSet();
    }

    private Set<AccessTarget.FieldAccessTarget> targetsOf(Set<JavaFieldAccess> fieldAccesses) {
        Set<AccessTarget.FieldAccessTarget> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            result.add(access.getTarget());
        }
        return result;
    }

    private Set<Integer> lineNumbersOf(Set<JavaFieldAccess> fieldAccesses) {
        Set<Integer> result = new HashSet<>();
        for (JavaFieldAccess access : fieldAccesses) {
            result.add(access.getLineNumber());
        }
        return result;
    }
}
