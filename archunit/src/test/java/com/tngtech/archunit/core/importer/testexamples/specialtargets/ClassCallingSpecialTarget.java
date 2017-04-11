package com.tngtech.archunit.core.importer.testexamples.specialtargets;

public class ClassCallingSpecialTarget {
    private SpecialTarget specialTarget;

    void callSpecialTarget() {
        specialTarget.primitiveArgs((byte) 0, 0L);
        specialTarget.primitiveReturnType();
        specialTarget.arrayArgs(new byte[0], new Object[0]);
        specialTarget.primitiveArrayReturnType();
        specialTarget.objectArrayReturnType();
        specialTarget.twoDimArrayArgs(new float[0][], new Object[0][]);
        specialTarget.primitiveTwoDimArrayReturnType();
        specialTarget.objectTwoDimArrayReturnType();
    }
}
