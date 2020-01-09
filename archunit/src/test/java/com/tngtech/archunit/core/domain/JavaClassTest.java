package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.InvalidSyntaxUsageException;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.testobjects.AAccessingB;
import com.tngtech.archunit.core.domain.testobjects.AExtendingSuperAImplementingInterfaceForA;
import com.tngtech.archunit.core.domain.testobjects.AhavingMembersOfTypeB;
import com.tngtech.archunit.core.domain.testobjects.AllPrimitiveDependencies;
import com.tngtech.archunit.core.domain.testobjects.B;
import com.tngtech.archunit.core.domain.testobjects.InterfaceForA;
import com.tngtech.archunit.core.domain.testobjects.IsArrayTestClass;
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
import org.assertj.core.api.iterable.Extractor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_ORIGIN_CLASS;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CODE_UNITS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_CONSTRUCTORS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_FIELDS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_MEMBERS;
import static com.tngtech.archunit.core.domain.JavaClass.Functions.GET_METHODS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
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
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importPackagesOf;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.getHierarchy;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class JavaClassTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void finds_array_type() {
        JavaMethod method = importClassWithContext(IsArrayTestClass.class).getMethod("anArray");

        assertThat(method.getRawReturnType().isArray()).isTrue();
        assertThat(method.getRawReturnType().tryGetComponentType().get()).matches(Object.class);
    }

    @Test
    public void finds_non_array_type() {
        JavaMethod method = importClassWithContext(IsArrayTestClass.class).getMethod("notAnArray");

        assertThat(method.getRawReturnType().isArray()).isFalse();
        assertThat(method.getRawReturnType().tryGetComponentType()).isAbsent();
    }

    @Test
    public void finds_multidimensional_array_type() {
        JavaClasses classes = importClassesWithContext(ClassUsedInArray.class, ClassAccessingOneDimensionalArray.class, ClassAccessingTwoDimensionalArray.class);
        JavaClass type = classes.get(ClassUsedInArray.class);
        JavaClass oneDimArray = classes.get(ClassAccessingOneDimensionalArray.class).getField("array").getRawType();
        JavaClass twoDimArray = classes.get(ClassAccessingTwoDimensionalArray.class).getField("array").getRawType();

        assertThat(oneDimArray.isArray()).isTrue();
        assertThat(oneDimArray.tryGetComponentType().get()).isEqualTo(type);
        assertThat(twoDimArray.isArray()).isTrue();
        assertThat(twoDimArray.tryGetComponentType().get()).isEqualTo(oneDimArray);
        assertThat(twoDimArray.tryGetComponentType().get().tryGetComponentType().get()).isEqualTo(type);
    }

    @Test
    public void finds_component_type_chain_of_otherwise_unreferenced_component_type() {
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
        assertThat(original).isEqualTo(javaClass);
    }

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = importClassWithContext(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(javaClass.reflect()).isEqualTo(ClassWithTwoFieldsAndTwoMethods.class);
        assertThat(javaClass.getFields()).hasSize(2);
        assertThat(javaClass.getMethods()).hasSize(2);

        for (JavaField field : javaClass.getFields()) {
            assertThat(field.getOwner()).isSameAs(javaClass);
        }
        for (JavaCodeUnit method : javaClass.getCodeUnits()) {
            assertThat(method.getOwner()).isSameAs(javaClass);
        }
    }

    @Test
    public void finds_constructors() {
        JavaClass javaClass = importClassWithContext(ClassWithSeveralConstructorsFieldsAndMethods.class);

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));
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
        JavaClass clazz = importClassesWithContext(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllSuperClasses()).extracting("name").containsExactly(
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = importClassesWithContext(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).extracting("name").containsExactly(
                clazz.getName(),
                SuperClassWithFieldAndMethod.class.getName(),
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
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(importClassWithContext(Parent.class)
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void isMetaAnnotatedWith_type() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz.isMetaAnnotatedWith(SomeAnnotation.class))
                .as("Parent is meta-annotated with @" + SomeAnnotation.class.getSimpleName()).isFalse();
        assertThat(clazz.isMetaAnnotatedWith(Retention.class))
                .as("Parent is meta-annotated with @" + Retention.class.getSimpleName()).isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_typeName() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz.isMetaAnnotatedWith(SomeAnnotation.class.getName()))
                .as("Parent is meta-annotated with @" + SomeAnnotation.class.getSimpleName()).isFalse();
        assertThat(clazz.isMetaAnnotatedWith(Retention.class.getName()))
                .as("Parent is meta-annotated with @" + Retention.class.getSimpleName()).isTrue();
    }

    @Test
    public void isMetaAnnotatedWith_predicate() {
        JavaClass clazz = importClassesWithContext(Parent.class, SomeAnnotation.class).get(Parent.class);

        assertThat(clazz
                .isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(clazz
                .isMetaAnnotatedWith(DescribedPredicate.<JavaAnnotation<?>>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = importClasses(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = importClassWithContext(Object.class);
        simulateCall().from(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCall().from(javaClass.getSuperClass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginOwner").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginOwner")
                .contains(javaClass, javaClass.getSuperClass().get());
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
                .containsOnlyElementsOf(ChildWithFieldAndMethod.Members.MEMBERS);

        assertThat(clazz.getAllMembers())
                .filteredOn(isNotObject())
                .extracting(memberIdentifier())
                .containsOnlyElementsOf(ImmutableSet.<String>builder()
                        .addAll(ChildWithFieldAndMethod.Members.MEMBERS)
                        .addAll(ParentWithFieldAndMethod.Members.MEMBERS)
                        .addAll(InterfaceWithFieldAndMethod.Members.MEMBERS)
                        .build());
    }

    @Test
    public void getCodeUnitWithName() {
        final JavaClass clazz = importClasses(ChildWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertIllegalArgumentException("childMethod", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("childMethod");
            }
        });
        assertIllegalArgumentException("childMethod", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("childMethod", Object.class);
            }
        });
        assertIllegalArgumentException("wrong", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("wrong", String.class);
            }
        });

        assertThat(clazz.getCodeUnitWithParameterTypes("childMethod", String.class))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, "childMethod", String.class));
        assertThat(clazz.getCodeUnitWithParameterTypeNames("childMethod", String.class.getName()))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, "childMethod", String.class));
        assertThat(clazz.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, Object.class))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, CONSTRUCTOR_NAME, Object.class));
        assertThat(clazz.getCodeUnitWithParameterTypeNames(CONSTRUCTOR_NAME, Object.class.getName()))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, CONSTRUCTOR_NAME, Object.class));
    }

    private Condition<JavaCodeUnit> equivalentCodeUnit(final Class<?> owner, final String methodName, final Class<?> paramType) {
        return new Condition<JavaCodeUnit>() {
            @Override
            public boolean matches(JavaCodeUnit value) {
                return value.getOwner().isEquivalentTo(owner) &&
                        value.getName().equals(methodName) &&
                        value.getRawParameterTypes().getNames().equals(ImmutableList.of(paramType.getName()));
            }
        };
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
    public void direct_dependencies_from_self_by_member_declarations() {
        JavaClass javaClass = importClasses(AhavingMembersOfTypeB.class, B.class).get(AhavingMembersOfTypeB.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .areAtLeastOne(fieldTypeDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.class)
                        .inLineNumber(0))
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
                        .inLineNumber(0));
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
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(WithType.class)
                        .inLineNumber(0))
                .areAtLeastOne(annotationMemberOfTypeDependency()
                        .from(ClassWithAnnotationDependencies.class)
                        .to(B.class)
                        .inLineNumber(0))
        ;
    }

    @Test
    public void direct_dependencies_from_self_finds_correct_set_of_target_types() {
        JavaClass javaClass = importPackagesOf(getClass()).get(ClassWithAnnotationDependencies.class);

        Set<JavaClass> targets = FluentIterable.from(javaClass.getDirectDependenciesFromSelf())
                .transform(toGuava(GET_TARGET_CLASS)).toSet();

        assertThatClasses(targets).matchInAnyOrder(
                B.class, AhavingMembersOfTypeB.class, Object.class, String.class,
                List.class, Serializable.class, SomeSuperClass.class,
                WithType.class, WithNestedAnnotations.class, OnClass.class, OnMethod.class,
                OnConstructor.class, OnField.class, MetaAnnotated.class, WithEnum.class, WithPrimitive.class,
                SomeEnumAsAnnotationParameter.class, SomeEnumAsAnnotationArrayParameter.class,
                SomeEnumAsNestedAnnotationParameter.class, SomeEnumAsDefaultParameter.class);
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
        JavaClass superClass = importClassesWithContext(SuperA.class, AExtendingSuperAImplementingInterfaceForA.class).get(SuperA.class);

        assertThat(superClass.getDirectDependenciesToSelf())
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
                        .inLineNumber(0));

        JavaClass exceptionClass = importClassesWithContext(AhavingMembersOfTypeB.class, B.BException.class)
                .get(B.BException.class);

        assertThat(exceptionClass.getDirectDependenciesToSelf())
                .areAtLeastOne(methodThrowsDeclarationDependency()
                        .from(AhavingMembersOfTypeB.class)
                        .to(B.BException.class)
                        .inLineNumber(0));
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
    public void direct_dependencies_to_self_finds_correct_set_of_origin_types() {
        JavaClasses classes = importPackagesOf(getClass());

        Set<JavaClass> origins = getOriginsOfDependenciesTo(classes.get(WithType.class));

        assertThatClasses(origins).matchInAnyOrder(ClassWithAnnotationDependencies.class, ClassWithSelfReferences.class, OnMethodParam.class);

        origins = getOriginsOfDependenciesTo(classes.get(B.class));

        assertThatClasses(origins).matchInAnyOrder(
                ClassWithAnnotationDependencies.class, OnMethodParam.class, AAccessingB.class, AhavingMembersOfTypeB.class);

        origins = getOriginsOfDependenciesTo(classes.get(SomeEnumAsNestedAnnotationParameter.class));

        assertThatClasses(origins).matchInAnyOrder(
                ClassWithAnnotationDependencies.class, WithEnum.class);
    }

    private Set<JavaClass> getOriginsOfDependenciesTo(JavaClass withType) {
        return FluentIterable.from(withType.getDirectDependenciesToSelf())
                .transform(toGuava(GET_ORIGIN_CLASS)).toSet();
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
    }

    @Test
    public void predicate_withType() {
        assertThat(type(Parent.class))
                .accepts(importClassWithContext(Parent.class))
                .rejects(importClassWithContext(SuperClassWithFieldAndMethod.class));

        assertThat(type(System.class)).hasDescription("type java.lang.System");
    }

    @Test
    public void predicate_simpleName() {
        assertThat(simpleName(Parent.class.getSimpleName()))
                .accepts(importClassWithContext(Parent.class))
                .rejects(importClassWithContext(SuperClassWithFieldAndMethod.class));

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
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .via(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(InterfaceWithMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(Parent.class)
                .to(InterfaceWithMethod.class)
                .isFalse();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(Parent.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(ClassWithTwoFieldsAndTwoMethods.class)
                .isFalse();
        assertThatAssignable().from(Parent.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isFalse();

        assertThat(assignableFrom(System.class)).hasDescription("assignable from java.lang.System");
    }

    @Test
    public void predicate_assignableTo() {
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(ClassWithTwoFieldsAndTwoMethods.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isFalse();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .via(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(Parent.class)
                .from(SuperClassWithFieldAndMethod.class)
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

        expectInvalidSyntaxUsageForClassInsteadOfInterface(thrown, AbstractList.class);
        implement(AbstractList.class);
    }

    public static void expectInvalidSyntaxUsageForClassInsteadOfInterface(ExpectedException thrown, Class<?> nonInterface) {
        thrown.expect(InvalidSyntaxUsageException.class);
        thrown.expectMessage(nonInterface.getName());
        thrown.expectMessage("interface");
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
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInAPackage("some..pkg")).accepts(clazz);

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInAPackage("some..pkg")).rejects(clazz);

        assertThat(resideInAPackage("..any..")).hasDescription("reside in a package '..any..'");
    }

    @Test
    public void predicate_reside_in_any_package() {
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg")).accepts(clazz);

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg")).rejects(clazz);

        assertThat(resideInAnyPackage("any.thing", "..any..")).hasDescription("reside in any package ['any.thing', '..any..']");
    }

    @Test
    public void predicate_equivalentTo() {
        JavaClass javaClass = importClasses(SuperClassWithFieldAndMethod.class, Parent.class).get(SuperClassWithFieldAndMethod.class);

        assertThat(equivalentTo(SuperClassWithFieldAndMethod.class))
                .accepts(javaClass);
        assertThat(equivalentTo(Parent.class))
                .rejects(javaClass)
                .hasDescription("equivalent to " + Parent.class.getName());
    }

    @Test
    public void predicate_belong_to() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(getClass());
        JavaClass outerAnonymous =
                getOnlyClassSettingField(classes, ClassWithNamedAndAnonymousInnerClasses.name_of_fieldIndicatingOuterAnonymousInnerClass);
        JavaClass nestedAnonymous =
                getOnlyClassSettingField(classes, ClassWithNamedAndAnonymousInnerClasses.name_of_fieldIndicatingNestedAnonymousInnerClass);

        assertThat(belongToAnyOf(Object.class, ClassWithNamedAndAnonymousInnerClasses.class))
                .hasDescription(String.format("belong to any of [%s, %s]",
                        Object.class.getName(), ClassWithNamedAndAnonymousInnerClasses.class.getName()))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.class))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.NamedInnerClass.class))
                .accepts(classes.get(ClassWithNamedAndAnonymousInnerClasses.NamedInnerClass.NestedNamedInnerClass.class))
                .accepts(outerAnonymous)
                .accepts(nestedAnonymous)
                .rejects(classes.get(getClass()));
    }

    private JavaClass getOnlyClassSettingField(JavaClasses classes, final String fieldName) {
        return getOnlyElement(classes.that(new DescribedPredicate<JavaClass>("") {
            @Override
            public boolean apply(JavaClass input) {
                for (JavaFieldAccess access : input.getFieldAccessesFromSelf()) {
                    if (access.getTarget().getName().equals(fieldName) && access.getAccessType() == SET) {
                        return true;
                    }
                }
                return false;
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

    private static DependencyConditionCreation methodThrowsDeclarationDependency() {
        return new DependencyConditionCreation("throws type");
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

        Condition<Dependency> toClassEquivalentTo(final Class<?> clazz) {
            return new Condition<Dependency>("any dependency to class " + clazz.getName()) {
                @Override
                public boolean matches(Dependency value) {
                    return value.getTargetClass().isEquivalentTo(clazz);
                }
            };
        }

        Condition<Dependency> fromClassEquivalentTo(final Class<?> clazz) {
            return new Condition<Dependency>("any dependency from class " + clazz.getName()) {
                @Override
                public boolean matches(Dependency value) {
                    return value.getOriginClass().isEquivalentTo(clazz);
                }
            };
        }
    }

    private static class DependencyConditionCreation {
        private final String descriptionPart;

        DependencyConditionCreation(String descriptionPart) {
            this.descriptionPart = descriptionPart;
        }

        Step2 from(Class<?> origin) {
            return new Step2(origin);
        }

        private class Step2 {
            private final Class<?> origin;
            private String originDescription;

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

            private class Step3 {
                private final Class<?> target;
                private final String targetDescription;

                Step3(Class<?> target) {
                    this.target = target;
                    targetDescription = target.getSimpleName();
                }

                Step3(Class<?> target, String targetName) {
                    this.target = target;
                    targetDescription = target.getSimpleName() + "." + targetName;
                }

                Condition<Dependency> inLineNumber(final int lineNumber) {
                    return new Condition<Dependency>(String.format(
                            "%s %s %s in line %d", originDescription, descriptionPart, targetDescription, lineNumber)) {
                        @Override
                        public boolean matches(Dependency value) {
                            return value.getOriginClass().isEquivalentTo(origin) &&
                                    value.getTargetClass().isEquivalentTo(target) &&
                                    value.getDescription().matches(String.format(".*%s.*%s.*%s.*:%d.*",
                                            origin.getSimpleName(), descriptionPart, targetDescription, lineNumber));
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

    private Extractor<JavaMember, String> memberIdentifier() {
        return new Extractor<JavaMember, String>() {
            @Override
            public String extract(JavaMember input) {
                return input.getOwner().getSimpleName() + "#" + input.getName();
            }
        };
    }

    private Condition<JavaMember> isNotObject() {
        return new Condition<JavaMember>() {
            @Override
            public boolean matches(JavaMember value) {
                return !value.getOwner().isEquivalentTo(Object.class);
            }
        };
    }

    private static JavaClass fakeClassWithPackage(String pkg) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getPackageName()).thenReturn(pkg);
        return javaClass;
    }

    private static AssignableAssert assertThatAssignable() {
        return new AssignableAssert();
    }

    private static class AssignableAssert {
        private String message;
        private Set<DescribedPredicate<JavaClass>> assignable = new HashSet<>();
        private Class<?> firstType;

        public FromEvaluation from(final Class<?> type) {
            firstType = type;
            message = String.format("assignableFrom(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable from") {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isAssignableFrom(type) && input.isAssignableFrom(type.getName());
                }
            }, assignableFrom(type), assignableFrom(type.getName()));
            return new FromEvaluation();
        }

        public ToEvaluation to(final Class<?> type) {
            firstType = type;
            message = String.format("assignableTo(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable to") {
                @Override
                public boolean apply(JavaClass input) {
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
            private List<AbstractBooleanAssert<?>> assignableAssertion = new ArrayList<>();

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
                    assignableAssertion.add(assertThat(predicate.apply(javaClass))
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
    static class ClassWithTwoFieldsAndTwoMethods extends SuperClassWithFieldAndMethod {
        String stringField;
        private int intField;

        void voidMethod() {
        }

        protected String stringMethod() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    abstract static class SuperClassWithFieldAndMethod extends Parent implements InterfaceWithMethod {
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
    static class ClassWithSeveralConstructorsFieldsAndMethods {
        String stringField;
        private int intField;

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
        class Inner {
        }
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

        private class NamedInnerClass {
            private class NestedNamedInnerClass {
            }
        }
    }

    private static class SomeSuperClass {
    }

    @OnClass
    @WithPrimitive(someInt = 5, someInts = {1, 2}, someString = "test", someStrings = {"test1", "test2"})
    @WithNestedAnnotations(
            outerType = AhavingMembersOfTypeB.class,
            nested = {
                    @WithType(type = B.class)
            },
            withEnum = @WithEnum(
                    someEnum = SomeEnumAsNestedAnnotationParameter.NESTED_ANNOTATION_PARAMETER,
                    enumArray = {SomeEnumAsAnnotationArrayParameter.ANNOTATION_ARRAY_PARAMETER}
            )
    )
    @MetaAnnotated
    public static class ClassWithAnnotationDependencies extends SomeSuperClass {
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

    @interface OnClass {
    }

    @interface OnField {
        SomeEnumAsAnnotationParameter value();
    }

    @interface OnConstructor {
    }

    @interface OnMethod {
    }

    @WithType(type = B.class)
    @interface OnMethodParam {
    }

    @Retention(RUNTIME)
    @interface WithType {
        Class<?> type();
    }

    @Retention(RUNTIME)
    @interface WithNestedAnnotations {
        Class<?> outerType();

        WithType[] nested();

        WithEnum withEnum();
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    @interface MetaAnnotation {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    @interface MetaAnnotated {
    }

    @interface WithEnum {
        SomeEnumAsDefaultParameter enumWithDefault() default SomeEnumAsDefaultParameter.DEFAULT_PARAMETER;

        SomeEnumAsNestedAnnotationParameter someEnum();

        SomeEnumAsAnnotationArrayParameter[] enumArray();
    }

    @Retention(RUNTIME)
    @interface WithPrimitive {
        int someInt();

        int[] someInts();

        String someString();

        String[] someStrings();
    }

    enum SomeEnumAsAnnotationParameter {
        ANNOTATION_PARAMETER
    }

    enum SomeEnumAsNestedAnnotationParameter {
        NESTED_ANNOTATION_PARAMETER
    }

    enum SomeEnumAsDefaultParameter {
        DEFAULT_PARAMETER
    }

    enum SomeEnumAsAnnotationArrayParameter {
        ANNOTATION_ARRAY_PARAMETER
    }

    @SuppressWarnings("ALL")
    private static class ClassWithSelfReferences extends Exception {
        static {
            ClassWithSelfReferences selfReference = new ClassWithSelfReferences(null, null);
        }

        ClassWithSelfReferences fieldSelfReference;

        ClassWithSelfReferences() throws ClassWithSelfReferences {
        }

        ClassWithSelfReferences(Object any, ClassWithSelfReferences selfReference) {
        }

        ClassWithSelfReferences methodReturnTypeSelfReference() {
            return null;
        }

        void methodParameterSelfReference(Object any, ClassWithSelfReferences selfReference) {
        }

        void methodCallSelfReference() {
            ClassWithSelfReferences self = null;
            self.methodParameterSelfReference(null, null);
        }

        void constructorCallSelfReference() {
            new ClassWithSelfReferences(null, null);
        }

        void fieldAccessSelfReference() {
            ClassWithSelfReferences self = null;
            self.fieldSelfReference = null;
        }

        @WithType(type = ClassWithSelfReferences.class)
        void annotationSelfReference() {
        }

        void throwableSelfReference() throws ClassWithSelfReferences {
        }
    }
}
