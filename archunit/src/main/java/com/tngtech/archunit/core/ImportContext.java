package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

interface ImportContext {
    JavaClass getJavaClassWithType(String name);

    Set<JavaField> createFields(JavaClass owner);

    Set<JavaMethod> createMethods(JavaClass owner);

    Set<JavaConstructor> createConstructors(JavaClass owner);

    Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner);

    Map<String, JavaAnnotation> createAnnotations(JavaClass owner);

    Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit codeUnit);

    Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit codeUnit);

    Set<AccessRecord<AccessTarget.ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit codeUnit);
}
