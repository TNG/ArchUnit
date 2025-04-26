package com.tngtech.archunit.core.domain;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;

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
        return Stream.of(
                WithConcreteTypeName.class,
                WithTypeVariable.class,
                WithNestedType.class,
                WithArrays.class,
                WithTypeVariableArrays.class,
                WithWildcards.class
        ).map(JavaParameterizedTypeTest::createTestInput);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static Arguments createTestInput(Class<?> testClass) {
        Type reflectionType = Arrays.stream(testClass.getTypeParameters())
                .filter(v -> v.getName().equals("TEST"))
                .map(v -> v.getBounds()[0])
                .findFirst().get();
        JavaType javaType = new ClassFileImporter().importClass(testClass).getTypeParameters().stream()
                .filter(v -> v.getName().equals("TEST"))
                .map(v -> v.getBounds().get(0))
                .findFirst().get();

        return $(javaType, reflectionType);
    }

    @ParameterizedTest
    @MethodSource("parameterized_types")
    void name_of_parameterized_type_matches_Reflection_API(JavaType javaType, Type reflectionType) {
        assertThat(javaType.getName()).isEqualTo(reflectionType.getTypeName());
    }
}
