package com.tngtech.archunit.core;

import java.io.Serializable;

import com.tngtech.archunit.core.TestUtils.AnotherClassWithFieldNamedValue;
import com.tngtech.archunit.core.TestUtils.ClassWithFieldNamedValue;
import com.tngtech.archunit.core.testexamples.SomeClass;
import com.tngtech.archunit.core.testexamples.SomeEnum;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaField;
import static com.tngtech.archunit.core.TestUtils.predicateWithDescription;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaFieldTest {

    @Test(expected = IllegalArgumentException.class)
    public void incompatible_owner_is_rejected() throws Exception {
        new JavaField.Builder()
                .withField(ClassWithFieldNamedValue.class.getDeclaredField("value"))
                .build(javaClass(AnotherClassWithFieldNamedValue.class));
    }

    @Test
    public void hasType_works() {
        assertThat(JavaField.hasType(DescribedPredicate.equalTo(TypeDetails.of(SomeEnum.class)))
                .apply(fieldWithType(SomeEnum.class))).as("Predicate matches").isTrue();
        assertThat(JavaField.hasType(DescribedPredicate.equalTo(TypeDetails.of(Serializable.class)))
                .apply(fieldWithType(SomeEnum.class))).as("Predicate matches").isFalse();

        assertThat(JavaField.hasType(predicateWithDescription("something")).getDescription())
                .isEqualTo("has type something");
    }

    private JavaField fieldWithType(Class<?> type) {
        JavaField result = javaField(SomeClass.class, "other");
        checkState(result.getType().equals(TypeDetails.of(type)), "field doesn't have the type the test expects anymore");
        return result;
    }
}
