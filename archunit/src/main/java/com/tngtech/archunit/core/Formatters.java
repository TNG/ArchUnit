package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;

public class Formatters {
    private static final String FULL_METHOD_NAME_TEMPLATE = "%s.%s(%s)";

    private Formatters() {
    }

    public static String formatMethod(String ownerName, String methodName, JavaClassList parameters) {
        return String.format(FULL_METHOD_NAME_TEMPLATE, ownerName, methodName, formatMethodParameters(parameters));
    }

    public static String formatMethod(String ownerName, String methodName, List<String> parameters) {
        return String.format(FULL_METHOD_NAME_TEMPLATE, ownerName, methodName, formatMethodParameterTypeNames(parameters));
    }

    private static String formatMethodParameters(List<? extends HasName> parameters) {
        List<String> simpleNames = new ArrayList<>();
        for (HasName type : parameters) {
            simpleNames.add(type.getName());
        }
        return formatMethodParameterTypeNames(simpleNames);
    }

    public static String formatMethodParameterTypeNames(List<String> typeNames) {
        List<String> formatted = new ArrayList<>();
        for (String name : typeNames) {
            formatted.add(String.format("%s.class", ensureSimpleName(name)));
        }
        return Joiner.on(", ").join(formatted);
    }

    static String ensureSimpleName(String name) {
        String lastPart = name.replaceAll("^.*(\\.|\\$)", "");
        return isAnonymousRest(lastPart) ? "" : lastPart;
    }

    // NOTE: Anonymous classes (e.g. clazz.getName() == some.Type$1) return an empty clazz.getSimpleName(),
    //       so we mimic this behavior
    private static boolean isAnonymousRest(String lastPart) {
        return Ints.tryParse(lastPart) != null;
    }
}
