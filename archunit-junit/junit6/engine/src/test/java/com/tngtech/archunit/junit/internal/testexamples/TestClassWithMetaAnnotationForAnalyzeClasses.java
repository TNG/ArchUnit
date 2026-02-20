package com.tngtech.archunit.junit.internal.testexamples;

import java.lang.annotation.Retention;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@TestClassWithMetaAnnotationForAnalyzeClasses.MetaAnalyzeClasses
public class TestClassWithMetaAnnotationForAnalyzeClasses {

  @ArchTest
  public static final ArchRule rule_in_class_with_meta_analyze_class_annotation = RuleThatFails.on(UnwantedClass.class);

  @Retention(RUNTIME)
  @AnalyzeClasses(wholeClasspath = true)
  public @interface MetaAnalyzeClasses {
  }
}
