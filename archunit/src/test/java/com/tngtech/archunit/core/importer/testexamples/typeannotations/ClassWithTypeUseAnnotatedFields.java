package com.tngtech.archunit.core.importer.testexamples.typeannotations;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ClassWithTypeUseAnnotatedFields {

    public @RuntimeRetainedTypeUseAnnotation ClassWithAnnotation fieldWithRuntimeRetainedTypeUseAnnotationOnType;

    public @ClassRetainedTypeUseAnnotation String fieldWithClassRetainedTypeUseAnnotationOnType;

    public List<@RuntimeRetainedTypeUseAnnotation String> fieldWithRuntimeRetainedTypeUseAnnotationOnTypeArgument;

    public Map<String, List<@RuntimeRetainedTypeUseAnnotation Integer>> fieldWithRuntimeRetainedTypeUseAnnotationOnNestedTypeArgument;

    public List<? extends @RuntimeRetainedTypeUseAnnotation Number> fieldWithRuntimeRetainedTypeUseAnnotationOnWildcardUpperBound;

    public List<? super @RuntimeRetainedTypeUseAnnotation Number> fieldWithRuntimeRetainedTypeUseAnnotationOnWildcardLowerBound;

    public List<@RuntimeRetainedTypeUseAnnotation ?> fieldWithRuntimeRetainedTypeUseAnnotationOnWildcard;

    public @RuntimeRetainedTypeUseAnnotation String[] fieldWithRuntimeRetainedTypeUseAnnotationOnArrayElementType;

    public String @RuntimeRetainedTypeUseAnnotation [] fieldWithRuntimeRetainedTypeUseAnnotationOnArrayType;

    public Outer.@RuntimeRetainedTypeUseAnnotation Inner fieldWithRuntimeRetainedTypeUseAnnotationOnInnerTypeQualifier;

    public @RuntimeRetainedTypeUseAnnotation @SecondRuntimeRetainedTypeUseAnnotation String fieldWithMultipleTypeUseAnnotationsAtSamePosition;

    public List<@RuntimeRetainedTypeUseAnnotation String> @SecondRuntimeRetainedTypeUseAnnotation [] fieldWithTypeUseAnnotationsAtMultiplePositions;

    // targets both FIELD and TYPE_USE: must appear as declaration annotation on the field
    // and as a type annotation on the field's type
    public @RuntimeRetainedFieldAndTypeUseAnnotation String fieldWithBothDeclarationAndTypeUseAnnotation;

    public static class Outer {
        public static class Inner {
        }
    }
}
