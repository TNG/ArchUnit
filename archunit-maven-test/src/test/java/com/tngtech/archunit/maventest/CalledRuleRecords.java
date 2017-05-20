package com.tngtech.archunit.maventest;

import java.io.File;
import java.io.IOException;

import static com.tngtech.archunit.thirdparty.com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertTrue;

class CalledRuleRecords {
    static void register(Class<?> ruleDeclaringClass, String ruleDescriptor) {
        File file = ruleFile(ruleDeclaringClass, ruleDescriptor);

        try {
            checkState(file.createNewFile(), "Can't create new file %s", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void verifyEvaluationOf(Class<?> ruleDeclaringClass, String ruleDescriptor) {
        assertTrue(
                String.format("Rule with descriptor %s was evaluated", ruleFileName(ruleDeclaringClass, ruleDescriptor)),
                ruleFile(ruleDeclaringClass, ruleDescriptor).exists());
    }

    private static File ruleFile(Class<?> ruleDeclaringClass, String ruleDescriptor) {
        File targetFolder = new File(CalledRuleRecords.class.getResource("/").getFile());
        return new File(targetFolder, ruleFileName(ruleDeclaringClass, ruleDescriptor));
    }

    private static String ruleFileName(Class<?> ruleDeclaringClass, String ruleName) {
        return ruleDeclaringClass.getSimpleName() + "_" + ruleName;
    }
}
