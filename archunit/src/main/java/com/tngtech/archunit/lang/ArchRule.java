package com.tngtech.archunit.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;

import static com.google.common.io.Resources.readLines;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a rule about a specified set of objects of interest (e.g. {@link JavaClass}).
 * To define a rule, use {@link Definition#all(ClassesTransformer)}, for example
 * <br/><br/><pre><code>
 * all(services).should(never(accessClassesThatResideIn("..ui..")).as("access the UI"))
 * </code></pre>
 * where '<code>services</code>' is (in this case just) a filter denoting when a {@link JavaClass} counts as service
 * (e.g. package contains 'svc', name ends in 'Service', ...).
 * <br/><br/>
 * {@link ClassesTransformer} in general defines how the type of objects
 * can be created from imported {@link JavaClasses}. It can filter, like in the example, or completely transform,
 * e.g. if you want to define a rule on all imported methods you could specify a transformer to retrieve methods
 * from classes, or if you're interested in slices of packages, the input transformer would specify how to transform
 * the imported classes to those slices to run an {@link ArchCondition} against.
 *
 * @see com.tngtech.archunit.library.dependencies.Slices.Transformer
 */
public interface ArchRule extends CanBeEvaluated {
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
                throw new ArchAssertionError(priority, message);
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

    class Definition<T> implements ArchRule {
        private final Priority priority;
        private final ClassesTransformer<T> classesTransformer;
        private final ArchCondition<T> condition;

        private Definition(InputDescription<T> inputDescription, ArchCondition<T> condition) {
            this.condition = condition;
            this.priority = inputDescription.priority;
            this.classesTransformer = inputDescription.classesTransformer;
        }

        @Override
        public void check(JavaClasses classes) {
            EvaluationResult result = evaluate(classes);
            Assertions.assertNoViolation(result, priority);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            condition.objectsToTest = classesTransformer.transform(classes);
            ConditionEvents events = new ConditionEvents();
            condition.check(events);
            return new EvaluationResult(this, events, priority);
        }

        @Override
        public String getDescription() {
            return ConfiguredMessageFormat.get().formatRuleText(condition.objectsToTest, condition);
        }

        /**
         * Takes an {@link ClassesTransformer} to specify how the set of objects of interest is to be created
         * from {@link JavaClasses} (which are the general input obtained from a {@link ClassFileImporter}).
         * The most simple {@link ClassesTransformer} is {@link #classes()}, which simply forwards the
         * {@link JavaClasses} as a collection of {@link JavaClass}.
         *
         * @param classesTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
         * @param <TYPE>             The target type to which the later used {@link ArchCondition ArchCondition&lt;TYPE&gt;}
         *                           will have to refer to
         * @return An {@link InputDescription OpenDescribable&lt;TYPE&gt;} to construct an {@link Definition ArchRule&lt;TYPE&gt;}
         */
        public static <TYPE> InputDescription<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
            return priority(MEDIUM).all(classesTransformer);
        }

        public static Creator priority(Priority priority) {
            return new Creator(priority);
        }

        public static ClassesTransformer<JavaClass> classes() {
            return new ClassesTransformer<JavaClass>("classes") {
                @Override
                public Iterable<JavaClass> doTransform(JavaClasses collection) {
                    return collection;
                }
            };
        }

        public static class Creator {
            private final Priority priority;

            private Creator(Priority priority) {
                this.priority = priority;
            }

            public <TYPE> InputDescription<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
                return new InputDescription<>(classesTransformer, priority);
            }
        }

        public static class InputDescription<TYPE> {
            final ClassesTransformer<TYPE> classesTransformer;
            final Priority priority;

            InputDescription(ClassesTransformer<TYPE> classesTransformer, Priority priority) {
                this.classesTransformer = classesTransformer;
                this.priority = priority;
            }

            public Definition<TYPE> should(ArchCondition<TYPE> condition) {
                return new Definition<>(this, condition);
            }
        }
    }
}
