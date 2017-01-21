package com.tngtech.archunit.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.google.common.io.Resources.readLines;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a rule about a specified set of objects of interest (e.g. {@link JavaClass}).
 * To define a rule, use {@link ArchRuleDefinition#all(ClassesTransformer)}, for example
 * <br/><br/><pre><code>
 * ClassesTransformer services = classes().that(resideIn("..svc..")).as("Services");
 * ArchRule rule = all(services).should(never(accessClassesThatResideIn("..ui..")).as("access the UI"));
 * rule.check(importedJavaClasses);
 * </code></pre>
 * where '<code>services</code>' is (in this case just) a filter denoting when a {@link JavaClass} counts as service
 * (e.g. package contains 'svc').
 * <br/><br/>
 * {@link ClassesTransformer} in general defines how the type of objects
 * can be created from imported {@link JavaClasses}. It can filter, like in the example, or completely transform,
 * e.g. if you want to define a rule on all imported methods you could specify a transformer to retrieve methods
 * from classes, or if you're interested in slices of packages, the input transformer would specify how to transform
 * the imported classes to those slices to run an {@link ArchCondition} against.
 *
 * @see com.tngtech.archunit.library.dependencies.Slices.Transformer
 */
public interface ArchRule extends CanBeEvaluated, CanOverrideDescription<ArchRule> {
    void check(JavaClasses classes);

    class Assertions {
        static final String ARCHUNIT_IGNORE_PATTERNS_FILE_NAME = "archunit_ignore_patterns.txt";

        public static void assertNoViolation(EvaluationResult result) {
            assertNoViolation(result, Priority.MEDIUM);
        }

        public static void assertNoViolation(EvaluationResult result, Priority priority) {
            FailureReport report = result.getFailureReport();

            Set<Pattern> patterns = readPatternsFrom(ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
            report = report.filter(notMatchedByAny(patterns));
            if (!report.isEmpty()) {
                String message = report.toString();
                throw new AssertionError(message);
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
            URL ignorePatternsResource = Assertions.class.getResource('/' + fileNameInClassPath);
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
                result.add(Pattern.compile(line));
            }
            return result.build();
        }
    }

    class Factory {
        public static <T> ArchRule create(final ClassesTransformer<T> classesTransformer, final ArchCondition<T> condition, final Priority priority) {
            return new SimpleArchRule<>(priority, classesTransformer, condition, Optional.<String>absent());
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
                EvaluationResult result = evaluate(classes);
                Assertions.assertNoViolation(result, priority);
            }

            @Override
            public EvaluationResult evaluate(JavaClasses classes) {
                Iterable<T> allObjects = classesTransformer.transform(classes);
                condition.init(allObjects);
                ConditionEvents events = new ConditionEvents();
                for (T object : allObjects) {
                    condition.check(object, events);
                }
                return new EvaluationResult(this, events, priority);
            }

            @Override
            public String getDescription() {
                return overriddenDescription.isPresent() ?
                        overriddenDescription.get() :
                        ConfiguredMessageFormat.get().formatRuleText(classesTransformer, condition);
            }
        }
    }
}
