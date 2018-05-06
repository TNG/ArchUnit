package com.tngtech.archunit.example;

import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.example.thirdparty.ThirdPartySubClassWithProblem;

public class ClassViolatingThirdPartyRules {
    ThirdPartyClassWithProblem illegallyInstantiateThirdPartyClass() {
        return new ThirdPartyClassWithProblem();
    }

    ThirdPartyClassWithProblem correctlyInstantiateThirdPartyClass() {
        return new ThirdPartyClassWorkaroundFactory().create();
    }

    ThirdPartySubClassWithProblem illegallyInstantiateThirdPartySubClass() {
        return new ThirdPartySubClassWithProblem();
    }

    ThirdPartySubClassWithProblem correctlyInstantiateThirdPartySubClass() {
        return new ThirdPartyClassWorkaroundFactory().createSubClass();
    }
}
