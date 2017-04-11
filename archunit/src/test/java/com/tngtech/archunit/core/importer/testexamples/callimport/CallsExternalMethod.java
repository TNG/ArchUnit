package com.tngtech.archunit.core.importer.testexamples.callimport;

import java.util.ArrayList;

public class CallsExternalMethod {
    String getString() {
        return new ArrayList<>().toString();
    }
}
