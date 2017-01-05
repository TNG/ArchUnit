package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.properties.HasDescription;

public interface CanBeEvaluated extends HasDescription {
    EvaluationResult evaluate(JavaClasses classes);
}
