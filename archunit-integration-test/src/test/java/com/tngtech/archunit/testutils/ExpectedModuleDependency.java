package com.tngtech.archunit.testutils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.testutils.ExpectedRelation.LineAssociation;

import static java.util.regex.Pattern.quote;

public class ExpectedModuleDependency implements MessageAssertionChain.Link {
    private final String dependencyPattern;
    private final Set<ExpectedRelation> details = new HashSet<>();

    private ExpectedModuleDependency(String dependencyPattern) {
        this.dependencyPattern = dependencyPattern;
    }

    public static ModuleDependencyCreator fromModule(String moduleName) {
        return new ModuleDependencyCreator(moduleName);
    }

    public static MessageAssertionChain.Link uncontained(ExpectedRelation call) {
        return new ExpectedUncontainedModuleDependency(call);
    }

    @Override
    public void addTo(HandlingAssertion handlingAssertion) {
        details.forEach(it -> it.addTo(handlingAssertion));
    }

    @Override
    public Result filterMatching(List<String> lines) {
        return new Result.Builder()
                .matchesLine(dependencyPattern)
                .contains(details)
                .build(lines);
    }

    @Override
    public String getDescription() {
        return String.format("Module Dependency :: matches %s :: matches each of [%s]",
                dependencyPattern, Joiner.on(", ").join(details));
    }

    public ExpectedModuleDependency including(ExpectedRelation relation) {
        details.add(relation);
        return this;
    }

    public static class ModuleDependencyCreator {
        private final String originModuleName;

        private ModuleDependencyCreator(String originModuleName) {
            this.originModuleName = originModuleName;
        }

        public ExpectedModuleDependency toModule(String moduleName) {
            String description = quote(String.format("[%s -> %s]", originModuleName, moduleName));
            return new ExpectedModuleDependency(String.format("Module Dependency %s.*", description));
        }
    }

    private static class ExpectedUncontainedModuleDependency implements MessageAssertionChain.Link {
        private final ExpectedRelation delegate;

        private ExpectedUncontainedModuleDependency(ExpectedRelation delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addTo(HandlingAssertion assertion) {
            delegate.addTo(assertion);
        }

        @Override
        public Result filterMatching(List<String> lines) {
            Result.Builder builder = new Result.Builder();
            delegate.associateLines(new LineAssociation() {
                @Override
                public void associateIfPatternMatches(String pattern) {
                    builder.matchesLine("Dependency not contained in any module:" + pattern);
                }

                @Override
                public void associateIfStringIsContained(String string) {
                    associateIfPatternMatches(".*" + quote(string) + ".*");
                }
            });
            return builder.build(lines);
        }

        @Override
        public String getDescription() {
            return "Uncontained module dependency: " + delegate;
        }
    }
}
