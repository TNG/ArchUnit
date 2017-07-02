package com.tngtech.archunit.junit;

import com.tngtech.archunit.junit.ExpectedAccess.ExpectedFieldAccess;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedMethodCall;

public interface ExpectsViolations {
    ExpectsViolations ofRule(String ruleText);

    ExpectsViolations byAccess(ExpectedFieldAccess access);

    ExpectsViolations byCall(ExpectedMethodCall call);

    ExpectsViolations by(MessageAssertionChain.Link assertion);
}
