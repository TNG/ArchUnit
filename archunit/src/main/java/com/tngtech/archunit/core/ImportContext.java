package com.tngtech.archunit.core;

import java.util.Set;

import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

public interface ImportContext {
    Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit codeUnit);

    Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit codeUnit);

    Set<AccessRecord<AccessTarget.ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit codeUnit);

    Optional<JavaClass> tryGetJavaClassWithType(String name);
}
