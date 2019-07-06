package com.tngtech.archunit.example.layers.security;

import java.security.cert.X509Certificate;

public class AdditionalSecurityChecks {
    void check(X509Certificate certificate) {
        checkAdditional(certificate.getSignature());
    }

    private void checkAdditional(byte[] signature) {
        // some great additional check
    }
}
