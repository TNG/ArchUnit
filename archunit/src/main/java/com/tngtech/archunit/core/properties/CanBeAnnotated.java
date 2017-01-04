package com.tngtech.archunit.core.properties;

import java.lang.annotation.Annotation;

public interface CanBeAnnotated {
    boolean isAnnotatedWith(Class<? extends Annotation> annotation);
}
