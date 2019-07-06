package com.tngtech.archunit.example.layers.anticorruption;

import com.tngtech.archunit.example.layers.anticorruption.internal.InternalType;

class InternalUtility {
    InternalType okaySinceTheVisibilityIsNonPublic() {
        return new InternalType();
    }
}
