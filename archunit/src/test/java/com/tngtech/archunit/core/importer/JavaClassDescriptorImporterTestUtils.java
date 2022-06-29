package com.tngtech.archunit.core.importer;

public class JavaClassDescriptorImporterTestUtils {
    public static boolean isLambdaMethodName(String methodName) {
        return JavaClassDescriptorImporter.isLambdaMethodName(methodName);
    }

    public static boolean isSyntheticAccessMethodName(String methodName) {
        return JavaClassDescriptorImporter.isSyntheticAccessMethodName(methodName);
    }
}
