package com.tngtech.archunit.testutils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.Dependency;

import static com.tngtech.archunit.testutils.MessageAssertionChain.matchesLine;
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

    public static UncontainedCreator uncontainedFrom(Class<?> origin) {
        return new UncontainedCreator(origin);
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

    public static class UncontainedCreator {
        private final Class<?> origin;

        private UncontainedCreator(Class<?> origin) {
            this.origin = origin;
        }

        public ExpectedUncontainedModuleDependency to(Class<?> target) {
            return new ExpectedUncontainedModuleDependency(origin, target);
        }
    }

    private static class ExpectedUncontainedModuleDependency implements MessageAssertionChain.Link, ExpectedRelation {
        private final String dependencyPattern;
        private final MessageAssertionChain.Link delegate;

        private ExpectedUncontainedModuleDependency(Class<?> origin, Class<?> target) {
            dependencyPattern = String.format("Dependency not contained in any module: .*%s.*%s.*",
                    quote(origin.getName()), quote(target.getName()));
            this.delegate = matchesLine(dependencyPattern);
        }

        @Override
        public void addTo(HandlingAssertion assertion) {
            assertion.byDependency(this);
        }

        @Override
        public void associateLines(LineAssociation association) {
            association.associateIfPatternMatches(dependencyPattern);
        }

        @Override
        public boolean correspondsTo(Object object) {
            return object instanceof Dependency;
        }

        @Override
        public Result filterMatching(List<String> lines) {
            return delegate.filterMatching(lines);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }
    }
}
