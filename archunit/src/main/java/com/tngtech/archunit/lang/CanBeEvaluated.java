package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasDescription;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface CanBeEvaluated extends HasDescription {
    @PublicAPI(usage = ACCESS)
    EvaluationResult evaluate(JavaClasses classes);
}
