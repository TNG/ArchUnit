package com.tngtech.archunit.integration;

import java.io.InputStream;
import java.security.cert.CertificateFactory;

import com.tngtech.archunit.example.persistence.WrongSecurityCheck;
import com.tngtech.archunit.exampletest.SecurityTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.ExpectedAccess.callFrom;

public class SecurityIntegrationTest extends SecurityTest {
    @Rule
    public final ExpectedViolation expectedViolation = ExpectedViolation.none();

    @Test
    @Override
    public void only_security_infrastructure_should_use_java_security() {
        expectedViolation.ofRule("classes that reside in a package 'java.security..' "
                + "should only be accessed by any package ['..example.security..', 'java.security..'], "
                + "because we want to have one isolated cross-cutting concern 'security'")

                .by(callFrom(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "getInstance", String.class)
                        .inLine(19))
                .by(callFrom(WrongSecurityCheck.class, "doCustomNonsense")
                        .toMethod(CertificateFactory.class, "generateCertificate", InputStream.class)
                        .inLine(19));

        super.only_security_infrastructure_should_use_java_security();
    }
}
