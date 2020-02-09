package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.junit.ArchIgnore;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
@ArchIgnore(reason = "some example description")
public @interface ArchIgnoreMetaAnnotation {
}
