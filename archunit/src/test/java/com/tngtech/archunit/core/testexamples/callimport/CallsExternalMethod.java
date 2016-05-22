package com.tngtech.archunit.core.testexamples.callimport;

import java.util.ArrayList;

public class CallsExternalMethod {
    String getString() {
        return new ArrayList<>().toString();
    }
}
