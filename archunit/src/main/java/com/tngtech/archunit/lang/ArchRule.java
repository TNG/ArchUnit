package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ClosedArchRule.ClosedDescribable;
import com.tngtech.archunit.lang.OpenArchRule.OpenDescribable;

import static com.tngtech.archunit.lang.Priority.MEDIUM;

/**
 * Base class for all rules about a specified set of objects of interest
 * (e.g. {@link com.tngtech.archunit.core.JavaClass}). Static factory methods allow either writing a rule
 * on the fly (e.g. in an unit test), or just specifying a rule to be applied to different sets of classes later.
 * To write a rule on the fly (assuming the classes to analyse are already available), use
 * {@link #all(Iterable objects)}, for example
 * <br/><br/><pre><code>
 * all(classes).should("do something").assertedBy(conditionThatDetectsThis)
 * </code></pre>
 * If you want to define a rule for later use, use {@link #all(InputTransformer)},
 * for example
 * <br/><br/><pre><code>
 * all(services).should("not access the ui").assertedBy(conditionThatDetectsThis)
 * </code></pre>
 * where '<code>services</code>' is a filter denoting when a {@link com.tngtech.archunit.core.JavaClass} counts as service
 * (e.g. package contains 'svc', name ends in 'Service', ...).
 * <br/><br/>
 * {@link InputTransformer} defines how the type of objects
 * can be created from imported {@link JavaClasses}, e.g. if you want to define a rule
 * on slices of packages, the input transformer would specify how the transform the imported classes to those slices
 * to run the rule against. It can also just transform {@link JavaClasses} by filtering a subset of interest.
 *
 * @param <T> The type of objects the rule applies to
 * @see ClosedArchRule
 * @see OpenArchRule
 */
public abstract class ArchRule<T> {
    private final String text;
    final ArchCondition<T> condition;

    ArchRule(String text, ArchCondition<T> condition) {
        this.text = text;
        this.condition = condition;
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

    public static <TYPE, ITERABLE extends Iterable<TYPE> & HasDescription>
    ClosedDescribable<TYPE, ITERABLE> all(ITERABLE iterable) {
        return priority(MEDIUM).all(iterable);
    }

    /**
     * Takes an {@link InputTransformer} to specify how the set of objects of interest is to be created
     * from {@link JavaClasses} (which are the general input obtained from a
     * {@link com.tngtech.archunit.core.ClassFileImporter ClassFileImporter}). The most simple {@link InputTransformer}
     * is {@link #classes()}, which simply forwards the {@link JavaClasses} as a collection of {@link JavaClass}.
     *
     * @param inputTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
     * @param <TYPE>           The target type to which the {@link ArchCondition ArchCondition&lt;TYPE&gt;} will refer to
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

        public <TYPE, ITERABLE extends Iterable<TYPE> & HasDescription>
        ClosedDescribable<TYPE, ITERABLE> all(ITERABLE iterable) {
            return new ClosedDescribable<>(iterable, priority);
        }

        public <TYPE> OpenDescribable<TYPE> all(InputTransformer<TYPE> inputTransformer) {
            return new OpenDescribable<>(inputTransformer, priority);
        }
    }
}
