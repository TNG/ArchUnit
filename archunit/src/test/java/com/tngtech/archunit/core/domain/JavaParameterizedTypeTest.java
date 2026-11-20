package com.tngtech.archunit.core.domain;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.testexamples.innerclassimport.ClassWithInnerClass;
import com.tngtech.archunit.core.importer.testexamples.innerclassimport.GenericClassWithInnerClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class JavaParameterizedTypeTest {

    @SuppressWarnings("unused")
    static Stream<Arguments> parameterized_types() {
        class WithConcreteTypeName<TEST extends List<String>> {
        }
        class WithTypeVariable<X, TEST extends List<X>> {
        }
        class WithNestedType<TEST extends List<Map<String, File>>> {
        }
        class WithArrays<TEST extends List<Map<String[], int[][]>>> {
        }
        class WithTypeVariableArrays<X, Y, TEST extends List<Map<X[], Y[][]>>> {
        }
        class WithWildcards<TEST extends List<Map<Map<?, ? extends File>, ? super Map<? extends String[][], ? extends int[][]>>>> {
        }
        class WithUnparameterizedOuterAndInnerType<TEST extends ClassWithInnerClass.Inner> {
        }
        class WithUnparameterizedOuterAndGenericInnerType<TEST extends ClassWithInnerClass.GenericInner<String>> {
        }
        class WithUnparameterizedOuterAndNestedStaticType<TEST extends ClassWithInnerClass.NestedStatic> {
        }
        @SuppressWarnings("rawtypes")
        class WithRawUseOuterQualifiedInnerType<TEST extends GenericClassWithInnerClass.Middle> {
        }
        class WithParameterizedOuterMiddleType<TEST extends GenericClassWithInnerClass<String>.Middle> {
        }
        class WithParameterizedOuterAndUnparameterizedMiddleTypeParameterizedInnerType<TEST extends GenericClassWithInnerClass<String>.Middle.GenericInner<String>> {
        }
        class WithParameterizedOuterQualifiedInnerTypeDepth2<TEST extends GenericClassWithInnerClass<Serializable>.GenericMiddle<String>.GenericInner<String>> {
        }
        return Stream.of(
                WithConcreteTypeName.class,
                WithTypeVariable.class,
                WithNestedType.class,
                WithArrays.class,
                WithTypeVariableArrays.class,
                WithWildcards.class,
                WithUnparameterizedOuterAndInnerType.class,
                WithUnparameterizedOuterAndGenericInnerType.class,
                WithUnparameterizedOuterAndNestedStaticType.class,
                WithRawUseOuterQualifiedInnerType.class,
                WithParameterizedOuterMiddleType.class,
                WithParameterizedOuterAndUnparameterizedMiddleTypeParameterizedInnerType.class,
                WithParameterizedOuterQualifiedInnerTypeDepth2.class
        ).map(JavaParameterizedTypeTest::createTestInput);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static Arguments createTestInput(Class<?> testClass) {
        Type reflectionType = Arrays.stream(testClass.getTypeParameters())
                .filter(v -> v.getName().equals("TEST"))
                .map(v -> v.getBounds()[0])
                .findFirst().get();
        JavaType javaType = new ClassFileImporter().importClasses(testClass, ClassWithInnerClass.class, ClassWithInnerClass.GenericInner.class, GenericClassWithInnerClass.class).get(testClass)
                .getTypeParameters().stream()
                .filter(v -> v.getName().equals("TEST"))
                .map(v -> v.getBounds().get(0))
                .findFirst().get();

        return arguments(javaType, reflectionType);
    }

    @ParameterizedTest
    @MethodSource("parameterized_types")
    void name_of_parameterized_type_matches_Reflection_API(JavaType javaType, Type reflectionType) {
        assertThat(javaType.getName()).isEqualTo(reflectionType.getTypeName());
    }

    @ParameterizedTest
    @MethodSource("parameterized_types")
    void erasure_and_enclosing_type_matches_Reflection_API(JavaType javaType, Type reflectionType) {
        assertThatType(javaType).matches(reflectionType);
    }

}
