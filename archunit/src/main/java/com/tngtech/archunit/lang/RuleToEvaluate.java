package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;

public interface RuleToEvaluate extends HasDescription {
    ConditionEvents evaluate();
}
