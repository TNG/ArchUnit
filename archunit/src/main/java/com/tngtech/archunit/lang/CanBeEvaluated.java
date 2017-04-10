package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasDescription;

public interface CanBeEvaluated extends HasDescription {
    EvaluationResult evaluate(JavaClasses classes);
}
