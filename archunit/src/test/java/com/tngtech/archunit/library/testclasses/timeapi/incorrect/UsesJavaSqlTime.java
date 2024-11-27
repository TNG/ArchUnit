package com.tngtech.archunit.library.testclasses.timeapi.incorrect;

import java.sql.Time;

public class UsesJavaSqlTime {

    void usesSqlTimestamp() {
        Time badUsage = new Time(System.currentTimeMillis());
    }
}
