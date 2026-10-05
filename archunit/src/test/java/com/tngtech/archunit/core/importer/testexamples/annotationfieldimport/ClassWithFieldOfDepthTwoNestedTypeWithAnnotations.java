package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation2;
import com.tngtech.archunit.core.importer.testexamples.annotationtypeuse.TypeUseAnnotation3;

public class ClassWithFieldOfDepthTwoNestedTypeWithAnnotations {
    public @TypeUseAnnotation ClassWithFieldOfDepthTwoNestedTypeWithAnnotations.@TypeUseAnnotation2 Middle.@TypeUseAnnotation3 Inner fieldWithAnnotatedInnerType;
    public @TypeUseAnnotation2 Middle.@TypeUseAnnotation3 Inner fieldWithAnnotatedInnerType_unqualified;

    public class Middle {
        public class Inner {

        }
    }
}
