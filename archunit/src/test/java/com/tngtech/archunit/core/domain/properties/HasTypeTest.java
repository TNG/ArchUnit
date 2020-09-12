package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class HasTypeTest {
    @DataProvider
    public static Object[][] type_predicates() {
        return testForEach(
                HasType.Predicates.rawType(String.class),
                HasType.Predicates.rawType(String.class.getName()),
                HasType.Predicates.rawType(equivalentTo(String.class)));
    }

    @Test
    @UseDataProvider("type_predicates")
    public void predicate_type(DescribedPredicate<HasType> predicate) {
        HasType matchingField = newHasType(String.class);
        HasType nonmatchingField = newHasType(Object.class);

        assertThat(predicate)
                .accepts(matchingField)
                .rejects(nonmatchingField);
    }

    @Test
    public void predicate_description() {
        assertThat(HasType.Predicates.rawType(String.class)).hasDescription("raw type " + String.class.getName());
        assertThat(HasType.Predicates.rawType(String.class.getName())).hasDescription("raw type " + String.class.getName());
        assertThat(HasType.Predicates.rawType(equivalentTo(String.class)))
                .hasDescription("raw type equivalent to " + String.class.getName());
    }

    @Test
    public void function_getType() {
        assertThatType(GET_RAW_TYPE.apply(newHasType(String.class))).matches(String.class);
    }

    private HasType newHasType(final Class<?> owner) {
        return new HasType() {

            @Override
            public JavaType getType() {
                return getRawType();
            }

            @Override
            public JavaClass getRawType() {
                return importClassWithContext(owner);
            }
        };
    }
}
