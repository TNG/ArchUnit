package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

import static com.google.common.base.Preconditions.checkState;

class ClassFileImportRecord {
    private final Set<JavaClass> classes = new HashSet<>();

    private final SetMultimap<String, JavaField.Builder> fieldBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaMethod.Builder> methodBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaConstructor.Builder> constructorBuildersByOwner = HashMultimap.create();
    private final Map<String, JavaStaticInitializer.Builder> staticInitializerBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, JavaAnnotation.Builder> annotationsByOwner = HashMultimap.create();

    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();

    ClassFileImportRecord addField(String ownerName, JavaField.Builder fieldBuilder) {
        fieldBuildersByOwner.put(ownerName, fieldBuilder);
        return this;
    }

    ClassFileImportRecord addMethod(String ownerName, JavaMethod.Builder methodBuilder) {
        methodBuildersByOwner.put(ownerName, methodBuilder);
        return this;
    }

    ClassFileImportRecord addConstructor(String ownerName, JavaConstructor.Builder constructorBuilder) {
        constructorBuildersByOwner.put(ownerName, constructorBuilder);
        return this;
    }

    ClassFileImportRecord setStaticInitializer(String ownerName, JavaStaticInitializer.Builder builder) {
        checkState(!staticInitializerBuildersByOwner.containsKey(ownerName),
                "Tried to add a second static initializer to %s, this is most likely a bug",
                ownerName);
        staticInitializerBuildersByOwner.put(ownerName, builder);
        return this;
    }

    ClassFileImportRecord addAnnotations(String ownerName, Set<JavaAnnotation.Builder> annotations) {
        this.annotationsByOwner.putAll(ownerName, annotations);
        return this;
    }

    Set<JavaField.Builder> getFieldBuildersFor(String ownerName) {
        return fieldBuildersByOwner.get(ownerName);
    }

    Set<JavaMethod.Builder> getMethodBuildersFor(String ownerName) {
        return methodBuildersByOwner.get(ownerName);
    }

    Set<JavaConstructor.Builder> getConstructorBuildersFor(String ownerName) {
        return constructorBuildersByOwner.get(ownerName);
    }

    Optional<JavaStaticInitializer.Builder> getStaticInitializerBuilderFor(String ownerName) {
        return Optional.fromNullable(staticInitializerBuildersByOwner.get(ownerName));
    }

    Set<JavaAnnotation.Builder> getAnnotationsFor(String ownerName) {
        return annotationsByOwner.get(ownerName);
    }

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

    Set<JavaClass> getClasses() {
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
