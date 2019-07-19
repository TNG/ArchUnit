package com.tngtech.archunit.example.layers;

import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.example.layers.thirdparty.ThirdPartySubClassWithProblem;

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
