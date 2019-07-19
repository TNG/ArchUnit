package com.tngtech.archunit.example.layers.thirdparty;

/**
 * Assuming creation via this factory would provide some workaround for correct initialization in the given context.
 */
public class ThirdPartyClassWorkaroundFactory {
    public ThirdPartyClassWithProblem create() {
        // some workaround here
        return new ThirdPartyClassWithProblem();
    }

    public ThirdPartySubClassWithProblem createSubClass() {
        // some workaround here
        return new ThirdPartySubClassWithProblem();
    }
}
