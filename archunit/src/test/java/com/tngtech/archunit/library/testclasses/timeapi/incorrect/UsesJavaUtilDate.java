package com.tngtech.archunit.library.testclasses.timeapi.incorrect;

import java.time.Instant;
import java.util.Date;

public class UsesJavaUtilDate {

    void usesJavaUtilDate() {
        Date badDate = Date.from(Instant.now());
    }
}
