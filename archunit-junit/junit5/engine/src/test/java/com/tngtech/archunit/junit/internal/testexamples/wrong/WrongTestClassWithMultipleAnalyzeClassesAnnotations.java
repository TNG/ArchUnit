package com.tngtech.archunit.junit.internal.testexamples.wrong;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.internal.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.internal.testexamples.UnwantedClass;
import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@AnalyzeClasses(packages = "dummy")
@WrongTestClassWithMultipleAnalyzeClassesAnnotations.MetaAnalyzeCls
public class WrongTestClassWithMultipleAnalyzeClassesAnnotations {

    @ArchTest
    public static final ArchRule dummy_rule = RuleThatFails.on(UnwantedClass.class);

    @Retention(RUNTIME)
    @Target(TYPE)
    @AnalyzeClasses(wholeClasspath = true)
    public @interface MetaAnalyzeCls {
    }
}
