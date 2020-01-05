/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.extension.ArchUnitExtensions;
import com.tngtech.archunit.lang.extension.EvaluatedRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesThat;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;

import static com.google.common.io.Resources.readLines;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a rule about a specified set of objects of interest (e.g. {@link JavaClass}).
 * To define a rule, use one of the factory methods within {@link ArchRuleDefinition}, for example
 * <br><br><pre><code>
 * ArchRule rule = {@link ArchRuleDefinition#noClasses()}.{@link GivenClasses#that() that()}.{@link ClassesThat#resideInAPackage(String) resideInAPackage("..svc..")}
 *                     .{@link GivenClasses#should() should()}.{@link ClassesShould#accessClassesThat() accessClassesThat()}.{@link ClassesThat#resideInAPackage(String) resideInAPackage("..ui..")};
 * rule.check(importedJavaClasses);
 * </code></pre>
 * <br>
 * To write rules on custom objects, you can use {@link ArchRuleDefinition#all(ClassesTransformer)}, where
 * {@link ClassesTransformer} defines how the type of objects can be created from imported {@link JavaClasses}.
 * E.g. if you want to define a rule on all imported methods you could specify a transformer to retrieve methods
 * from classes, or if you're interested in slices of packages, the input transformer would specify how to transform
 * the imported classes to those slices to run an {@link ArchCondition} against.
 *
 * @see com.tngtech.archunit.library.dependencies.Slices.Transformer
 */
public interface ArchRule extends CanBeEvaluated, CanOverrideDescription<ArchRule> {
    @PublicAPI(usage = ACCESS)
    void check(JavaClasses classes);

    @PublicAPI(usage = ACCESS)
    ArchRule because(String reason);

    @PublicAPI(usage = ACCESS)
    final class Assertions {
        private static final ArchUnitExtensions extensions = new ArchUnitExtensions();

        private Assertions() {
        }

        static final String ARCHUNIT_IGNORE_PATTERNS_FILE_NAME = "archunit_ignore_patterns.txt";
        private static final String COMMENT_LINE_PREFIX = "#";

        @PublicAPI(usage = ACCESS)
        public static void check(ArchRule rule, JavaClasses classes) {
            EvaluationResult result = rule.evaluate(classes);
            extensions.dispatch(new SimpleEvaluatedRule(rule, classes, result));
            assertNoViolation(result);
        }

        @PublicAPI(usage = ACCESS)
        public static void assertNoViolation(EvaluationResult result) {
            FailureReport report = result.getFailureReport();

            Set<Pattern> patterns = readPatternsFrom(ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
            report = report.filter(notMatchedByAny(patterns));
            if (!report.isEmpty()) {
                throw new AssertionError(report.toString());
            }
        }

        private static Predicate<String> notMatchedByAny(final Set<Pattern> patterns) {
            return new Predicate<String>() {
                @Override
                public boolean apply(String message) {
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(message.replaceAll("\r*\n", " ")).matches()) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }

        private static Set<Pattern> readPatternsFrom(String fileNameInClassPath) {
            URL ignorePatternsResource = getCurrentClassLoader(Assertions.class).getResource(fileNameInClassPath);
            if (ignorePatternsResource == null) {
                return Collections.emptySet();
            }

            try {
                return readPatternsFrom(ignorePatternsResource);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static Set<Pattern> readPatternsFrom(URL ignorePatternsResource) throws IOException {
            ImmutableSet.Builder<Pattern> result = ImmutableSet.builder();
            for (String line : readLines(ignorePatternsResource, UTF_8)) {
                if (!line.startsWith(COMMENT_LINE_PREFIX)) {
                    result.add(Pattern.compile(line));
                }
            }
            return result.build();
        }

        private static class SimpleEvaluatedRule implements EvaluatedRule {
            private final ArchRule rule;
            private final JavaClasses importedClasses;
            private final EvaluationResult evaluationResult;

            SimpleEvaluatedRule(ArchRule rule, JavaClasses importedClasses, EvaluationResult evaluationResult) {
                this.rule = rule;
                this.importedClasses = importedClasses;
                this.evaluationResult = evaluationResult;
            }

            @Override
            public ArchRule getRule() {
                return rule;
            }

            @Override
            public JavaClasses getClasses() {
                return importedClasses;
            }

            @Override
            public EvaluationResult getResult() {
                return evaluationResult;
            }
        }
    }

    @Internal
    class Factory {
        public static <T> ArchRule create(final ClassesTransformer<T> classesTransformer, final ArchCondition<T> condition, final Priority priority) {
            return new SimpleArchRule<>(priority, classesTransformer, condition, Optional.<String>absent());
        }

        public static ArchRule withBecause(ArchRule rule, String reason) {
            return rule.as(createBecauseDescription(rule, reason));
        }

        static String createBecauseDescription(ArchRule rule, String reason) {
            return rule.getDescription() + ", because " + reason;
        }

        private static class SimpleArchRule<T> implements ArchRule {
            private final Priority priority;
            private final ClassesTransformer<T> classesTransformer;
            private final ArchCondition<T> condition;
            private final Optional<String> overriddenDescription;

            private SimpleArchRule(Priority priority, ClassesTransformer<T> classesTransformer, ArchCondition<T> condition,
                    Optional<String> overriddenDescription) {
                this.priority = priority;
                this.classesTransformer = classesTransformer;
                this.condition = condition;
                this.overriddenDescription = overriddenDescription;
            }

            @Override
            public ArchRule as(String newDescription) {
                return new SimpleArchRule<>(priority, classesTransformer, condition, Optional.of(newDescription));
            }

            @Override
            public void check(JavaClasses classes) {
                Assertions.check(this, classes);
            }

            @Override
            public ArchRule because(String reason) {
                return withBecause(this, reason);
            }

            @Override
            public EvaluationResult evaluate(JavaClasses classes) {
                Iterable<T> allObjects = classesTransformer.transform(classes);
                condition.init(allObjects);
                ConditionEvents events = new ConditionEvents();
                for (T object : allObjects) {
                    condition.check(object, events);
                }
                condition.finish(events);
                return new EvaluationResult(this, events, priority);
            }

            @Override
            public String getDescription() {
                return overriddenDescription.isPresent() ?
                        overriddenDescription.get() :
                        ConfiguredMessageFormat.get().formatRuleText(classesTransformer, condition);
            }

            @Override
            public String toString() {
                return getDescription();
            }
        }
    }

    @Internal
    interface Transformation {
        ArchRule apply(ArchRule rule);

        @Internal
        final class As implements Transformation {
            private final String description;

            public As(String description) {
                this.description = description;
            }

            @Override
            public ArchRule apply(ArchRule rule) {
                return rule.as(description);
            }

            @Override
            public String toString() {
                return String.format("as '%s'", description);
            }
        }

        @Internal
        final class Because implements Transformation {
            private final String reason;

            public Because(String reason) {
                this.reason = reason;
            }

            @Override
            public ArchRule apply(ArchRule rule) {
                return rule.because(reason);
            }

            @Override
            public String toString() {
                return String.format("because '%s'", reason);
            }
        }
    }
}
