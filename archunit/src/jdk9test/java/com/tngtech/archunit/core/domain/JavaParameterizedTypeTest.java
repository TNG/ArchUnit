package com.tngtech.archunit.core.domain;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JavaParameterizedTypeTest {

    @DataProvider
    @SuppressWarnings("unused")
    public static Object[][] parameterized_types() {
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
        ).map(JavaParameterizedTypeTest::createTestInput).toArray(Object[][]::new);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static Object[] createTestInput(Class<?> testClass) {
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

    @Test
    @UseDataProvider("parameterized_types")
    public void name_of_parameterized_type_matches_Reflection_API(JavaType javaType, Type reflectionType) {
        assertThat(javaType.getName()).isEqualTo(reflectionType.getTypeName());
    }
}
