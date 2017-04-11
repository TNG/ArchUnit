package com.tngtech.archunit.core.domain;

import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;

@Internal
public interface ImportContext {
    JavaClass getJavaClassWithType(String name);

    Optional<JavaClass> createSuperClass(JavaClass owner);

    Set<JavaClass> createInterfaces(JavaClass owner);

    Set<JavaField> createFields(JavaClass owner);

    Set<JavaMethod> createMethods(JavaClass owner);

    Set<JavaConstructor> createConstructors(JavaClass owner);

    Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner);

    Map<String, JavaAnnotation> createAnnotations(JavaClass owner);

    Optional<JavaClass> createEnclosingClass(JavaClass owner);

    Set<JavaFieldAccess> getFieldAccessesFor(JavaCodeUnit codeUnit);

    Set<JavaMethodCall> getMethodCallsFor(JavaCodeUnit codeUnit);

    Set<JavaConstructorCall> getConstructorCallsFor(JavaCodeUnit codeUnit);
}
