package com.tngtech.archunit.library.testclasses.timeapi.incorrect;

import java.sql.Date;

public class UsesJavaSqlDate {

    void usesSqlDate() {
        Date badUsage = new Date(System.currentTimeMillis());
    }
}
