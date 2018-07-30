package com.tngtech.archunit.core.domain.testobjects;

public class AllPrimitiveDependencies {
    private byte aByte;
    private char character;
    private short aShort;
    private int anInt;
    private long aLong;
    private float aFloat;
    private double aDouble;
    private boolean aBoolean;

    public AllPrimitiveDependencies(byte aByte, char character, short aShort, int anInt,
            long aLong, float aFloat, double aDouble, boolean aBoolean) {
        this.aByte = aByte;
        this.character = character;
        this.aShort = aShort;
        this.anInt = anInt;
        this.aLong = aLong;
        this.aFloat = aFloat;
        this.aDouble = aDouble;
        this.aBoolean = aBoolean;
    }

    public void testVoid() {
    }

    public byte testByte() {
        return aByte;
    }

    public char testChar() {
        return character;
    }

    public short testShort() {
        return aShort;
    }

    public int testInt() {
        return anInt;
    }

    public long testLong() {
        return aLong;
    }

    public float testFloat() {
        return aFloat;
    }

    public double testDouble() {
        return aDouble;
    }

    public boolean isaBoolean() {
        return aBoolean;
    }

    public void testMethodParameters(byte aByte, char character, short aShort, int anInt,
            long aLong, float aFloat, double aDouble, boolean aBoolean) {
    }
}
