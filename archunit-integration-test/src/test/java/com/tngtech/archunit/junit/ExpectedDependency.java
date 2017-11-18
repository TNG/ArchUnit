package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.domain.Dependency;

import static java.util.regex.Pattern.quote;

public class ExpectedDependency implements ExpectedRelation {
    private final Class<?> origin;
    private final Class<?> target;
    private String dependencyTypePattern;
    private int lineNumber;

    private ExpectedDependency(Class<?> origin, String dependencyTypePattern, Class<?> target) {
        this(origin, dependencyTypePattern, target, 0);
    }

    private ExpectedDependency(Class<?> origin, String dependencyTypePattern, Class<?> target, int lineNumber) {
        this.origin = origin;
        this.target = target;
        this.dependencyTypePattern = dependencyTypePattern;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return String.format("Matches: %s ... %s ... %s ... .java:%d",
                origin.getName(), dependencyTypePattern, target.getName(), lineNumber);
    }

    public static InheritanceCreator inheritanceFrom(Class<?> clazz) {
        return new InheritanceCreator(clazz);
    }

    public static AccessCreator accessFrom(Class<?> clazz) {
        return new AccessCreator(clazz);
    }

    @Override
    public void associateLines(LineAssociation association) {
        association.associateIfPatternMatches(getCompleteDescriptionPattern());
    }

    private String getCompleteDescriptionPattern() {
        return String.format(".*%s.*%s.*%s.*\\.java:%d.*",
                quote(origin.getName()), dependencyTypePattern, quote(target.getName()), lineNumber);
    }

    @Override
    public boolean correspondsTo(Object object) {
        if (!(object instanceof Dependency)) {
            return false;
        }

        Dependency dependency = (Dependency) object;
        boolean originMatches = dependency.getOriginClass().isEquivalentTo(origin);
        boolean targetMatches = dependency.getTargetClass().isEquivalentTo(target);
        boolean descriptionMatches = dependency.getDescription().matches(getCompleteDescriptionPattern());
        return originMatches && targetMatches && descriptionMatches;
    }

    public static class InheritanceCreator {
        private final Class<?> clazz;

        private InheritanceCreator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ExpectedDependency extending(Class<?> superClass) {
            return new ExpectedDependency(clazz, "extends", superClass);
        }

        public ExpectedDependency implementing(Class<?> anInterface) {
            return new ExpectedDependency(clazz, "implements", anInterface);
        }
    }

    public static class AccessCreator {
        private final Class<?> originClass;

        private AccessCreator(Class<?> originClass) {
            this.originClass = originClass;
        }

        public Step2 toFieldDeclaredIn(Class<?> clazz) {
            return new Step2(clazz, "(accesses|gets|sets)");
        }

        public Step2 toCodeUnitDeclaredIn(Class<?> clazz) {
            return new Step2(clazz, "calls");
        }

        public class Step2 {
            private final Class<?> targetClass;
            private final String description;

            Step2(Class<?> targetClass, String description) {
                this.targetClass = targetClass;
                this.description = description;
            }

            public ExpectedDependency inLineNumber(int lineNumber) {
                return new ExpectedDependency(originClass, description, targetClass, lineNumber);
            }
        }
    }
}
