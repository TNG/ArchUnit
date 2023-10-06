package com.tngtech.archunit.library.modules.testexamples.annotation_with_custom_name;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface MyModuleWithCustomName {
    String customName();
}
