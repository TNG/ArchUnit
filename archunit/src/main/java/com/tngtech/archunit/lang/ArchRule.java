package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.DescribedIterable;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;

import static com.tngtech.archunit.lang.Priority.MEDIUM;

/**
 * Base class for all rules about a specified set of objects of interest
 * (e.g. {@link JavaClass}).
 * To define a rule, use {@link #all(InputTransformer)}, for example
 * <br/><br/><pre><code>
 * all(services).should("not access the ui").assertedBy(conditionThatDetectsThis)
 * </code></pre>
 * where '<code>services</code>' is a filter denoting when a {@link JavaClass} counts as service
 * (e.g. package contains 'svc', name ends in 'Service', ...).
 * <br/><br/>
 * {@link InputTransformer} defines how the type of objects
 * can be created from imported {@link JavaClasses}, e.g. if you want to define a rule
 * on slices of packages, the input transformer would specify how the transform the imported classes to those slices
 * to run the rule against. It can also just transform {@link JavaClasses} by filtering a subset of interest.
 *
 * @param <T> The type of objects the rule applies to
 */
public class ArchRule<T> {
    private Priority priority;
    private InputTransformer<T> inputTransformer;
    private final String text;
    private final ArchCondition<T> condition;

    ArchRule(String text, ArchCondition<T> condition) {
        this.text = text;
        this.condition = condition;
    }

    ArchRule(OpenDescribable<T> describable, ArchCondition<T> condition) {
        this(condition.getDescription(), condition);
        this.priority = describable.priority;
        this.inputTransformer = describable.inputTransformer;
    }

    public void check(JavaClasses classes) {
        DescribedIterable<T> describedCollection = inputTransformer.transform(classes);
        String completeRuleText = String.format("%s should %s", describedCollection.getDescription(), condition.getDescription());
        condition.objectsToTest = describedCollection;
        ClosedArchRule<?> rule = new ClosedArchRule<>(describedCollection, completeRuleText, condition);
        ArchRuleAssertion.from(rule).assertNoViolations(priority);
    }

    void evaluate(Iterable<T> objectsToTest, ConditionEvents events) {
        for (T object : objectsToTest) {
            condition.check(object, events);
        }
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Takes an {@link InputTransformer} to specify how the set of objects of interest is to be created
     * from {@link JavaClasses} (which are the general input obtained from a {@link ClassFileImporter}).
     * The most simple {@link InputTransformer} is {@link #classes()}, which simply forwards the
     * {@link JavaClasses} as a collection of {@link JavaClass}.
     *
     * @param inputTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
     * @param <TYPE>           The target type to which the later used {@link ArchCondition ArchCondition&lt;TYPE&gt;}
     *                         will have to refer to
     * @return An {@link OpenDescribable OpenDescribable&lt;TYPE&gt;} to construct an {@link ArchRule ArchRule&lt;TYPE&gt;}
     */
    public static <TYPE> OpenDescribable<TYPE> all(InputTransformer<TYPE> inputTransformer) {
        return priority(MEDIUM).all(inputTransformer);
    }

    public static Creator priority(Priority priority) {
        return new Creator(priority);
    }

    public static InputTransformer<JavaClass> classes() {
        return new InputTransformer<JavaClass>("classes") {
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

        public <TYPE> OpenDescribable<TYPE> all(InputTransformer<TYPE> inputTransformer) {
            return new OpenDescribable<>(inputTransformer, priority);
        }
    }

    public static class OpenDescribable<TYPE> {
        final InputTransformer<TYPE> inputTransformer;
        final Priority priority;

        OpenDescribable(InputTransformer<TYPE> inputTransformer, Priority priority) {
            this.inputTransformer = inputTransformer;
            this.priority = priority;
        }

        public ArchRule<TYPE> should(ArchCondition<TYPE> condition) {
            return new ArchRule<>(this, condition);
        }
    }

    static final class ClosedArchRule<T> extends ArchRule<T> implements RuleToEvaluate {
        private final Iterable<T> objectsToTest;

        ClosedArchRule(Iterable<T> objectsToTest, String text, ArchCondition<T> condition) {
            super(text, condition);
            this.objectsToTest = objectsToTest;
        }

        @Override
        public ConditionEvents evaluate() {
            ConditionEvents events = new ConditionEvents();
            super.evaluate(objectsToTest, events);
            return events;
        }

        @Override
        public String getDescription() {
            return toString();
        }
    }
}
