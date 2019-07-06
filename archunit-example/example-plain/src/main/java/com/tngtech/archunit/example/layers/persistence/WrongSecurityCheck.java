package com.tngtech.archunit.example.layers.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class WrongSecurityCheck {
    private Certificate certificate;

    public WrongSecurityCheck() {
        doCustomNonsense();
    }

    private void doCustomNonsense() {
        try (FileInputStream fileInput = new FileInputStream(new File("/some/magic/resource.cert"))) {
            certificate = CertificateFactory.getInstance("X509").generateCertificate(fileInput);
        } catch (CertificateException | IOException e) {
            // we do not handle exceptions, because we're lazy ;-)
        }
    }
}
