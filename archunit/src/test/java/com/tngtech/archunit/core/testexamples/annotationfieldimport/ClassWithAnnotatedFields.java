package com.tngtech.archunit.core.testexamples.annotationfieldimport;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.testexamples.SomeEnum;

import static com.tngtech.archunit.core.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ClassWithAnnotatedFields {
    @FieldAnnotationWithStringValue("something")
    public Object stringAnnotatedField;

    @FieldAnnotationWithStringValue("otherThing")
    @FieldAnnotationWithIntValue(otherValue = "overridden")
    public Object stringAndIntAnnotatedField;

    @FieldAnnotationWithEnumAndArrayValue(classes = {Object.class, Serializable.class})
    public Object enumAndArrayAnnotatedField;

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithStringValue {
        String value();
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithIntValue {
        int intValue() default 0;

        String otherValue() default "Nothing";
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithEnumAndArrayValue {
        SomeEnum value() default SOME_VALUE;

        Class[] classes();
    }

}
