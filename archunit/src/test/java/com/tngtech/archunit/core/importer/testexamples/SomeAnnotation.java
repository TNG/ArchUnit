package com.tngtech.archunit.core.importer.testexamples;

public @interface SomeAnnotation {
    String mandatory();

    String optional() default "optional";

    SomeEnum mandatoryEnum();

    SomeEnum optionalEnum() default SomeEnum.OTHER_VALUE;
}
