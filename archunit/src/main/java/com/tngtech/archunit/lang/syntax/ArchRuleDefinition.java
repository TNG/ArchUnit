package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;

import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;

public class ArchRuleDefinition<T> {
    private ArchRuleDefinition() {
    }

    /**
     * @see Creator#all(ClassesTransformer)
     */
    public static <TYPE> InputDescription<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).all(classesTransformer);
    }

    /**
     * @see Creator#no(ClassesTransformer)
     */
    public static <TYPE> InputDescription<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).no(classesTransformer);
    }

    public static Creator priority(Priority priority) {
        return new Creator(priority);
    }

    public static ClassesTransformer<JavaClass> classes() {
        return new AbstractClassesTransformer<JavaClass>("classes") {
            @Override
            public Iterable<JavaClass> doTransform(JavaClasses collection) {
                return collection;
            }
        };
    }

    public static GivenClasses allClasses() {
        return priority(MEDIUM).allClasses();
    }

    public static GivenClasses noClasses() {
        return priority(MEDIUM).noClasses();
    }

    public static class Creator {
        private final Priority priority;

        private Creator(Priority priority) {
            this.priority = priority;
        }

        public GivenClasses allClasses() {
            return new GivenClassesInternal(priority, classes());
        }

        public GivenClasses noClasses() {
            return new GivenClassesInternal(priority, classes().as("no classes"), new Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>>() {
                @Override
                public ArchCondition<JavaClass> apply(final ArchCondition<JavaClass> condition) {
                    return never(condition).as(condition.getDescription());
                }
            });
        }

        /**
         * Takes a {@link ClassesTransformer} to specify how the set of objects of interest is to be created
         * from {@link JavaClasses} (which are the general input obtained from a {@link ClassFileImporter}).
         * The most simple {@link ClassesTransformer} is {@link #classes()}, which simply forwards the
         * {@link JavaClasses} as a collection of {@link JavaClass}.
         *
         * @param classesTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
         * @param <TYPE>             The target type to which the later used {@link ArchCondition ArchCondition&lt;TYPE&gt;}
         *                           will have to refer to
         * @return An {@link InputDescription OpenDescribable&lt;TYPE&gt;} to construct an {@link ArchRuleDefinition ArchRule&lt;TYPE&gt;}
         */
        public <TYPE> InputDescription<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
            return new InputDescription<>(classesTransformer, priority);
        }

        /**
         * Same as {@link #all(ClassesTransformer)}, but negates the following condition.
         */
        public <TYPE> InputDescription<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
            return new InputDescription<TYPE>(classesTransformer.as("no " + classesTransformer.getDescription()), priority) {
                @Override
                public ArchRule should(ArchCondition<TYPE> condition) {
                    return super.should(never(condition).as(condition.getDescription()));
                }
            };
        }
    }

    public static class InputDescription<TYPE> {
        final ClassesTransformer<TYPE> classesTransformer;
        final Priority priority;

        private InputDescription(ClassesTransformer<TYPE> classesTransformer, Priority priority) {
            this.classesTransformer = classesTransformer;
            this.priority = priority;
        }

        public ArchRule should(ArchCondition<TYPE> condition) {
            return ArchRule.Factory.create(classesTransformer, condition, priority);
        }
    }
}
