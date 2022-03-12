package com.tngtech.archunit.core.importer;

public class JavaClassDescriptorImporterTestUtils {
    public static boolean isLambdaMethodName(String methodName) {
        return JavaClassDescriptorImporter.isLambdaMethodName(methodName);
    }
}
