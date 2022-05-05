package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.FailureReport;

import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

class ClassesShouldEvaluator {
    private static final String OPTIONAL_ARGS_REGEX = "(?:\\([^)]*\\))?";
    private static final String METHOD_OR_FIELD_REGEX = "\\.[\\w<>]+" + OPTIONAL_ARGS_REGEX;
    private static final String MEMBER_REFERENCE_REGEX = "<(.*)" + METHOD_OR_FIELD_REGEX + ">";
    private static final String SAME_CLASS_BACK_REFERENCE_REGEX = "<\\1" + METHOD_OR_FIELD_REGEX + ">";
    private static final String SELF_REFERENCE_REGEX = MEMBER_REFERENCE_REGEX + ".*" + SAME_CLASS_BACK_REFERENCE_REGEX;

    private final ArchRule rule;
    private final ClassInReportLineMatcher reportLineMatcher;

    private ClassesShouldEvaluator(ArchRule rule, ClassInReportLineMatcher reportLineMatcher) {
        this.rule = rule;
        this.reportLineMatcher = reportLineMatcher;
    }

    static ClassesShouldEvaluator filterClassesAppearingInFailureReport(ArchRule rule) {
        return new ClassesShouldEvaluator(rule, new ClassContainedInLineMatcher());
    }

    static ClassesShouldEvaluator filterViolationCausesInFailureReport(ArchRule rule) {
        return new ClassesShouldEvaluator(rule, new ClassOriginInLineMatcher());
    }

    Set<JavaClass> on(Class<?>... toCheck) {
        JavaClasses classes = importClasses(toCheck);
        List<String> relevantFailures = getRelevantFailures(classes);
        return classes.stream().filter(clazz -> anyLineMatches(relevantFailures, clazz)).collect(toSet());
    }

    private boolean anyLineMatches(List<String> relevantFailures, JavaClass clazz) {
        return relevantFailures.stream().anyMatch(line -> reportLineMatcher.matches(line, clazz));
    }

    private List<String> getRelevantFailures(JavaClasses classes) {
        return linesIn(rule.evaluate(classes).getFailureReport()).stream()
                .filter(line -> !isDefaultConstructor(line))
                .filter(line -> !isSelfReference(line))
                .filter(line -> !isExtendsJavaLangAnnotation(line))
                .collect(toList());
    }

    private boolean isDefaultConstructor(String line) {
        return line.contains(Object.class.getName());
    }

    private boolean isSelfReference(String line) {
        return line.matches(".*" + SELF_REFERENCE_REGEX + ".*");
    }

    private boolean isExtendsJavaLangAnnotation(String line) {
        return line.matches(String.format(".*extends.*<%s> in.*", Annotation.class.getName()));
    }

    private List<String> linesIn(FailureReport failureReport) {
        return failureReport.getDetails().stream()
                .flatMap(details -> Splitter.on(System.lineSeparator()).splitToStream(details))
                .collect(toList());
    }

    private JavaClasses importClasses(Class<?>... classes) {
        try {
            ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(true);
            return new ClassFileImporter().importClasses(ImmutableSet.copyOf(classes));
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private abstract static class ClassInReportLineMatcher {
        abstract boolean matches(String line, JavaClass javaClass);
    }

    private static class ClassContainedInLineMatcher extends ClassInReportLineMatcher {
        @Override
        boolean matches(String line, JavaClass javaClass) {
            return line.contains(javaClass.getName());
        }
    }

    private static class ClassOriginInLineMatcher extends ClassInReportLineMatcher {
        @Override
        boolean matches(String line, JavaClass javaClass) {
            String optionalMemberName = "(\\.[^$]+)?";
            String optionalMethodParameters = "(\\([^)]*\\))?";
            String originPattern = String.format(".*<%s%s%s> .* <.*> in \\(.*\\.java.*\\)",
                    quote(javaClass.getName()), optionalMemberName, optionalMethodParameters);
            return line.matches(originPattern);
        }
    }
}
