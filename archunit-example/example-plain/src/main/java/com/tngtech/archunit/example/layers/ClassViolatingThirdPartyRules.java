package com.tngtech.archunit.example.layers;

import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartySubclassWithProblem;

public class ClassViolatingThirdPartyRules {
    ThirdPartyClassWithProblem illegallyInstantiateThirdPartyClass() {
        return new ThirdPartyClassWithProblem();
    }

    ThirdPartyClassWithProblem correctlyInstantiateThirdPartyClass() {
        return new ThirdPartyClassWorkaroundFactory().create();
    }

    ThirdPartySubclassWithProblem illegallyInstantiateThirdPartySubclass() {
        return new ThirdPartySubclassWithProblem();
    }

    ThirdPartySubclassWithProblem correctlyInstantiateThirdPartySubclass() {
        return new ThirdPartyClassWorkaroundFactory().createSubclass();
    }
}
