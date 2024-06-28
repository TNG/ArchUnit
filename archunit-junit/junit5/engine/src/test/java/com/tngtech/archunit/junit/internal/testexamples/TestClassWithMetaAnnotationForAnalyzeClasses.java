package com.tngtech.archunit.junit.internal.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@TestClassWithMetaAnnotationForAnalyzeClasses.MetaAnalyzeCls
public class TestClassWithMetaAnnotationForAnalyzeClasses {

  @ArchTest
  public static final ArchRule rule_in_class_with_meta_analyze_class_annotation = RuleThatFails.on(UnwantedClass.class);

  @Retention(RUNTIME)
  @Target(TYPE)
  @AnalyzeClasses(wholeClasspath = true)
  public @interface MetaAnalyzeCls {
  }
}
