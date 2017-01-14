package com.tngtech.archunit.core.testexamples;

public @interface SomeAnnotation {
    String mandatory();

    String optional() default "optional";

    SomeEnum mandatoryEnum();

    SomeEnum optionalEnum() default SomeEnum.OTHER_VALUE;
}
