package com.tngtech.archunit.example.anticorruption;

import com.tngtech.archunit.example.anticorruption.internal.InternalType;

class InternalUtility {
    InternalType okaySinceTheVisibilityIsNonPublic() {
        return new InternalType();
    }
}
