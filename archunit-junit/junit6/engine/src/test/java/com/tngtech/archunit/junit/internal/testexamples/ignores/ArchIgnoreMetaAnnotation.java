package com.tngtech.archunit.junit.internal.testexamples.ignores;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.junit.ArchIgnore;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
@ArchIgnore(reason = "some example description")
public @interface ArchIgnoreMetaAnnotation {
}
