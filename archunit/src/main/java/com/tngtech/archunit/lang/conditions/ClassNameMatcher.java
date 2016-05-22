package com.tngtech.archunit.lang.conditions;

public class ClassNameMatcher {
    private final String classNameRegex;

    private ClassNameMatcher(String classNameIdentifier) {
        this.classNameRegex = regexOf(validated(classNameIdentifier, "classNameIdentifier"));
    }

    private String regexOf(String classNameIdentifier) {
        return classNameIdentifier.replace("*", ".*");
    }

    private String validated(String argument, String name) {
        if (argument == null) {
            throw new IllegalArgumentException(String.format("%s must not be null", name));
        }
        return argument;
    }

    public static ClassNameMatcher of(String classNameIdentifier) {
        return new ClassNameMatcher(classNameIdentifier);
    }

    public boolean matches(String simpleClassName) {
        return validated(simpleClassName, "simpleClassName").matches(classNameRegex);
    }
}
