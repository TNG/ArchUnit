package com.tngtech.archunit.core.testexamples.annotationmethodimport;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.testexamples.SomeEnum;

import static com.tngtech.archunit.core.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ClassWithAnnotatedMethods {
    @MethodAnnotationWithEnumAndArrayValue(classes = {Object.class, Serializable.class})
    public ClassWithAnnotatedMethods() {

    }

    @MethodAnnotationWithStringValue("something")
    public Object stringAnnotatedMethod() {
        return null;
    }

    @MethodAnnotationWithStringValue("otherThing")
    @MethodAnnotationWithIntValue(otherValue = "overridden")
    public Object stringAndIntAnnotatedMethod() {
        return null;
    }

    @MethodAnnotationWithEnumAndArrayValue(classes = {Object.class, Serializable.class})
    public Object enumAndArrayAnnotatedMethod() {
        return null;
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithStringValue {
        String value();
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithIntValue {
        int intValue() default 0;

        String otherValue() default "Nothing";
    }

    @Target({METHOD, CONSTRUCTOR})
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithEnumAndArrayValue {
        SomeEnum value() default SOME_VALUE;

        Class[] classes();
    }
}
