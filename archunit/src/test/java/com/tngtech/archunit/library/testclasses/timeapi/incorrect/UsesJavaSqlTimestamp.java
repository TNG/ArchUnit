package com.tngtech.archunit.library.testclasses.timeapi.incorrect;

import java.sql.Timestamp;

public class UsesJavaSqlTimestamp {

    void usesSqlTimestamp() {
        Timestamp badUsage = new Timestamp(System.currentTimeMillis());
    }
}
