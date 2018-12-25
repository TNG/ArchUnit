package com.tngtech.archunit.htmlvisualization.testjson.violations;

public class Accessor {
    public void simpleMethodCall() {
        new Target().method();
    }

    public void complexMethodCall(String foo, Object bar) {
        new Target().complexMethod(foo, bar, this);
    }

    public void fieldAccess1(Target target) {
        target.field1 = "accessed";
    }

    public void fieldAccess2(Target target) {
        target.field2 = "accessed";
    }
}
