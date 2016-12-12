package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class ClassFileImportRecord {
    private final Set<JavaClass> classes = new HashSet<>();
    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();

    void registerFieldAccess(RawAccessRecord.ForField record) {
        rawFieldAccessRecords.add(record);
    }

    void registerMethodCall(RawAccessRecord record) {
        rawMethodCallRecords.add(record);
    }

    void registerConstructorCall(RawAccessRecord record) {
        rawConstructorCallRecords.add(record);
    }

    Set<RawAccessRecord.ForField> getRawFieldAccessRecords() {
        return ImmutableSet.copyOf(rawFieldAccessRecords);
    }

    Set<RawAccessRecord> getRawMethodCallRecords() {
        return ImmutableSet.copyOf(rawMethodCallRecords);
    }

    Set<RawAccessRecord> getRawConstructorCallRecords() {
        return ImmutableSet.copyOf(rawConstructorCallRecords);
    }

    void addAll(Collection<JavaClass> javaClasses) {
        classes.addAll(javaClasses);
    }

    public Set<JavaClass> getClasses() {
        return classes;
    }

    Set<RawAccessRecord> getAccessRecords() {
        return ImmutableSet.<RawAccessRecord>builder()
                .addAll(rawFieldAccessRecords)
                .addAll(rawMethodCallRecords)
                .addAll(rawConstructorCallRecords)
                .build();
    }
}
