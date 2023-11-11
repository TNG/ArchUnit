package com.tngtech.archunit.core.domain;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.testobjects.AAccessingB;
import com.tngtech.archunit.core.domain.testobjects.AExtendingSuperAImplementingInterfaceForA;
import com.tngtech.archunit.core.domain.testobjects.AReferencingB;
import com.tngtech.archunit.core.domain.testobjects.AhavingMembersOfTypeB;
import com.tngtech.archunit.core.domain.testobjects.AllPrimitiveDependencies;
import com.tngtech.archunit.core.domain.testobjects.ArrayComponentTypeDependencies;
import com.tngtech.archunit.core.domain.testobjects.B;
import com.tngtech.archunit.core.domain.testobjects.BReferencedByA;
import com.tngtech.archunit.core.domain.testobjects.ComponentTypeDependency;
import com.tngtech.archunit.core.domain.testobjects.DependenciesOnClassObjects;
import com.tngtech.archunit.core.domain.testobjects.InterfaceForA;
import com.tngtech.archunit.core.domain.testobjects.SuperA;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassAccessingOneDimensionalArray;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassAccessingTwoDimensionalArray;
import com.tngtech.archunit.core.importer.testexamples.arrays.ClassUsedInArray;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CODE_UNITS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTORS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELDS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_MEMBERS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_METHODS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_STATIC_INITIALIZER;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyCodeUnitsThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyConstructorsThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyFieldsThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyMembersThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyMethodsThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.containAnyStaticInitializersThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.implement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importPackagesOf;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.properties.HasName.AndFullName.Predicates.fullNameMatching;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatCodeUnit;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependencies;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.getHierarchy;
import static com.tngtech.archunit.testutil.assertion.DependenciesAssertion.from;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
@SuppressWarnings("SameParameterValue")
public class JavaClassTest {

    @Test
    public void finds_array_type() {
        @SuppressWarnings("unused")
        class IsArrayTestClass {
            Object[] anArray() {
                return null;
            }
        }

        JavaMethod method = importClassWithContext(IsArrayTestClass.class).getMethod("anArray");
        JavaClass arrayType = method.getRawReturnType();

        assertThat(arrayType.isArray()).isTrue();
        assertThatType(arrayType.tryGetComponentType().get())
                .isSameAs(arrayType.getComponentType())
                .isSameAs(arrayType.getBaseComponentType())
                .matches(Object.class);
    }

    @Test
    public void finds_non_array_type() {
        @SuppressWarnings("unused")
        class IsArrayTestClass {
            Object notAnArray() {
                return null;
            }
        }

        JavaMethod method = importClassWithContext(IsArrayTestClass.class).getMethod("notAnArray");
        JavaClass nonArrayType = method.getRawReturnType();

        assertThat(nonArrayType.isArray()).isFalse();
        assertThat(nonArrayType.tryGetComponentType()).isEmpty();
        assertThatThrownBy(nonArrayType::getComponentType).isInstanceOf(IllegalStateException.class);
        assertThat(nonArrayType.getBaseComponentType()).isSameAs(nonArrayType);
    }

    @Test
    public void finds_multidimensional_array_type() {
        JavaClasses classes = importClassesWithContext(ClassUsedInArray.class, ClassAccessingOneDimensionalArray.class, ClassAccessingTwoDimensionalArray.class);
        JavaClass type = classes.get(ClassUsedInArray.class);
        JavaClass oneDimArray = classes.get(ClassAccessingOneDimensionalArray.class).getField("array").getRawType();
        JavaClass twoDimArray = classes.get(ClassAccessingTwoDimensionalArray.class).getField("array").getRawType();

        assertThat(oneDimArray.isArray()).isTrue();
        assertThatType(oneDimArray.tryGetComponentType().get()).isEqualTo(type);
        assertThat(twoDimArray.isArray()).isTrue();
        assertThatType(twoDimArray.tryGetComponentType().get()).isEqualTo(oneDimArray);
        assertThatType(twoDimArray.tryGetComponentType().get().tryGetComponentType().get()).isEqualTo(type);
    }

    @Test
    public void erased_type_of_class_is_the_class_itself() {
        class SimpleClass {
        }

        JavaType type = new ClassFileImporter().importClass(SimpleClass.class);

        assertThat(type.toErasure()).isEqualTo(type);
    }

    @Test
    public void get_all_involved_raw_types_returns_only_self_for_non_array_type() {
        class SimpleClass {
        }

        JavaClass clazz = new ClassFileImporter().importClass(SimpleClass.class);

        assertThatTypes(clazz.getAllInvolvedRawTypes()).matchExactly(SimpleClass.class);
    }

    @Test
    public void get_all_involved_raw_types_returns_component_type_for_array_type() {
        class SimpleClass {
            @SuppressWarnings("unused")
            SimpleClass[][] field;
        }

        JavaType arrayType = new ClassFileImporter().importClass(SimpleClass.class).getField("field").getType();

        assertThatTypes(arrayType.getAllInvolvedRawTypes()).matchExactly(SimpleClass.class);
    }

    @Test
    public void finds_component_type_chain_of_otherwise_unreferenced_component_type() {
        @SuppressWarnings("unused")
        class OnlyReferencingMultiDimArray {
            OnlyReferencingMultiDimArray[][][] field;
        }

        JavaClass javaClass = importClassesWithContext(OnlyReferencingMultiDimArray.class)
                .get(OnlyReferencingMultiDimArray.class);

        JavaClass arrayType = javaClass.getField("field").getRawType();
        JavaClass twoDim = arrayType.getComponentType();
        assertThat(twoDim.getName()).isEqualTo(OnlyReferencingMultiDimArray[][].class.getName());

        JavaClass oneDim = twoDim.getComponentType();
        assertThat(oneDim.getName()).isEqualTo(OnlyReferencingMultiDimArray[].class.getName());

        JavaClass original = oneDim.getComponentType();
        assertThatType(original).isEqualTo(javaClass);

        assertThat(arrayType.getBaseComponentType())
                .isSameAs(twoDim.getBaseComponentType())
                .isSameAs(oneDim.getBaseComponentType())
                .isSameAs(original.getBaseComponentType())
                .isSameAs(original);
    }

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = importClassWithContext(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(javaClass.reflect()).isEqualTo(ClassWithTwoFieldsAndTwoMethods.class);
        assertThat(javaClass.getFields()).hasSize(2);
        Set<JavaField> allFields = excludeJavaLangObject(javaClass.getAllFields());
        assertThat(allFields).hasSize(3);
        assertThat(javaClass.getMethods()).hasSize(2);
        Set<JavaMethod> allMethods = excludeJavaLangObject(javaClass.getAllMethods());
        assertThat(allMethods).hasSize(4);

        for (JavaField field : javaClass.getFields()) {
            assertThatType(field.getOwner()).isSameAs(javaClass);
        }
        assertThatTypes(allFields.stream().map(JavaMember::getOwner).collect(toSet()))
                .matchInAnyOrder(ClassWithTwoFieldsAndTwoMethods.class, SuperclassWithFieldAndMethod.class);
        for (JavaCodeUnit method : javaClass.getCodeUnits()) {
            assertThatType(method.getOwner()).isSameAs(javaClass);
        }
        assertThatTypes(allMethods.stream().map(JavaMember::getOwner).collect(toSet()))
                .matchInAnyOrder(ClassWithTwoFieldsAndTwoMethods.class, SuperclassWithFieldAndMethod.class, InterfaceWithMethod.class);
    }

    @Test
    public void finds_constructors() {
        JavaClass javaClass = importClassWithContext(ClassWithSeveralConstructorsFieldsAndMethods.class);

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));

        Set<JavaConstructor> allConstructors = excludeJavaLangObject(javaClass.getAllConstructors());
        assertThat(allConstructors).as("all constructors").hasSize(5);
        assertThatTypes(allConstructors.stream().map(JavaConstructor::getOwner).collect(toSet()))
                .matchInAnyOrder(ClassWithSeveralConstructorsFieldsAndMethods.class, ParentOfClassWithSeveralConstructorsFieldsAndMethods.class);
    }

    @Test
    public void reports_non_existing_members_as_absent() {
        JavaClass javaClass = importClassWithContext(ParentWithFieldAndMethod.class);

        assertThat(javaClass.tryGetField("notthere")).isEmpty();
        assertThat(javaClass.tryGetMethod("notthere")).isEmpty();
        assertThat(javaClass.tryGetMethod("notthere", Object.class)).isEmpty();
        assertThat(javaClass.tryGetMethod("notthere", Object.class.getName())).isEmpty();
        assertThat(javaClass.tryGetConstructor()).isEmpty();
        assertThat(javaClass.tryGetConstructor(String.class)).isEmpty();
        assertThat(javaClass.tryGetConstructor(String.class.getName())).isEmpty();
    }

    @Test
    public void anonymous_class_has_package_of_declaring_class() {
        Serializable input = new Serializable() {
        };

        JavaClass anonymous = importClassWithContext(input.getClass());

        assertThat(anonymous.getPackageName()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void inner_class_has_package_of_declaring_class() {
        JavaClass anonymous = importClassWithContext(ClassWithInnerClass.Inner.class);

        assertThat(anonymous.getPackageName()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void superclasses_are_found() {
        JavaClass clazz = importClassesWithContext(ClassWithTwoFieldsAndTwoMethods.class, SuperclassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllRawSuperclasses()).extracting("name").containsExactly(
                SuperclassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
        assertThat(clazz.getAllRawSuperclasses()).extracting("name").containsExactly(
                SuperclassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = importClassesWithContext(ClassWithTwoFieldsAndTwoMethods.class, SuperclassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).extracting("name").containsExactly(
                clazz.getName(),
                SuperclassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void all_classes_self_is_assignable_to() {
        JavaClass clazz = importClasses(ChildWithFieldAndMethod.class, ParentWithFieldAndMethod.class, InterfaceWithFieldAndMethod.class)
                .get(ChildWithFieldAndMethod.class);

        assertThat(clazz.getAllClassesSelfIsAssignableTo())
                .extracting("name")
                .containsOnly(
                        ChildWithFieldAndMethod.class.getName(),
                        ParentWithFieldAndMethod.class.getName(),
                        InterfaceWithFieldAndMethod.class.getName(),
                        Object.class.getName());
    }

    @Test
    public void isAnnotatedWith_type() {
        assertThat(importClassWithContext(Parent.class).isAnnotatedWith(SomeAnnotation.class))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(importClassWithContext(Parent.class).isAnnotatedWith(Retention.class))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void isAnnotatedWith_typeName() {
        assertThat(importClassWithContext(Parent.class).isAnnotatedWith(SomeAnnotation.class.getName()))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(importClassWithContext(Parent.class).isAnnotatedWith(Retention.class.getName()))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void isAnnotatedWith_predicate() {
        assertThat(importClassWithContext(Parent.class)
                .isAnnotatedWith(DescribedPredicate.alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(importClassWithContext(Parent.class)
                .isAnnotatedWith(DescribedPredicate.alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_type() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz.isMetaAnnotatedWith(SomeAnnotation.class))
                .as("Parent is meta-annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(clazz.isMetaAnnotatedWith(Retention.class))
                .as("Parent is meta-annotated with @" + Retention.class.getSimpleName()).isTrue();
        assertThat(clazz.isMetaAnnotatedWith(Deprecated.class))
                .as("Parent is meta-annotated with @" + Deprecated.class.getSimpleName()).isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_typeName() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz.isMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .as("Parent is meta-annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(clazz.isMetaAnnotatedWith(Retention.class.getName()))
                .as("Parent is meta-annotated with @" + Retention.class.getSimpleName()).isTrue();
        assertThat(clazz.isMetaAnnotatedWith(Deprecated.class.getName()))
                .as("Parent is meta-annotated with @" + Deprecated.class.getSimpleName()).isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_predicate() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz
                .isMetaAnnotatedWith(DescribedPredicate.alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(clazz
                .isMetaAnnotatedWith(DescribedPredicate.alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_correctly_resolves_cyclic_annotations() {
        JavaClass javaClass = importClasses(ClassWithCyclicMetaAnnotation.class,
                AnnotationWithCyclicAnnotation.class, MetaAnnotationWithCyclicAnnotation.class,
                Retention.class).get(ClassWithCyclicMetaAnnotation.class);

        assertThat(javaClass.isMetaAnnotatedWith(Deprecated.class)).isFalse();
        assertThat(javaClass.isMetaAnnotatedWith(Retention.class)).isTrue();
        assertThat(javaClass.isMetaAnnotatedWith(MetaAnnotationWithCyclicAnnotation.class)).isTrue();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = importClasses(ClassWithTwoFieldsAndTwoMethods.class, SuperclassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = importClassWithContext(Object.class);
        simulateCall().from(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCall().from(javaClass.getRawSuperclass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginOwner").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginOwner")
                .contains(javaClass, javaClass.getRawSuperclass().get());
    }

    @Test
    public void JavaClass_is_equivalent_to_reflect_type() {
        JavaClass list = importClassWithContext(List.class);

        assertThat(list.isEquivalentTo(List.class)).as("JavaClass is List.class").isTrue();
        assertThat(list.isEquivalentTo(Collection.class)).as("JavaClass is Collection.class").isFalse();
    }

    @Test
    public void getMembers_and_getAllMembers() {
        JavaClass clazz = importClasses(
                ChildWithFieldAndMethod.class,
                ParentWithFieldAndMethod.class,
                InterfaceWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertThat(clazz.getMembers())
                .extracting(memberIdentifier())
                .hasSameElementsAs(ChildWithFieldAndMethod.Members.MEMBERS);

        assertThat(clazz.getAllMembers())
                .filteredOn(isNotObject())
                .extracting(memberIdentifier())
                .hasSameElementsAs(ImmutableSet.<String>builder()
                        .addAll(ChildWithFieldAndMethod.Members.MEMBERS)
                        .addAll(ParentWithFieldAndMethod.Members.MEMBERS)
                        .addAll(InterfaceWithFieldAndMethod.Members.MEMBERS)
                        .build());
    }

    @Test
    public void getCodeUnitWithParameterTypes() {
        JavaClass clazz = importClasses(ChildWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertIllegalArgumentException("childMethod", () -> clazz.getCodeUnitWithParameterTypes("childMethod"));
        assertIllegalArgumentException("childMethod", () -> clazz.getCodeUnitWithParameterTypes("childMethod", Object.class));
        assertIllegalArgumentException("wrong", () -> clazz.getCodeUnitWithParameterTypes("wrong", String.class));

        assertThatCodeUnit(clazz.getCodeUnitWithParameterTypes("childMethod", String.class))
                .matchesMethod(ChildWithFieldAndMethod.class, "childMethod", String.class);
        assertThatCodeUnit(clazz.getCodeUnitWithParameterTypeNames("childMethod", String.class.getName()))
                .matchesMethod(ChildWithFieldAndMethod.class, "childMethod", String.class);
        assertThatCodeUnit(clazz.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, Object.class))
                .matchesConstructor(ChildWithFieldAndMethod.class, Object.class);
        assertThatCodeUnit(clazz.getCodeUnitWithParameterTypeNames(CONSTRUCTOR_NAME, Object.class.getName()))
                .matchesConstructor(ChildWithFieldAndMethod.class, Object.class);
    }

    @Test
    public void tryGetCodeUnitWithParameterTypes() {
        JavaClass clazz = importClasses(ChildWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertThatCodeUnit(clazz.tryGetCodeUnitWithParameterTypes("childMethod", Collections.singletonList(String.class)).get())
                .matchesMethod(ChildWithFieldAndMethod.class, "childMethod", String.class);
        assertThatCodeUnit(clazz.tryGetCodeUnitWithParameterTypeNames("childMethod", singletonList(String.class.getName())).get())
                .matchesMethod(ChildWithFieldAndMethod.class, "childMethod", String.class);
        assertThatCodeUnit(clazz.tryGetCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, Collections.singletonList(Object.class)).get())
                .matchesConstructor(ChildWithFieldAndMethod.class, Object.class);
        assertThatCodeUnit(clazz.tryGetCodeUnitWithParameterTypeNames(CONSTRUCTOR_NAME, singletonList(Object.class.getName())).get())
                .matchesConstructor(ChildWithFieldAndMethod.class, Object.class);

        assertThat(clazz.tryGetCodeUnitWithParameterTypes("childMethod", Collections.emptyList())).isEmpty();
        assertThat(clazz.tryGetCodeUnitWithParameterTypeNames("childMethod", Collections.emptyList())).isEmpty();
        assertThat(clazz.tryGetCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, Collections.emptyList())).isEmpty();
        assertThat(clazz.tryGetCodeUnitWithParameterTypeNames(CONSTRUCTOR_NAME, Collections.emptyList())).isEmpty();
    }

    @Test
    public void getMethod_returns_non_synthetic_method_if_method_name_and_parameters_are_ambiguous() {
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

        JavaClass javaClass = new ClassFileImporter().importClasses(Parent.class, Child.class).get(Child.class);
        JavaMethod method = javaClass.getMethod("covariantlyOverriddenCausingBridgeMethod");

        assertThatType(method.getReturnType()).matches(String.class);
        assertThat(method.getModifiers()).doesNotContain(SYNTHETIC);
    }

    @Test
    public void has_no_dependencies_to_primitives() {
        JavaClass javaClass = importClassWithContext(AllPrimitiveDependencies.class);
        assertThat(javaClass.getDirectDependenciesFromSelf()).doNotHave(anyDependency().toPrimitives());
    }

    @Test
    public void has_no_self_dependencies() {
        JavaClass javaClass = importClassWithContext(ClassWithSelfReferences.class);

        assertThat(javaClass.getDirectDependenciesFromSelf()).doNotHave(anyDependency().toClassEquivalentTo(ClassWithSelfReferences.class));
        assertThat(javaClass.getDirectDependenciesToSelf()).doNotHave(anyDependency().fromClassEquivalentTo(ClassWithSelfReferences.class));
    }

    @Test
    public void direct_dependencies_from_self_by_accesses() {
        JavaClass javaClass = importClasses(AAccessingB.class, B.class).get(AAccessingB.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .areAtLeastOne(callDependency()
                        .from(AAccessingB.class)
                        .to(B.class, CONSTRUCTOR_NAME)
                        .inLineNumber(5))
                .areAtLeastOne(setFieldDependency()
                        .from(AAccessingB.class)
                        .to(B.class, "field")
                        .inLineNumber(6))
                .areAtLeastOne(callDependency()
                        .from(AAccessingB.class)
                        .to(B.class, "call")
                        .inLineNumber(7));
    }

    @Test
    public void direct_dependencies_from_self_by_inheritance() {
        JavaClass javaClass = importClassWithContext(AExtendingSuperAImplementingInterfaceForA.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .areAtLeastOne(extendsDependency()
                        .from(AExtendingSuperAImplementingInterfaceForA.class)
                        .to(SuperA.class)
                        .inLineNumber(0))
                .areAtLeastOne(implementsDependency()
                        .from(AExtendingSuperAImplementingInterfaceForA.class)
                        .to(InterfaceForA.class)
                        .inLineNumber(0));
    }

    @Test
    public void direct_dependencies_from_self_by_inheritance_through_generic_superclass_parameters() {
        @SuppressWarnings("unused")
        class Base<X, Y> {
        }
        class Child<FIRST, SECOND> extends Base<
                Comparable<Child<FIRST, SECOND>>,
                Map<
                        Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                        Map<? extends BufferedInputStream[][],
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>>>>> {
        }

        JavaClass javaClass = importClassWithContext(Child.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(Child.class).to(Base.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("extends")

                        .from(Child.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic superclass <" + quote(Base.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(Comparable.class, Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)

                        .from(Child.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_inheritance_through_generic_interface_parameters() {
        class Child<FIRST, SECOND> implements InterfaceWithTwoTypeParameters<
                Comparable<Child<FIRST, SECOND>>,
                Map<
                        Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                        Map<? extends BufferedInputStream[][],
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>>>>> {
        }

        JavaClass javaClass = importClassWithContext(Child.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(Child.class).to(InterfaceWithTwoTypeParameters.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("implements")

                        .from(Child.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic interface <" + quote(InterfaceWithTwoTypeParameters.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(Comparable.class, Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)

                        .from(Child.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_field_declarations() {
        @SuppressWarnings("unused")
        class SomeGenericType<FIRST, SECOND> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST, SECOND> {
            // @formatter:off
            SomeGenericType<
              Comparable<SomeGenericType<FIRST, SECOND>>,
              Map<
                Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                Map<
                  ? extends BufferedInputStream[][],
                  Map<
                    ? extends Serializable,
                    List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>
                  >
                >
              >
            > field;
            // @formatter:on
        }

        JavaClass javaClass = importClassWithContext(SomeClass.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(SomeClass.class).to(SomeGenericType.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("Field <%s.field> has type <%s>", SomeClass.class.getName(), SomeGenericType.class.getName())

                        .from(SomeClass.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic type <" + quote(SomeGenericType.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(Comparable.class, Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)

                        .from(SomeClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_method_return_type_declarations() {
        @SuppressWarnings("unused")
        class SomeGenericType<FIRST, SECOND> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST, SECOND> {
            // @formatter:off
            SomeGenericType<
              Comparable<SomeGenericType<FIRST, SECOND>>,
              Map<
                Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                Map<
                  ? extends BufferedInputStream[][],
                  Map<
                    ? extends Serializable,
                    List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>
                  >
                >
              >
            > method() {
              return null;
            }
            // @formatter:on
        }

        JavaClass javaClass = importClassWithContext(SomeClass.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(SomeClass.class).to(SomeGenericType.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("Method <%s.method()> has return type <%s>", SomeClass.class.getName(), SomeGenericType.class.getName())

                        .from(SomeClass.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic return type <" + quote(SomeGenericType.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(Comparable.class, Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)

                        .from(SomeClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_method_parameter_type_declarations() {
        @SuppressWarnings("unused")
        class SomeGenericType<FIRST, SECOND> {
        }
        @SuppressWarnings("unused")
        class SomeClass<FIRST, SECOND> {
            // @formatter:off
            void method(
              SomeGenericType<
                Comparable<SomeGenericType<FIRST, SECOND>>,
                Map<
                  Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                  Map<
                    ? extends BufferedInputStream[][],
                    Map<
                      ? extends Serializable,
                      List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>
                    >
                  >
                >
              > firstParam,
              List<? extends FileSystem> secondParam) {
            }
            // @formatter:on
        }

        JavaClass javaClass = importClassWithContext(SomeClass.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(SomeClass.class).to(SomeGenericType.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("Method <%s.method(%s, %s)> has parameter of type <%s>",
                                SomeClass.class.getName(), SomeGenericType.class.getName(), List.class.getName(), SomeGenericType.class.getName())

                        .from(SomeClass.class).to(List.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("Method <%s.method(%s, %s)> has parameter of type <%s>",
                                SomeClass.class.getName(), SomeGenericType.class.getName(), List.class.getName(), List.class.getName())

                        .from(SomeClass.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic parameter type <" + quote(SomeGenericType.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(Comparable.class, Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)

                        .from(SomeClass.class)
                        .withExpectedDescriptionPatternTemplate(
                                ".*has generic parameter type <" + quote(List.class.getName()) +
                                        ".+> with type argument depending on <#target>.*")
                        .to(FileSystem.class)

                        .from(SomeClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_member_declarations() {
        JavaClass javaClass = importClasses(AhavingMembersOfTypeB.class, B.class).get(AhavingMembersOfTypeB.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .areAtLeastOne(methodReturnTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
                .areAtLeastOne(methodThrowsDeclarationDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.BException.class)
                        .inLineNumber(0))
                .areAtLeast(2, parameterTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
                .areAtLeastOne(methodChecksInstanceOfDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(7))
                .areAtLeastOne(methodChecksInstanceOfDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(25));
    }

    @Test
    public void direct_dependencies_from_self_by_annotation() {
        JavaClass javaClass = importClasses(ClassWithAnnotationDependencies.class)
                .get(ClassWithAnnotationDependencies.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnClass.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnField.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnConstructor.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnMethod.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnMethodParam.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(WithType.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(B.class)
                        .inLineNumber(0));
    }

    @Test
    public void finds_array_component_types_as_dependencies_from_self() {
        JavaClass javaClass = importClassWithContext(ArrayComponentTypeDependencies.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(
                        from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Field <%s.asField> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Constructor <%s.<init>(%s)> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency[].class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asMethodParameter(%s)> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency[].class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asReturnType()> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asCallTarget()> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 18));
    }

    @Test
    public void direct_dependencies_from_self_by_class_type_parameters() {
        @SuppressWarnings("unused")
        class ClassWithTypeParameters<
                FIRST extends List<?> & Serializable & Comparable<FIRST>,
                SECOND extends Map<
                        Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                        Map<? extends BufferedInputStream[][],
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>>>>,
                SELF extends ClassWithTypeParameters<FIRST, SECOND, SELF>> {
        }

        JavaClass javaClass = importClasses(ClassWithTypeParameters.class).get(ClassWithTypeParameters.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(ClassWithTypeParameters.class)
                        .withExpectedDescriptionTemplate("type parameter 'FIRST' depending on")
                        .to(List.class, Serializable.class, Comparable.class)
                        .inLocation(getClass(), 0)

                        .from(ClassWithTypeParameters.class)
                        .withExpectedDescriptionTemplate("type parameter 'SECOND' depending on")
                        .to(Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)
                        .inLocation(getClass(), 0)

                        .from(ClassWithTypeParameters.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @DataProvider
    public static Object[][] data_direct_dependencies_from_self_by_code_unit_type_parameters() {
        @SuppressWarnings("unused")
        class TypeParametersOnConstructor {
            <
                    FIRST extends List<?> & Serializable & Comparable<FIRST>,
                    SECOND extends Map<
                            Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                            Map<? extends BufferedInputStream[][],
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>>>>,
                    SELF extends TypeParametersOnConstructor
                    > TypeParametersOnConstructor() {
            }
        }
        @SuppressWarnings("unused")
        class TypeParametersOnMethod {
            <
                    FIRST extends List<?> & Serializable & Comparable<FIRST>,
                    SECOND extends Map<
                            Map.Entry<FIRST, Map.Entry<String, FIRST>>,
                            Map<? extends BufferedInputStream[][],
                                    Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<FIRST, ? extends File>>>>>>>>,
                    SELF extends TypeParametersOnMethod
                    > void typeParametersOnMethod() {
            }
        }
        return testForEach(
                importClasses(TypeParametersOnConstructor.class).get(TypeParametersOnConstructor.class),
                importClasses(TypeParametersOnMethod.class).get(TypeParametersOnMethod.class)
        );
    }

    @Test
    @UseDataProvider
    public void test_direct_dependencies_from_self_by_code_unit_type_parameters(JavaClass javaClass) {
        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(javaClass)
                        .withExpectedDescriptionTemplate("type parameter 'FIRST' depending on")
                        .to(List.class, Serializable.class, Comparable.class)
                        .inLocation(getClass(), 0)

                        .from(javaClass)
                        .withExpectedDescriptionTemplate("type parameter 'SECOND' depending on")
                        .to(Map.class, Map.Entry.class, String.class, BufferedInputStream[][].class, Serializable.class, List.class, Set.class, Iterable.class, File.class)
                        .inLocation(getClass(), 0)

                        .from(javaClass).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_from_self_by_referenced_class_objects() {
        JavaClass javaClass = new ClassFileImporter().importClass(DependenciesOnClassObjects.class);

        assertThatDependencies(javaClass.getDirectDependenciesFromSelf())
                .contain(from(DependenciesOnClassObjects.class).to(FilterInputStream.class).inLocation(DependenciesOnClassObjects.class, 16)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.class).to(Buffer.class).inLocation(DependenciesOnClassObjects.class, 16)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.class).to(File.class).inLocation(DependenciesOnClassObjects.class, 19)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.class).to(Path.class).inLocation(DependenciesOnClassObjects.class, 19)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.class).to(FileSystem.class).inLocation(DependenciesOnClassObjects.class, 22)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.class).to(Charset.class).inLocation(DependenciesOnClassObjects.class, 22)
                        .withDescriptionContaining("references class object")
                );
    }

    @Test
    public void direct_dependencies_from_self_finds_correct_set_of_target_types() {
        JavaClass javaClass = importPackagesOf(getClass()).get(ClassWithAnnotationDependencies.class);

        Set<JavaClass> targets = javaClass.getDirectDependenciesFromSelf().stream().map(GET_TARGET_CLASS).collect(toSet());

        assertThatTypes(targets).matchInAnyOrder(
                B.class, AhavingMembersOfTypeB.class, Object.class, String.class,
                List.class, Serializable.class, SomeSuperclass.class,
                WithType.class, WithNestedAnnotations.class, OnClass.class, OnMethod.class, OnMethodParam.class,
                OnConstructor.class, OnField.class, MetaAnnotated.class, WithEnum.class, WithPrimitive.class,
                WithOtherEnum.class, WithOtherType.class,
                SomeEnumAsAnnotationParameter.class, SomeEnumAsAnnotationArrayParameter.class,
                SomeEnumAsNestedAnnotationParameter.class, SomeEnumAsDefaultParameter.class,
                SomeOtherEnumAsAnnotationParameter.class, SomeOtherEnumAsDefaultParameter.class,
                SomeOtherEnumAsAnnotationArrayParameter.class, SomeTypeAsDefaultParameter.class);
    }

    @Test
    public void direct_dependencies_to_self_by_accesses() {
        JavaClass javaClass = importClassesWithContext(AAccessingB.class, B.class).get(B.class);

        assertThat(javaClass.getDirectDependenciesToSelf())
                .hasSize(3)
                .areAtLeastOne(callDependency()
                        .from(AAccessingB.class)
                        .to(B.class, CONSTRUCTOR_NAME)
                        .inLineNumber(5))
                .areAtLeastOne(setFieldDependency()
                        .from(AAccessingB.class)
                        .to(B.class, "field")
                        .inLineNumber(6))
                .areAtLeastOne(callDependency()
                        .from(AAccessingB.class)
                        .to(B.class, "call")
                        .inLineNumber(7));
    }

    @Test
    public void direct_dependencies_to_self_by_inheritance() {
        JavaClass superclass = importClassesWithContext(SuperA.class, AExtendingSuperAImplementingInterfaceForA.class).get(SuperA.class);

        assertThat(superclass.getDirectDependenciesToSelf())
                .areAtLeastOne(extendsDependency()
                        .from(AExtendingSuperAImplementingInterfaceForA.class)
                        .to(SuperA.class)
                        .inLineNumber(0));

        JavaClass interfaceClass =
                importClassesWithContext(InterfaceForA.class, AExtendingSuperAImplementingInterfaceForA.class).get(InterfaceForA.class);

        assertThat(interfaceClass.getDirectDependenciesToSelf())
                .areAtLeastOne(implementsDependency()
                        .from(AExtendingSuperAImplementingInterfaceForA.class)
                        .to(InterfaceForA.class)
                        .inLineNumber(0));
    }

    @Test
    public void direct_dependencies_to_self_by_inheritance_through_generic_superclass_parameters() {
        @SuppressWarnings("unused")
        class FirstBase<X, Y> {
        }
        class FirstChild extends FirstBase<
                Comparable<FirstChild>,
                Map<
                        Map.Entry<?, Map.Entry<String, ?>>,
                        Map<? extends BufferedInputStream[][],
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<?, ? extends File>>>>>>>>> {
        }
        @SuppressWarnings("unused")
        class SecondBase<T> {
        }
        class SecondChild extends SecondBase<Map<?, ? super File>> {
        }

        JavaClasses javaClasses = importClassesWithContext(FirstChild.class, SecondChild.class, BufferedInputStream.class, File.class);

        assertThatDependencies(javaClasses.get(File.class).getDirectDependenciesToSelf())
                .contain(from(FirstChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Class <%s> has generic superclass <%s.+> with type argument depending on <%s>.*",
                                FirstChild.class.getName(), FirstBase.class.getName(), File.class.getName())

                        .from(SecondChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Class <%s> has generic superclass <%s.+> with type argument depending on <%s>.*",
                                SecondChild.class.getName(), SecondBase.class.getName(), File.class.getName())
                );

        assertThatDependencies(javaClasses.get(BufferedInputStream.class).getDirectDependenciesToSelf())
                .contain(from(FirstChild.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_to_self_by_inheritance_through_generic_interface_parameters() {
        class FirstChild implements InterfaceWithTwoTypeParameters<
                Comparable<FirstChild>,
                Map<
                        Map.Entry<?, Map.Entry<String, ?>>,
                        Map<? extends BufferedInputStream[][],
                                Map<? extends Serializable, List<List<? extends Set<? super Iterable<? super Map<?, ? extends File>>>>>>>>> {
        }
        class SecondChild implements InterfaceWithTypeParameter<Map<?, ? super File>> {
        }

        JavaClasses javaClasses = importClassesWithContext(FirstChild.class, SecondChild.class, BufferedInputStream.class, File.class);

        assertThatDependencies(javaClasses.get(File.class).getDirectDependenciesToSelf())
                .contain(from(FirstChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Class <%s> has generic interface <%s.+> with type argument depending on <%s>.*",
                                FirstChild.class.getName(), InterfaceWithTwoTypeParameters.class.getName(), File.class.getName())

                        .from(SecondChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Class <%s> has generic interface <%s.+> with type argument depending on <%s>.*",
                                SecondChild.class.getName(), InterfaceWithTypeParameter.class.getName(), File.class.getName())
                );

        assertThatDependencies(javaClasses.get(BufferedInputStream.class).getDirectDependenciesToSelf())
                .contain(from(FirstChild.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_to_self_by_generic_field_type_parameters() {
        @SuppressWarnings("unused")
        class FirstGenericType<X, Y> {
        }
        @SuppressWarnings("unused")
        class FirstClass {
            // @formatter:off
            FirstGenericType<
              Comparable<FirstClass>,
              Map<
                Map.Entry<?, Map.Entry<String, ?>>,
                Map<
                  ? extends BufferedInputStream[][],
                  Map<
                    ? extends Serializable,
                    List<List<? extends Set<? super Iterable<? super Map<?, ? extends File>>>>>
                  >
                >
              >
            > field;
            // @formatter:on
        }
        @SuppressWarnings("unused")
        class SecondGenericType<T> {
        }
        @SuppressWarnings("unused")
        class SecondChild {
            SecondGenericType<Map<?, ? super File>> field;
        }

        JavaClasses javaClasses = importClassesWithContext(FirstClass.class, SecondChild.class, BufferedInputStream.class, File.class);

        assertThatDependencies(javaClasses.get(File.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Field <%s.field> has generic type <%s.+> with type argument depending on <%s>.*",
                                FirstClass.class.getName(), FirstGenericType.class.getName(), File.class.getName())

                        .from(SecondChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Field <%s.field> has generic type <%s.+> with type argument depending on <%s>.*",
                                SecondChild.class.getName(), SecondGenericType.class.getName(), File.class.getName())
                );

        assertThatDependencies(javaClasses.get(BufferedInputStream.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_to_self_by_generic_method_return_type_parameters() {
        @SuppressWarnings("unused")
        class FirstGenericType<X, Y> {
        }
        @SuppressWarnings("unused")
        class FirstClass {
            // @formatter:off
            FirstGenericType<
              Comparable<FirstClass>,
              Map<
                Map.Entry<?, Map.Entry<String, ?>>,
                Map<
                  ? extends BufferedInputStream[][],
                  Map<
                    ? extends Serializable,
                    List<List<? extends Set<? super Iterable<? super Map<?, ? extends File>>>>>
                  >
                >
              >
            > method() {
              return null;
            }
            // @formatter:on
        }
        @SuppressWarnings("unused")
        class SecondGenericType<T> {
        }
        @SuppressWarnings("unused")
        class SecondChild {
            SecondGenericType<Map<?, ? super File>> method() {
                return null;
            }
        }

        JavaClasses javaClasses = importClassesWithContext(FirstClass.class, SecondChild.class, BufferedInputStream.class, File.class);

        assertThatDependencies(javaClasses.get(File.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Method <%s.method\\(\\)> has generic return type <%s.+> with type argument depending on <%s>.*",
                                FirstClass.class.getName(), FirstGenericType.class.getName(), File.class.getName())

                        .from(SecondChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Method <%s.method\\(\\)> has generic return type <%s.+> with type argument depending on <%s>.*",
                                SecondChild.class.getName(), SecondGenericType.class.getName(), File.class.getName())
                );

        assertThatDependencies(javaClasses.get(BufferedInputStream.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_to_self_by_generic_method_parameter_type_parameters() {
        @SuppressWarnings("unused")
        class FirstGenericType<X, Y> {
        }
        @SuppressWarnings("unused")
        class FirstClass {
            // @formatter:off
            void method(
              FirstGenericType<
                Comparable<FirstClass>,
                Map<
                  Map.Entry<?, Map.Entry<String, ?>>,
                  Map<
                    ? extends BufferedInputStream[][],
                    Map<
                      ? extends Serializable,
                      List<List<? extends Set<? super Iterable<? super Map<?, ? extends File>>>>>
                    >
                  >
                >
              > param) {
            }
            // @formatter:on
        }
        @SuppressWarnings("unused")
        class SecondGenericType<T> {
        }
        @SuppressWarnings("unused")
        class SecondChild {
            void method(List<String> irrelevant, SecondGenericType<Map<?, ? super File>> param) {
            }
        }

        JavaClasses javaClasses = importClassesWithContext(FirstClass.class, SecondChild.class, BufferedInputStream.class, File.class);

        assertThatDependencies(javaClasses.get(File.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Method <%s.method\\([^)]+\\)> has generic parameter type <%s.+> with type argument depending on <%s>.*",
                                FirstClass.class.getName(), FirstGenericType.class.getName(), File.class.getName())

                        .from(SecondChild.class).to(File.class).inLocation(getClass(), 0)
                        .withDescriptionMatching("Method <%s.method\\([^)]+\\)> has generic parameter type <%s.+> with type argument depending on <%s>.*",
                                SecondChild.class.getName(), SecondGenericType.class.getName(), File.class.getName())
                );

        assertThatDependencies(javaClasses.get(BufferedInputStream.class).getDirectDependenciesToSelf())
                .contain(from(FirstClass.class).to(BufferedInputStream.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("depends on component type <%s>", BufferedInputStream.class.getName())
                );
    }

    @Test
    public void direct_dependencies_to_self_by_member_declarations() {
        JavaClass javaClass = importClassesWithContext(AhavingMembersOfTypeB.class, B.class).get(B.class);

        assertThat(javaClass.getDirectDependenciesToSelf())
                .areAtLeastOne(fieldTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
                .areAtLeastOne(methodReturnTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
                .areAtLeast(2, parameterTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
                .areAtLeastOne(methodChecksInstanceOfDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(7))
                .areAtLeastOne(methodChecksInstanceOfDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(25));

        JavaClass exceptionClass = importClassesWithContext(AhavingMembersOfTypeB.class, B.BException.class)
                .get(B.BException.class);

        assertThat(exceptionClass.getDirectDependenciesToSelf())
                .areAtLeastOne(methodThrowsDeclarationDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.BException.class)
                        .inLineNumber(0));
    }

    @Test
    public void direct_dependencies_from_self_by_references() {
        JavaClass javaClass = importClasses(AReferencingB.class, BReferencedByA.class).get(AReferencingB.class);

        assertReferencesFromAToB(javaClass.getDirectDependenciesFromSelf());
    }

    @Test
    public void direct_dependencies_to_self_by_references() {
        JavaClass javaClass = importClasses(AReferencingB.class, BReferencedByA.class).get(BReferencedByA.class);

        assertReferencesFromAToB(javaClass.getDirectDependenciesToSelf());
    }

    private void assertReferencesFromAToB(Set<Dependency> dependencies) {
        assertThat(dependencies)
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, CONSTRUCTOR_NAME)
                        .inLineNumber(9))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, CONSTRUCTOR_NAME)
                        .inLineNumber(10))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getSomeField")
                        .inLineNumber(14))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getSomeField")
                        .inLineNumber(15))
                .areAtLeastOne(referenceDependency()
                        .from(AReferencingB.class)
                        .to(BReferencedByA.class, "getNothing")
                        .inLineNumber(16));
    }

    @Test
    public void direct_dependencies_to_self_by_annotation() {
        JavaClasses javaClasses = importPackagesOf(getClass());

        assertThat(javaClasses.get(OnClass.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnClass.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(OnField.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnField.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(OnMethod.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnMethod.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(OnMethodParam.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnMethodParam.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(OnConstructor.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(OnConstructor.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(WithType.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(WithType.class)
                        .inLineNumber(0));

        assertThat(javaClasses.get(B.class).getDirectDependenciesToSelf())
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(B.class)
                        .inLineNumber(0));
    }

    @Test
    public void finds_array_component_types_as_dependencies_to_self() {
        JavaClass javaClass = new ClassFileImporter().importClasses(ArrayComponentTypeDependencies.class, ComponentTypeDependency.class)
                .get(ComponentTypeDependency.class);

        assertThatDependencies(javaClass.getDirectDependenciesToSelf())
                .contain(
                        from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Field <%s.asField> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Constructor <%s.<init>(%s)> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency[].class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asMethodParameter(%s)> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency[].class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asReturnType()> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 0).

                                from(ArrayComponentTypeDependencies.class).to(ComponentTypeDependency.class)
                                .withDescriptionContaining("Method <%s.asCallTarget()> depends on component type <%s>",
                                        ArrayComponentTypeDependencies.class.getName(), ComponentTypeDependency.class.getName())
                                .inLocation(ArrayComponentTypeDependencies.class, 18));
    }

    @Test
    public void direct_dependencies_to_self_by_class_type_parameters() {
        class ClassOtherTypeSignaturesDependOn {
        }
        @SuppressWarnings("unused")
        class FirstDependingOnOtherThroughTypeParameter<T extends ClassOtherTypeSignaturesDependOn> {
        }
        @SuppressWarnings("unused")
        class SecondDependingOnOtherThroughTypeParameter<
                U extends Map<?, List<? super Set<? extends ClassOtherTypeSignaturesDependOn>>>,
                V extends Map<ClassOtherTypeSignaturesDependOn, ClassOtherTypeSignaturesDependOn>> {
        }

        JavaClass someClass = importClasses(ClassOtherTypeSignaturesDependOn.class, FirstDependingOnOtherThroughTypeParameter.class, SecondDependingOnOtherThroughTypeParameter.class)
                .get(ClassOtherTypeSignaturesDependOn.class);

        assertThatDependencies(someClass.getDirectDependenciesToSelf())
                .contain(from(FirstDependingOnOtherThroughTypeParameter.class).to(ClassOtherTypeSignaturesDependOn.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'T' depending on")

                        .from(SecondDependingOnOtherThroughTypeParameter.class).to(ClassOtherTypeSignaturesDependOn.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'U' depending on")

                        .from(SecondDependingOnOtherThroughTypeParameter.class).to(ClassOtherTypeSignaturesDependOn.class).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'V' depending on")
                );
    }

    @DataProvider
    public static Object[][] data_direct_dependencies_to_self_by_code_unit_type_parameters() {
        class ClassOtherConstructorTypeSignaturesDependOn {
        }
        @SuppressWarnings("unused")
        class FirstDependingOnOtherThroughConstructorTypeParameter {
            <T extends ClassOtherConstructorTypeSignaturesDependOn> FirstDependingOnOtherThroughConstructorTypeParameter() {
            }
        }
        @SuppressWarnings("unused")
        class SecondDependingOnOtherThroughConstructorTypeParameter {
            <
                    U extends Map<?, List<? super Set<? extends ClassOtherConstructorTypeSignaturesDependOn>>>,
                    V extends Map<ClassOtherConstructorTypeSignaturesDependOn, ClassOtherConstructorTypeSignaturesDependOn>
                    > SecondDependingOnOtherThroughConstructorTypeParameter() {
            }
        }

        class ClassOtherMethodTypeSignaturesDependOn {
        }
        @SuppressWarnings("unused")
        class FirstDependingOnOtherThroughMethodTypeParameter {
            <T extends ClassOtherMethodTypeSignaturesDependOn> void firstDependingOnOtherThroughMethodTypeParameter() {
            }
        }
        @SuppressWarnings("unused")
        class SecondDependingOnOtherThroughMethodTypeParameter {
            <
                    U extends Map<?, List<? super Set<? extends ClassOtherMethodTypeSignaturesDependOn>>>,
                    V extends Map<ClassOtherMethodTypeSignaturesDependOn, ClassOtherMethodTypeSignaturesDependOn>
                    > void secondDependingOnOtherThroughMethodTypeParameter() {
            }
        }

        JavaClasses dependenciesThroughConstructorTypeParameters = importClasses(
                ClassOtherConstructorTypeSignaturesDependOn.class,
                FirstDependingOnOtherThroughConstructorTypeParameter.class,
                SecondDependingOnOtherThroughConstructorTypeParameter.class
        );
        JavaClasses dependenciesThroughMethodTypeParameters = importClasses(
                ClassOtherMethodTypeSignaturesDependOn.class,
                FirstDependingOnOtherThroughMethodTypeParameter.class,
                SecondDependingOnOtherThroughMethodTypeParameter.class
        );
        return $$(
                $(
                        dependenciesThroughConstructorTypeParameters.get(FirstDependingOnOtherThroughConstructorTypeParameter.class),
                        dependenciesThroughConstructorTypeParameters.get(SecondDependingOnOtherThroughConstructorTypeParameter.class),
                        dependenciesThroughConstructorTypeParameters.get(ClassOtherConstructorTypeSignaturesDependOn.class)
                ),
                $(
                        dependenciesThroughMethodTypeParameters.get(FirstDependingOnOtherThroughMethodTypeParameter.class),
                        dependenciesThroughMethodTypeParameters.get(SecondDependingOnOtherThroughMethodTypeParameter.class),
                        dependenciesThroughMethodTypeParameters.get(ClassOtherMethodTypeSignaturesDependOn.class)
                )
        );
    }

    @Test
    @UseDataProvider
    public void test_direct_dependencies_to_self_by_code_unit_type_parameters(JavaClass firstOrigin, JavaClass secondOrigin, JavaClass expectedTarget) {
        assertThatDependencies(expectedTarget.getDirectDependenciesToSelf())
                .contain(from(firstOrigin).to(expectedTarget).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'T' depending on")

                        .from(secondOrigin).to(expectedTarget).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'U' depending on")

                        .from(secondOrigin).to(expectedTarget).inLocation(getClass(), 0)
                        .withDescriptionContaining("type parameter 'V' depending on")
                );
    }

    @Test
    public void direct_dependencies_to_self_by_referenced_class_objects() {
        JavaClass javaClass = new ClassFileImporter()
                .importClasses(DependenciesOnClassObjects.class, DependenciesOnClassObjects.MoreDependencies.class, Charset.class)
                .get(Charset.class);

        assertThatDependencies(javaClass.getDirectDependenciesToSelf())
                .contain(from(DependenciesOnClassObjects.class).to(Charset.class).inLocation(DependenciesOnClassObjects.class, 22)
                        .withDescriptionContaining("references class object")

                        .from(DependenciesOnClassObjects.MoreDependencies.class).to(Charset.class).inLocation(DependenciesOnClassObjects.class, 27)
                        .withDescriptionContaining("references class object")
                );
    }

    @Test
    public void direct_dependencies_to_self_finds_correct_set_of_origin_types() {
        JavaClasses classes = importPackagesOf(getClass());

        Set<JavaClass> origins = getOriginsOfDependenciesTo(classes.get(WithType.class));

        assertThatTypes(origins).matchInAnyOrder(
                getClass(), ClassWithAnnotationDependencies.class, ClassWithSelfReferences.class, WithNestedAnnotations.class, OnMethodParam.class);

        origins = getOriginsOfDependenciesTo(classes.get(B.class));

        assertThatTypes(origins).matchInAnyOrder(
                getClass(), ClassWithAnnotationDependencies.class, OnMethodParam.class, AAccessingB.class, AhavingMembersOfTypeB.class);

        origins = getOriginsOfDependenciesTo(classes.get(SomeEnumAsNestedAnnotationParameter.class));

        assertThatTypes(origins).matchInAnyOrder(
                getClass(), ClassWithAnnotationDependencies.class, WithEnum.class);
    }

    private Set<JavaClass> getOriginsOfDependenciesTo(JavaClass withType) {
        return withType.getDirectDependenciesToSelf().stream().map(GET_ORIGIN_CLASS).collect(toSet());
    }

    @Test
    public void function_getSimpleName() {
        assertThat(JavaClass.Functions.GET_SIMPLE_NAME.apply(importClassWithContext(List.class)))
                .as("result of GET_SIMPLE_NAME(clazz)")
                .isEqualTo(List.class.getSimpleName());
    }

    @Test
    public void function_getPackage() {
        JavaClass javaClass = importClassWithContext(List.class);
        assertThat(JavaClass.Functions.GET_PACKAGE.apply(javaClass))
                .as("result of GET_PACKAGE(clazz)")
                .isEqualTo(javaClass.getPackage());

        assertThat(JavaClass.Functions.GET_PACKAGE_NAME.apply(javaClass))
                .as("result of GET_PACKAGE_NAME(clazz)")
                .isEqualTo(javaClass.getPackageName());
    }

    @Test
    public void functions_get_members() {
        JavaClass javaClass = importClassWithContext(ClassWithSeveralConstructorsFieldsAndMethods.class);

        assertThat(GET_MEMBERS.apply(javaClass)).isEqualTo(javaClass.getMembers());
        assertThat(GET_FIELDS.apply(javaClass)).isEqualTo(javaClass.getFields());
        assertThat(GET_CODE_UNITS.apply(javaClass)).isEqualTo(javaClass.getCodeUnits());
        assertThat(GET_METHODS.apply(javaClass)).isEqualTo(javaClass.getMethods());
        assertThat(GET_CONSTRUCTORS.apply(javaClass)).isEqualTo(javaClass.getConstructors());
        assertThat(GET_STATIC_INITIALIZER.apply(javaClass)).contains(javaClass.getStaticInitializer().get());
    }

    static class Data_function_get_code_unit_references {
        static class Target {
            void target() {
            }
        }
    }

    @Test
    public void function_get_code_unit_references_from_self() {
        @SuppressWarnings("unused")
        class Origin {
            Consumer<Data_function_get_code_unit_references.Target> origin() {
                Supplier<Data_function_get_code_unit_references.Target> supplier =
                        Data_function_get_code_unit_references.Target::new;
                return Data_function_get_code_unit_references.Target::target;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClasses(Origin.class, Data_function_get_code_unit_references.Target.class).get(Origin.class);

        assertThat(JavaClass.Functions.GET_CODE_UNIT_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getCodeUnitReferencesFromSelf());
        assertThat(JavaClass.Functions.GET_METHOD_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getMethodReferencesFromSelf());
        assertThat(JavaClass.Functions.GET_CONSTRUCTOR_REFERENCES_FROM_SELF.apply(javaClass)).isEqualTo(javaClass.getConstructorReferencesFromSelf());
    }

    @Test
    public void function_get_code_unit_references_to_self() {
        @SuppressWarnings("unused")
        class Origin {
            Consumer<Data_function_get_code_unit_references.Target> origin() {
                Supplier<Data_function_get_code_unit_references.Target> supplier = Data_function_get_code_unit_references.Target::new;
                return Data_function_get_code_unit_references.Target::target;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClasses(Origin.class, Data_function_get_code_unit_references.Target.class)
                .get(Data_function_get_code_unit_references.Target.class);

        assertThat(JavaClass.Functions.GET_CODE_UNIT_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getCodeUnitReferencesToSelf());
        assertThat(JavaClass.Functions.GET_METHOD_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getMethodReferencesToSelf());
        assertThat(JavaClass.Functions.GET_CONSTRUCTOR_REFERENCES_TO_SELF.apply(javaClass)).isEqualTo(javaClass.getConstructorReferencesToSelf());
    }

    @Test
    public void predicate_withType() {
        assertThat(type(Parent.class))
                .accepts(importClassWithContext(Parent.class))
                .rejects(importClassWithContext(SuperclassWithFieldAndMethod.class));

        assertThat(type(System.class)).hasDescription("type java.lang.System");
    }

    @Test
    public void predicate_simpleName() {
        assertThat(simpleName(Parent.class.getSimpleName()))
                .accepts(importClassWithContext(Parent.class))
                .rejects(importClassWithContext(SuperclassWithFieldAndMethod.class));

        assertThat(simpleName("Simple")).hasDescription("simple name 'Simple'");
    }

    @Test
    public void predicate_simpleNameStartingWith() {
        JavaClass input = importClassWithContext(Parent.class);
        assertThat(simpleNameStartingWith("P")).accepts(input);
        assertThat(simpleNameStartingWith("Pa")).accepts(input);
        assertThat(simpleNameStartingWith("PA")).rejects(input);
        assertThat(simpleNameStartingWith(".P")).rejects(input);
        assertThat(simpleNameStartingWith("")).accepts(input);

        assertThat(simpleNameStartingWith("wrong")).rejects(input);

        // Full match test
        assertThat(simpleNameStartingWith(input.getName())).rejects(input);
        assertThat(simpleNameStartingWith(input.getName().substring(0, 2))).rejects(input);

        assertThat(simpleNameStartingWith("Prefix")).hasDescription("simple name starting with 'Prefix'");
    }

    @Test
    public void predicate_simpleNameContaining() {
        JavaClass input = importClassWithContext(Parent.class);

        assertThat(simpleNameContaining("Parent")).accepts(input);
        assertThat(simpleNameContaining("Par")).accepts(input);
        assertThat(simpleNameContaining("a")).accepts(input);
        assertThat(simpleNameContaining("b")).rejects(input);
        assertThat(simpleNameContaining("A")).rejects(input);
        assertThat(simpleNameContaining("ent")).accepts(input);
        assertThat(simpleNameContaining("Pent")).rejects(input);
        assertThat(simpleNameContaining("aren")).accepts(input);
        assertThat(simpleNameContaining("")).accepts(input);

        // Full match test
        assertThat(simpleNameContaining(input.getName())).rejects(input);
        assertThat(simpleNameContaining(" ")).rejects(input);
        assertThat(simpleNameContaining(".")).rejects(input);

        assertThat(simpleNameContaining(".Parent")).rejects(input);

        assertThat(simpleNameContaining("Infix")).hasDescription("simple name containing 'Infix'");
    }

    @Test
    public void predicate_simpleNameEndingWith() {
        JavaClass input = importClassWithContext(Parent.class);

        assertThat(simpleNameEndingWith("Parent")).accepts(input);
        assertThat(simpleNameEndingWith("ent")).accepts(input);
        assertThat(simpleNameEndingWith("")).accepts(input);

        // Full match test
        assertThat(simpleNameEndingWith(input.getName())).rejects(input);
        assertThat(simpleNameEndingWith(" ")).rejects(input);
        assertThat(simpleNameEndingWith(".")).rejects(input);

        assertThat(simpleNameEndingWith(".Parent")).rejects(input);

        assertThat(simpleNameEndingWith("Suffix")).hasDescription("simple name ending with 'Suffix'");
    }

    @Test
    public void predicate_assignableFrom() {
        assertThatAssignable().from(SuperclassWithFieldAndMethod.class)
                .to(SuperclassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .to(SuperclassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(SuperclassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .via(SuperclassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(InterfaceWithMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(Parent.class)
                .to(InterfaceWithMethod.class)
                .isFalse();
        assertThatAssignable().from(SuperclassWithFieldAndMethod.class)
                .to(Parent.class)
                .isTrue();
        assertThatAssignable().from(SuperclassWithFieldAndMethod.class)
                .to(ClassWithTwoFieldsAndTwoMethods.class)
                .isFalse();
        assertThatAssignable().from(Parent.class)
                .to(SuperclassWithFieldAndMethod.class)
                .isFalse();

        assertThat(assignableFrom(System.class)).hasDescription("assignable from java.lang.System");
    }

    @Test
    public void predicate_assignableTo() {
        assertThatAssignable().to(SuperclassWithFieldAndMethod.class)
                .from(SuperclassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(ClassWithTwoFieldsAndTwoMethods.class)
                .from(SuperclassWithFieldAndMethod.class)
                .isFalse();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(SuperclassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .via(SuperclassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperclassWithFieldAndMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperclassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(Parent.class)
                .from(SuperclassWithFieldAndMethod.class)
                .isTrue();

        assertThat(assignableTo(System.class)).hasDescription("assignable to java.lang.System");
    }

    @DataProvider
    public static Object[][] implement_match_cases() {
        return testForEach(
                implement(List.class),
                implement(Collection.class),
                implement(List.class.getName()),
                implement(Collection.class.getName()),
                implement(name(List.class.getName())),
                implement(name(Collection.class.getName())));
    }

    @Test
    @UseDataProvider("implement_match_cases")
    public void predicate_implement_matches(DescribedPredicate<JavaClass> expectedMatch) {
        assertThat(expectedMatch).accepts(classWithHierarchy(ArrayList.class));
    }

    @Test
    public void implement_rejects_non_interface_types() {
        implement(Serializable.class);

        expectInvalidSyntaxUsageForClassInsteadOfInterface(AbstractList.class, () -> implement(AbstractList.class));
    }

    public static void expectInvalidSyntaxUsageForClassInsteadOfInterface(Class<?> nonInterface, ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(InvalidSyntaxUsageException.class)
                .hasMessageContaining(nonInterface.getName())
                .hasMessageContaining("interface");
    }

    @DataProvider
    public static Object[][] implement_mismatch_cases() {
        return testForEach(
                implement(HasDescription.class),
                implement(Set.class),
                implement(HasDescription.class.getName()),
                implement(Set.class.getName()),
                implement(String.class.getName()),
                implement(name(HasDescription.class.getName())),
                implement(name(Set.class.getName())),
                implement(name(String.class.getName())));
    }

    @Test
    @UseDataProvider("implement_mismatch_cases")
    public void predicate_implement_mismatches(DescribedPredicate<JavaClass> expectedMismatch) {
        assertThat(expectedMismatch).rejects(classWithHierarchy(ArrayList.class));
    }

    @Test
    public void predicate_implement_descriptions() {
        assertThat(implement(List.class))
                .hasDescription("implement " + List.class.getName());
        assertThat(implement(List.class.getName()))
                .hasDescription("implement " + List.class.getName());
        assertThat(implement(DescribedPredicate.<JavaClass>alwaysTrue().as("custom")))
                .hasDescription("implement custom");
    }

    @Test
    public void predicate_interfaces() {
        assertThat(INTERFACES)
                .accepts(importClassWithContext(Serializable.class))
                .rejects(importClassWithContext(Object.class))
                .hasDescription("interfaces");
    }

    @Test
    public void predicate_reside_in_a_package() {
        JavaClass clazz = importClassWithContext(getClass());

        assertThat(resideInAPackage("com..domain")).accepts(clazz);

        assertThat(resideInAPackage("some..pkg")).rejects(clazz);

        assertThat(resideInAPackage("..any..")).hasDescription("reside in a package '..any..'");
    }

    @Test
    public void predicate_reside_in_any_package() {
        JavaClass clazz = importClassWithContext(getClass());

        assertThat(resideInAnyPackage("any.thing", "com..domain")).accepts(clazz);

        assertThat(resideInAnyPackage("any.thing", "some..pkg")).rejects(clazz);

        assertThat(resideInAnyPackage("any.thing", "..any..")).hasDescription("reside in any package ['any.thing', '..any..']");
    }

    @Test
    public void predicate_equivalentTo() {
        JavaClass javaClass = importClasses(SuperclassWithFieldAndMethod.class, Parent.class).get(SuperclassWithFieldAndMethod.class);

        assertThat(equivalentTo(SuperclassWithFieldAndMethod.class))
                .accepts(javaClass);
        assertThat(equivalentTo(Parent.class))
                .rejects(javaClass)
                .hasDescription("equivalent to " + Parent.class.getName());
    }

    @DataProvider
    public static Object[][] data_predicate_belong_to() {
        return testForEach(
                belongToAnyOf(Object.class, ClassWithNamedAndAnonymousInnerClasses.class),
                belongTo(describe(
                        String.format("any of [%s, %s]", Object.class.getName(), ClassWithNamedAndAnonymousInnerClasses.class.getName()),
                        javaClass -> javaClass.isEquivalentTo(Object.class) || javaClass.isEquivalentTo(ClassWithNamedAndAnonymousInnerClasses.class)))
        );
    }

    @Test
    @UseDataProvider
    public void test_predicate_belong_to(DescribedPredicate<JavaClass> belongToPredicate) {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(getClass());
        JavaClass outerAnonymous =
                getOnlyClassSettingField(classes, ClassWithNamedAndAnonymousInnerClasses.name_of_fieldIndicatingOuterAnonymousInnerClass);
        JavaClass nestedAnonymous =
                getOnlyClassSettingField(classes, ClassWithNamedAndAnonymousInnerClasses.name_of_fieldIndicatingNestedAnonymousInnerClass);

        assertThat(belongToPredicate)
                .hasDescription(String.format("belong to any of [%s, %s]",
                        Object.class.getName(), ClassWithNamedAndAnonymousInnerClasses.class.getName()))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.class))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.NamedInnerClass.class))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.NamedInnerClass.NestedNamedInnerClass.class))
                .accepts(outerAnonymous)
                .accepts(nestedAnonymous)
                .rejects(classes.get(getClass()));
    }

    @Test
    public void predicate_containAnyMembersThat() {
        @SuppressWarnings("unused")
        class Match {
            String right;
        }
        @SuppressWarnings("unused")
        class Mismatch {
            String wrong;
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Match.class, Mismatch.class);

        assertThat(containAnyMembersThat(name("right")))
                .hasDescription("contain any members that name 'right'")
                .accepts(classes.get(Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    @Test
    public void predicate_containAnyFieldsThat() {
        @SuppressWarnings("unused")
        class Match {
            String right;
        }
        @SuppressWarnings("unused")
        class Mismatch {
            String wrong;
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Match.class, Mismatch.class);

        assertThat(containAnyFieldsThat(name("right")))
                .hasDescription("contain any fields that name 'right'")
                .accepts(classes.get(Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    @Test
    public void predicate_containAnyCodeUnitsThat() {
        @SuppressWarnings("unused")
        class Match {
            void right() {
            }
        }
        @SuppressWarnings("unused")
        class Mismatch {
            void wrong() {
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Match.class, Mismatch.class);

        assertThat(containAnyCodeUnitsThat(name("right")))
                .hasDescription("contain any code units that name 'right'")
                .accepts(classes.get(Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    @Test
    public void predicate_containAnyMethodsThat() {
        @SuppressWarnings("unused")
        class Match {
            void right() {
            }
        }
        @SuppressWarnings("unused")
        class Mismatch {
            void wrong() {
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Match.class, Mismatch.class);

        assertThat(containAnyMethodsThat(name("right")))
                .hasDescription("contain any methods that name 'right'")
                .accepts(classes.get(Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    @Test
    public void predicate_containAnyConstructorsThat() {
        @SuppressWarnings("unused")
        class Match {
            Match(Serializable param) {
            }
        }
        @SuppressWarnings("unused")
        class Mismatch {
            Mismatch(String param) {
            }
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Match.class, Mismatch.class);

        String regex = ".*" + Serializable.class.getSimpleName() + ".*";
        assertThat(containAnyConstructorsThat(fullNameMatching(regex)))
                .hasDescription("contain any constructors that full name matching '" + regex + "'")
                .accepts(classes.get(Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    private static class Data_of_predicate_containAnyStaticInitializersThat {
        @SuppressWarnings("unused")
        static class Match {
            static {
                System.out.println("static initializer");
            }
        }
    }

    @Test
    public void predicate_containAnyStaticInitializersThat() {
        @SuppressWarnings("unused")
        class Mismatch {
        }
        JavaClasses classes = new ClassFileImporter().importClasses(Data_of_predicate_containAnyStaticInitializersThat.Match.class, Mismatch.class);

        assertThat(containAnyStaticInitializersThat(DescribedPredicate.<JavaMember>alwaysTrue()))
                .hasDescription("contain any static initializers that always true")
                .accepts(classes.get(Data_of_predicate_containAnyStaticInitializersThat.Match.class))
                .rejects(classes.get(Mismatch.class));
    }

    private <T extends JavaMember> Set<T> excludeJavaLangObject(Set<T> members) {
        return members.stream().filter(it -> !it.getOwner().isEquivalentTo(Object.class)).collect(toSet());
    }

    private JavaClass getOnlyClassSettingField(JavaClasses classes, String fieldName) {
        return getOnlyElement(classes.that(new DescribedPredicate<JavaClass>("") {
            @Override
            public boolean test(JavaClass input) {
                return input.getFieldAccessesFromSelf().stream()
                        .anyMatch(access -> access.getTarget().getName().equals(fieldName) && access.getAccessType() == SET);
            }
        }));
    }

    private JavaClass classWithHierarchy(Class<?> clazz) {
        Set<Class<?>> classesToImport = getHierarchy(clazz);
        return importClasses(classesToImport.toArray(new Class<?>[0])).get(clazz);
    }

    private static DependencyConditionCreation callDependency() {
        return new DependencyConditionCreation("calls");
    }

    private static DependencyConditionCreation setFieldDependency() {
        return new DependencyConditionCreation("sets");
    }

    private static DependencyConditionCreation implementsDependency() {
        return new DependencyConditionCreation("implements");
    }

    private static DependencyConditionCreation extendsDependency() {
        return new DependencyConditionCreation("extends");
    }

    private static DependencyConditionCreation fieldTypeDependency() {
        return new DependencyConditionCreation("has type");
    }

    private static DependencyConditionCreation parameterTypeDependency() {
        return new DependencyConditionCreation("has parameter of type");
    }

    private static DependencyConditionCreation methodReturnTypeDependency() {
        return new DependencyConditionCreation("has return type");
    }

    private static DependencyConditionCreation referenceDependency() {
        return new DependencyConditionCreation("references");
    }

    private static DependencyConditionCreation methodThrowsDeclarationDependency() {
        return new DependencyConditionCreation("throws type");
    }

    private static DependencyConditionCreation methodChecksInstanceOfDependency() {
        return new DependencyConditionCreation("checks instanceof");
    }

    private static DependencyConditionCreation annotationTypeDependency() {
        return new DependencyConditionCreation("is annotated with");
    }

    private static DependencyConditionCreation annotationMemberOfTypeDependency() {
        return new DependencyConditionCreation("has annotation member of type");
    }

    private static AnyDependencyConditionCreation anyDependency() {
        return new AnyDependencyConditionCreation();
    }

    private static class AnyDependencyConditionCreation {
        Condition<Dependency> toPrimitives() {
            return new Condition<Dependency>("any dependency to primitives") {
                @Override
                public boolean matches(Dependency value) {
                    return value.getTargetClass().isPrimitive();
                }
            };
        }

        Condition<Dependency> toClassEquivalentTo(Class<?> clazz) {
            return new Condition<Dependency>("any dependency to class " + clazz.getName()) {
                @Override
                public boolean matches(Dependency value) {
                    return value.getTargetClass().isEquivalentTo(clazz);
                }
            };
        }

        Condition<Dependency> fromClassEquivalentTo(Class<?> clazz) {
            return new Condition<Dependency>("any dependency from class " + clazz.getName()) {
                @Override
                public boolean matches(Dependency value) {
                    return value.getOriginClass().isEquivalentTo(clazz);
                }
            };
        }
    }

    static class DependencyConditionCreation {
        private final String descriptionPart;

        DependencyConditionCreation(String descriptionPart) {
            this.descriptionPart = descriptionPart;
        }

        Step2 from(Class<?> origin) {
            return new Step2(origin);
        }

        class Step2 {
            private final Class<?> origin;
            private final String originDescription;

            Step2(Class<?> origin) {
                this.origin = origin;
                originDescription = origin.getName();
            }

            Step3 to(Class<?> target) {
                return new Step3(target);
            }

            Step3 to(Class<?> target, String targetName) {
                return new Step3(target, targetName);
            }

            class Step3 {
                private final Class<?> target;
                private final String targetDescription;

                Step3(Class<?> target) {
                    this.target = target;
                    targetDescription = target.getName();
                }

                Step3(Class<?> target, String targetName) {
                    this.target = target;
                    targetDescription = target.getSimpleName() + "." + targetName;
                }

                Condition<Dependency> inLineNumber(int lineNumber) {
                    return new Condition<Dependency>(String.format(
                            "%s %s %s in line %d", originDescription, descriptionPart, targetDescription, lineNumber)) {
                        @Override
                        public boolean matches(Dependency value) {
                            return value.getOriginClass().isEquivalentTo(origin) &&
                                    value.getTargetClass().isEquivalentTo(target) &&
                                    value.getDescription().matches(String.format(".*%s.*%s.*%s.*:%d.*",
                                            quote(origin.getSimpleName()), quote(descriptionPart), quote(targetDescription), lineNumber));
                        }
                    };
                }
            }
        }
    }

    private void assertIllegalArgumentException(String expectedMessagePart, Runnable runnable) {
        try {
            runnable.run();
            Assert.fail("Should have thrown an " + IllegalArgumentException.class.getSimpleName());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .as("Messagee of %s", IllegalArgumentException.class.getSimpleName())
                    .contains(expectedMessagePart);
        }
    }

    private Function<JavaMember, String> memberIdentifier() {
        return input -> input.getOwner().getSimpleName() + "#" + input.getName();
    }

    private Condition<JavaMember> isNotObject() {
        return new Condition<JavaMember>() {
            @Override
            public boolean matches(JavaMember value) {
                return !value.getOwner().isEquivalentTo(Object.class);
            }
        };
    }

    private static AssignableAssert assertThatAssignable() {
        return new AssignableAssert();
    }

    private static class AssignableAssert {
        private String message;
        private Set<DescribedPredicate<JavaClass>> assignable = new HashSet<>();
        private Class<?> firstType;

        public FromEvaluation from(Class<?> type) {
            firstType = type;
            message = String.format("assignableFrom(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable from") {
                @Override
                public boolean test(JavaClass input) {
                    return input.isAssignableFrom(type) && input.isAssignableFrom(type.getName());
                }
            }, assignableFrom(type), assignableFrom(type.getName()));
            return new FromEvaluation();
        }

        public ToEvaluation to(Class<?> type) {
            firstType = type;
            message = String.format("assignableTo(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable to") {
                @Override
                public boolean test(JavaClass input) {
                    return input.isAssignableTo(type) && input.isAssignableTo(type.getName());
                }
            }, assignableTo(type), assignableTo(type.getName()));
            return new ToEvaluation();
        }

        private class FromEvaluation extends Evaluation<FromEvaluation> {
            public FromEvaluation to(Class<?> toType) {
                return evaluationToType(toType);
            }
        }

        private class ToEvaluation extends Evaluation<ToEvaluation> {
            public ToEvaluation from(Class<?> fromType) {
                return evaluationToType(fromType);
            }
        }

        private class Evaluation<SELF> {
            private final List<AbstractBooleanAssert<?>> assignableAssertion = new ArrayList<>();

            private final Set<Class<?>> additionalTypes = new HashSet<>();

            // NOTE: We need all the classes in the context to create realistic hierarchies
            SELF via(Class<?> type) {
                additionalTypes.add(type);
                return self();
            }

            @SuppressWarnings("unchecked")
            private SELF self() {
                return (SELF) this;
            }

            SELF evaluationToType(Class<?> secondType) {
                Class<?>[] types = ImmutableSet.<Class<?>>builder()
                        .addAll(additionalTypes).add(firstType).add(secondType)
                        .build().toArray(new Class<?>[0]);
                JavaClass javaClass = importClassesWithContext(types).get(secondType);
                for (DescribedPredicate<JavaClass> predicate : assignable) {
                    assignableAssertion.add(assertThat(predicate.test(javaClass))
                            .as(message + secondType.getSimpleName()));
                }
                return self();
            }

            public void isTrue() {
                for (AbstractBooleanAssert<?> assertion : assignableAssertion) {
                    assertion.isTrue();
                }
            }

            public void isFalse() {
                for (AbstractBooleanAssert<?> assertion : assignableAssertion) {
                    assertion.isFalse();
                }
            }
        }
    }

    @SuppressWarnings("unused")
    static class ClassWithTwoFieldsAndTwoMethods extends SuperclassWithFieldAndMethod {
        String stringField;
        private int intField;

        void voidMethod() {
        }

        protected String stringMethod() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    abstract static class SuperclassWithFieldAndMethod extends Parent implements InterfaceWithMethod {
        private Object objectField;

        @Override
        public Object objectMethod() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    interface InterfaceWithMethod {
        Object objectMethod();
    }

    @SomeAnnotation
    abstract static class Parent {
    }

    @SuppressWarnings("unused")
    static class ParentOfClassWithSeveralConstructorsFieldsAndMethods {
        ParentOfClassWithSeveralConstructorsFieldsAndMethods() {
        }

        ParentOfClassWithSeveralConstructorsFieldsAndMethods(Object anyParam) {
        }
    }

    @SuppressWarnings("unused")
    static class ClassWithSeveralConstructorsFieldsAndMethods extends ParentOfClassWithSeveralConstructorsFieldsAndMethods {
        String stringField;
        private int intField;

        static {
            System.out.println("static initializer");
        }

        private ClassWithSeveralConstructorsFieldsAndMethods() {
        }

        ClassWithSeveralConstructorsFieldsAndMethods(String string) {
        }

        public ClassWithSeveralConstructorsFieldsAndMethods(int number, Object[] objects) {
        }

        void voidMethod() {
        }

        protected String stringMethod() {
            return null;
        }
    }

    static class ClassWithInnerClass {
        @SuppressWarnings("InnerClassMayBeStatic")
        class Inner {
        }
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithTypeParameter<T> {
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithTwoTypeParameters<A, B> {
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    private static class ParentWithFieldAndMethod implements InterfaceWithFieldAndMethod {
        static class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "ParentWithFieldAndMethod#parentField",
                    "ParentWithFieldAndMethod#parentMethod",
                    "ParentWithFieldAndMethod#" + CONSTRUCTOR_NAME);
        }

        Object parentField;

        ParentWithFieldAndMethod(Object parentField) {
            this.parentField = parentField;
        }

        @Override
        public void parentMethod() {
        }
    }

    @SuppressWarnings("unused")
    private static class ChildWithFieldAndMethod extends ParentWithFieldAndMethod {
        static class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "ChildWithFieldAndMethod#childField",
                    "ChildWithFieldAndMethod#childMethod",
                    "ChildWithFieldAndMethod#" + CONSTRUCTOR_NAME);
        }

        Object childField;

        ChildWithFieldAndMethod(Object childField) {
            super(childField);
            this.childField = childField;
        }

        void childMethod(String param) {
        }
    }

    @SuppressWarnings("unused")
    private interface InterfaceWithFieldAndMethod {
        class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "InterfaceWithFieldAndMethod#interfaceField",
                    "InterfaceWithFieldAndMethod#parentMethod");
        }

        String interfaceField = "foo";

        void parentMethod();
    }

    @SuppressWarnings("unused")
    private static class ClassWithNamedAndAnonymousInnerClasses {
        static final String name_of_fieldIndicatingOuterAnonymousInnerClass = "fieldIndicatingOuterAnonymousInnerClass";
        static final String name_of_fieldIndicatingNestedAnonymousInnerClass = "fieldIndicatingNestedAnonymousInnerClass";

        private Object fieldIndicatingOuterAnonymousInnerClass;
        private Object fieldIndicatingNestedAnonymousInnerClass;

        void createAnonymousClasses() {
            new Runnable() {
                @Override
                public void run() {
                    fieldIndicatingOuterAnonymousInnerClass = "set";
                    new Runnable() {
                        @Override
                        public void run() {
                            fieldIndicatingNestedAnonymousInnerClass = "set";
                        }
                    };
                }
            };
        }

        @SuppressWarnings("InnerClassMayBeStatic")
        private class NamedInnerClass {
            private class NestedNamedInnerClass {
            }
        }
    }

    private static class SomeSuperclass {
    }

    @OnClass
    @WithPrimitive(someInt = 5, someInts = {1, 2}, someString = "test", someStrings = {"test1", "test2"})
    @WithNestedAnnotations(
            outerType = AhavingMembersOfTypeB.class,
            nested = {
                    @WithType(type = B.class)
            },
            typeWithDefaults = @WithOtherType,
            withEnum = @WithEnum(
                    someEnum = SomeEnumAsNestedAnnotationParameter.NESTED_ANNOTATION_PARAMETER,
                    enumArray = {SomeEnumAsAnnotationArrayParameter.ANNOTATION_ARRAY_PARAMETER}
            )
    )
    @MetaAnnotated
    @SuppressWarnings("unused")
    private static class ClassWithAnnotationDependencies extends SomeSuperclass {
        @OnField(SomeEnumAsAnnotationParameter.ANNOTATION_PARAMETER)
        Object field;

        @OnConstructor
        ClassWithAnnotationDependencies(Serializable param) {
        }

        @OnMethod
        List<?> method() {
            return null;
        }

        void method(@OnMethodParam String param) {
        }
    }

    private @interface OnClass {
    }

    private @interface OnField {
        SomeEnumAsAnnotationParameter value();
    }

    private @interface OnConstructor {
    }

    private @interface OnMethod {
    }

    @WithType(type = B.class)
    private @interface OnMethodParam {
    }

    @Retention(RUNTIME)
    private @interface WithType {
        Class<?> type();
    }

    @SuppressWarnings("unused")
    @Retention(RUNTIME)
    private @interface WithOtherType {
        Class<?> typeWithDefault() default SomeTypeAsDefaultParameter.class;
    }

    @SuppressWarnings("unused")
    @Retention(RUNTIME)
    private @interface WithNestedAnnotations {
        Class<?> outerType();

        WithType[] nested();

        WithOtherType typeWithDefaults();

        WithEnum withEnum();

        WithOtherEnum annotationWithDefault() default @WithOtherEnum(
                someEnum = SomeOtherEnumAsAnnotationParameter.OTHER_ANNOTATION_PARAMETER,
                enumArray = {SomeOtherEnumAsAnnotationArrayParameter.OTHER_ANNOTATION_ARRAY_PARAMETER, SomeOtherEnumAsAnnotationArrayParameter.OTHER_ANNOTATION_ARRAY_PARAMETER});
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    private @interface MetaAnnotation {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    private @interface MetaAnnotated {
    }

    @SuppressWarnings("unused")
    private @interface WithEnum {
        SomeEnumAsDefaultParameter enumWithDefault() default SomeEnumAsDefaultParameter.DEFAULT_PARAMETER;

        SomeEnumAsNestedAnnotationParameter someEnum();

        SomeEnumAsAnnotationArrayParameter[] enumArray();
    }

    @SuppressWarnings("unused")
    private @interface WithOtherEnum {
        SomeOtherEnumAsDefaultParameter enumWithDefault() default SomeOtherEnumAsDefaultParameter.OTHER_DEFAULT_PARAMETER;

        SomeOtherEnumAsAnnotationParameter someEnum();

        SomeOtherEnumAsAnnotationArrayParameter[] enumArray();
    }

    @Retention(RUNTIME)
    private @interface WithPrimitive {
        int someInt();

        int[] someInts();

        String someString();

        String[] someStrings();
    }

    private enum SomeEnumAsAnnotationParameter {
        ANNOTATION_PARAMETER
    }

    private enum SomeOtherEnumAsAnnotationParameter {
        OTHER_ANNOTATION_PARAMETER
    }

    private enum SomeEnumAsNestedAnnotationParameter {
        NESTED_ANNOTATION_PARAMETER
    }

    private enum SomeEnumAsDefaultParameter {
        DEFAULT_PARAMETER
    }

    private enum SomeOtherEnumAsDefaultParameter {
        OTHER_DEFAULT_PARAMETER
    }

    private enum SomeEnumAsAnnotationArrayParameter {
        ANNOTATION_ARRAY_PARAMETER
    }

    private enum SomeOtherEnumAsAnnotationArrayParameter {
        OTHER_ANNOTATION_ARRAY_PARAMETER
    }

    private static class SomeTypeAsDefaultParameter {
    }

    @AnnotationWithCyclicAnnotation
    @Retention(RUNTIME)
    private @interface MetaAnnotationWithCyclicAnnotation {
    }

    @AnnotationWithCyclicAnnotation
    @MetaAnnotationWithCyclicAnnotation
    private @interface AnnotationWithCyclicAnnotation {
    }

    @AnnotationWithCyclicAnnotation
    private static class ClassWithCyclicMetaAnnotation {
    }

    @SuppressWarnings("ALL")
    private static class ClassWithSelfReferences extends Exception {
        static {
            ClassWithSelfReferences selfReference = new ClassWithSelfReferences(null, (ClassWithSelfReferences) null);
        }

        ClassWithSelfReferences fieldSelfReference;
        ClassWithSelfReferences[] arrayFieldSelfReference;

        ClassWithSelfReferences() throws ClassWithSelfReferences {
        }

        ClassWithSelfReferences(Object any, ClassWithSelfReferences selfReference) {
        }

        ClassWithSelfReferences(Object any, ClassWithSelfReferences[] selfReference) {
        }

        ClassWithSelfReferences methodReturnTypeSelfReference() {
            return null;
        }

        ClassWithSelfReferences[] methodArrayReturnTypeSelfReference() {
            return null;
        }

        void methodParameterSelfReference(Object any, ClassWithSelfReferences selfReference) {
        }

        void methodArrayParameterSelfReference(Object any, ClassWithSelfReferences[] selfReference) {
        }

        void methodCallSelfReference() {
            ClassWithSelfReferences self = null;
            self.methodParameterSelfReference(null, null);
            ClassWithSelfReferences[] arraySelf = null;
            arraySelf.clone();
        }

        void constructorCallSelfReference() {
            new ClassWithSelfReferences(null, (ClassWithSelfReferences) null);
        }

        void fieldAccessSelfReference() {
            ClassWithSelfReferences self = null;
            self.fieldSelfReference = null;
            self.arrayFieldSelfReference = null;
        }

        @WithType(type = ClassWithSelfReferences.class)
        void annotationSelfReference() {
        }

        void throwableSelfReference() throws ClassWithSelfReferences {
        }
    }
}
