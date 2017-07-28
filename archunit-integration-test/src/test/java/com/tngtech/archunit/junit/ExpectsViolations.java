package com.tngtech.archunit.junit;

import com.tngtech.archunit.junit.ExpectedAccess.ExpectedCall;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedFieldAccess;

public interface ExpectsViolations {
    ExpectsViolations ofRule(String ruleText);

    ExpectsViolations by(ExpectedFieldAccess access);

    ExpectsViolations by(ExpectedCall call);

    ExpectsViolations by(MessageAssertionChain.Link assertion);
}
