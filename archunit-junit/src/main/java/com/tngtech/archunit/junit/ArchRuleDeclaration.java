package com.tngtech.archunit.junit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

abstract class ArchRuleDeclaration<T extends AnnotatedElement> {
    private final Class<?> testClass;
    final T declaration;
    private final boolean forceIgnore;

    ArchRuleDeclaration(Class<?> testClass, T declaration, boolean forceIgnore) {
        this.testClass = testClass;
        this.declaration = declaration;
        this.forceIgnore = forceIgnore;
    }

    abstract void handleWith(Handler handler);

    static ArchRuleDeclaration from(Class<?> testClass, Method method, boolean forceIgnore) {
        return new AsMethod(testClass, method, forceIgnore);
    }

    static ArchRuleDeclaration from(Class<?> testClass, Field field, boolean forceIgnore) {
        return new AsField(testClass, field, forceIgnore);
    }

    static <T extends AnnotatedElement & Member> boolean elementShouldBeIgnored(T member) {
        return elementShouldBeIgnored(member.getDeclaringClass(), member);
    }

    static boolean elementShouldBeIgnored(Class<?> testClass, AnnotatedElement ruleDeclaration) {
        return testClass.getAnnotation(ArchIgnore.class) != null ||
                ruleDeclaration.getAnnotation(ArchIgnore.class) != null;
    }

    boolean shouldBeIgnored() {
        return forceIgnore || elementShouldBeIgnored(testClass, declaration);
    }

    private static class AsMethod extends ArchRuleDeclaration<Method> {
        AsMethod(Class<?> testClass, Method method, boolean forceIgnore) {
            super(testClass, method, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleMethodDeclaration(declaration, shouldBeIgnored());
        }
    }

    private static class AsField extends ArchRuleDeclaration<Field> {
        AsField(Class<?> testClass, Field field, boolean forceIgnore) {
            super(testClass, field, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleFieldDeclaration(declaration, shouldBeIgnored());
        }
    }

    interface Handler {
        void handleFieldDeclaration(Field field, boolean ignore);

        void handleMethodDeclaration(Method method, boolean ignore);
    }
}
