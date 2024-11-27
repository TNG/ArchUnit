package com.tngtech.archunit.library.testclasses.timeapi.incorrect;

import java.util.Calendar;

public class UsesJavaUtilCalender {

    void usesJavaUtilCalender() {
        Calendar badUsage = Calendar.getInstance();
    }
}
