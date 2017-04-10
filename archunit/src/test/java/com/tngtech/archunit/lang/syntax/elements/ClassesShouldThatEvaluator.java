package com.tngtech.archunit.lang.syntax.elements;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.FailureReport;

class ClassesShouldThatEvaluator {
    private static String OPTIONAL_ARGS_REGEX = "(?:\\([^)]*\\))?";
    private static String METHOD_OR_FIELD_REGEX = "\\.[\\w<>]+" + OPTIONAL_ARGS_REGEX;
    private static String MEMBER_REFERENCE_REGEX = "<(.*)" + METHOD_OR_FIELD_REGEX + ">";
    private static String SAME_CLASS_BACK_REFERENCE_REGEX = "<\\1" + METHOD_OR_FIELD_REGEX + ">";
    private static String SELF_REFERENCE_REGEX = MEMBER_REFERENCE_REGEX + ".*" + SAME_CLASS_BACK_REFERENCE_REGEX;

    private final ArchRule rule;

    private ClassesShouldThatEvaluator(ArchRule rule) {
        this.rule = rule;
    }

    static ClassesShouldThatEvaluator filterClassesAppearingInFailureReport(ClassesShouldConjunction classesShould) {
        return new ClassesShouldThatEvaluator(classesShould);
    }

    List<JavaClass> on(Class<?>... toCheck) {
        JavaClasses classes = importClasses(toCheck);
        String report = getRelevantFailures(classes);
        List<JavaClass> result = new ArrayList<>();
        for (JavaClass clazz : classes) {
            if (report.contains(clazz.getName())) {
                result.add(clazz);
            }
        }
        return result;
    }

    private String getRelevantFailures(JavaClasses classes) {
        List<String> relevant = new ArrayList<>();
        for (String line : linesIn(rule.evaluate(classes).getFailureReport())) {
            if (!isDefaultConstructor(line) && !isSelfReference(line)) {
                relevant.add(line);
            }
        }
        return Joiner.on("#").join(relevant);
    }

    private boolean isDefaultConstructor(String line) {
        return line.contains(Object.class.getName());
    }

    private boolean isSelfReference(String line) {
        return line.matches(".*" + SELF_REFERENCE_REGEX + ".*");
    }

    private List<String> linesIn(FailureReport failureReport) {
        List<String> result = new ArrayList<>();
        for (String details : failureReport.getDetails()) {
            result.addAll(Splitter.on(System.lineSeparator()).splitToList(details));
        }
        return result;
    }

    private JavaClasses importClasses(Class<?>... classes) {
        try {
            ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);
            return new ClassFileImporter().importClasses(ImmutableSet.copyOf(classes));
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
