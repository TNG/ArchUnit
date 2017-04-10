package com.tngtech.archunit.core.importer.testexamples.specialtargets;

public interface SuperOne {
    void primitiveArgs(byte b, long l);

    byte primitiveReturnType();

    void arrayArgs(byte[] bytes, Object[] objects);

    short[] primitiveArrayReturnType();

    String[] objectArrayReturnType();

    void twoDimArrayArgs(float[][] bytes, Object[][] objects);

    double[][] primitiveTwoDimArrayReturnType();

    String[][] objectTwoDimArrayReturnType();
}
