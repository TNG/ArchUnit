package com.tngtech.archunit.core.importer.testexamples.modifierfieldimport;

public class ClassWithFieldsWithModifiers {
    private Object privateField;
    private final Object privateFinalField = null;
    private static Object privateStaticField;
    private static final Object privateStaticFinalField = null;
    Object defaultField;
    static Object staticDefaultField;
    protected Object protectedField;
    protected final Object protectedFinalField = null;
    public Object publicField;
    public static final Object publicStaticFinalField = null;
    volatile Object volatileField;
    transient Object synchronizedField;
}
