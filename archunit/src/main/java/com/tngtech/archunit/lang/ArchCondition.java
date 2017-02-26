package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ArchCondition<T> {
    private String description;

    public ArchCondition(String description) {
        this.description = checkNotNull(description);
    }

    /**
     * Can be used to prepare this condition with respect to the collection of all objects the condition
     * will be tested against.
     *
     * @param allObjectsToTest All objects that {@link #check(Object, ConditionEvents)} will be called against
     */
    public void init(Iterable<T> allObjectsToTest) {
    }

    public abstract void check(T item, ConditionEvents events);

    public ArchCondition<T> and(ArchCondition<? super T> condition) {
        return new AndCondition<>(this, condition.<T>forSubType());
    }

    public ArchCondition<T> or(ArchCondition<? super T> condition) {
        return new OrCondition<>(this, condition.<T>forSubType());
    }

    public String getDescription() {
        return description;
    }

    public ArchCondition<T> as(String description, Object... args) {
        return new ArchCondition<T>(String.format(description, args)) {
            @Override
            public void init(Iterable<T> allObjectsToTest) {
                ArchCondition.this.init(allObjectsToTest);
            }

            @Override
            public void check(T item, ConditionEvents events) {
                ArchCondition.this.check(item, events);
            }
        };
    }

    @SuppressWarnings("unchecked") // Cast is safe since input parameter is contravariant
    public <U extends T> ArchCondition<U> forSubType() {
        return (ArchCondition<U>) this;
    }

    private abstract static class JoinCondition<T> extends ArchCondition<T> {
        private final Collection<ArchCondition<T>> conditions;

        private JoinCondition(String infix, Collection<ArchCondition<T>> conditions) {
            super(joinDescriptionsOf(infix, conditions));
            this.conditions = conditions;
        }

        private static <T> String joinDescriptionsOf(String infix, Collection<ArchCondition<T>> conditions) {
            List<String> descriptions = new ArrayList<>();
            for (ArchCondition<T> condition : conditions) {
                descriptions.add(condition.getDescription());
            }
            return Joiner.on(" " + infix + " ").join(descriptions);
        }

        @Override
        public void init(Iterable<T> allObjectsToTest) {
            for (ArchCondition<T> condition : conditions) {
                condition.init(allObjectsToTest);
            }
        }

        List<ConditionWithEvents> evaluateConditions(T item) {
            List<ConditionWithEvents> evaluate = new ArrayList<>();
            for (ArchCondition<T> condition : conditions) {
                evaluate.add(new ConditionWithEvents(condition, item));
            }
            return evaluate;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + conditions + "}";
        }
    }

    private static class ConditionWithEvents {
        private final ArchCondition<?> condition;
        private final ConditionEvents events;

        <T> ConditionWithEvents(ArchCondition<T> condition, T item) {
            this(condition, check(condition, item));
        }

        ConditionWithEvents(ArchCondition<?> condition, ConditionEvents events) {
            this.condition = condition;
            this.events = events;
        }

        private static <T> ConditionEvents check(ArchCondition<T> condition, T item) {
            ConditionEvents events = new ConditionEvents();
            condition.check(item, events);
            return events;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("condition", condition)
                    .add("events", events)
                    .toString();
        }
    }

    private abstract static class JoinConditionEvent<T> implements ConditionEvent<T> {
        private final T correspondingObject;
        final List<ConditionWithEvents> evaluatedConditions;

        JoinConditionEvent(T correspondingObject, List<ConditionWithEvents> evaluatedConditions) {
            this.correspondingObject = correspondingObject;
            this.evaluatedConditions = evaluatedConditions;
        }

        Set<String> getUniqueLinesOfViolations() { // FIXME: Sort by line number, then lexicographically
            final Set<String> result = new TreeSet<>();
            CollectsLines lines = new CollectsLines() {
                @Override
                public void add(String line) {
                    result.add(line);
                }
            };
            for (ConditionWithEvents evaluation : evaluatedConditions) {
                for (ConditionEvent event : evaluation.events) {
                    if (event.isViolation()) {
                        event.describeTo(lines);
                    }
                }
            }
            return result;
        }

        @Override
        public T getCorrespondingObject() {
            return correspondingObject;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("evaluatedConditions", evaluatedConditions)
                    .toString();
        }

        List<ConditionWithEvents> invert(List<ConditionWithEvents> evaluatedConditions) {
            List<ConditionWithEvents> inverted = new ArrayList<>();
            for (ConditionWithEvents evaluation : evaluatedConditions) {
                inverted.add(invert(evaluation));
            }
            return inverted;
        }

        ConditionWithEvents invert(ConditionWithEvents evaluation) {
            ConditionEvents invertedEvents = new ConditionEvents();
            for (ConditionEvent event : evaluation.events) {
                event.addInvertedTo(invertedEvents);
            }
            return new ConditionWithEvents(evaluation.condition, invertedEvents);
        }
    }

    private static class AndCondition<T> extends JoinCondition<T> {
        private AndCondition(ArchCondition<T> first, ArchCondition<T> second) {
            super("and", ImmutableList.of(first, second));
        }

        @Override
        public void check(T item, ConditionEvents events) {
            events.add(new AndConditionEvent<>(item, evaluateConditions(item)));
        }
    }

    private static class OrCondition<T> extends JoinCondition<T> {
        private OrCondition(ArchCondition<T> first, ArchCondition<T> second) {
            super("or", ImmutableList.of(first, second));
        }

        @Override
        public void check(T item, ConditionEvents events) {
            events.add(new OrConditionEvent<>(item, evaluateConditions(item)));
        }
    }

    private static class AndConditionEvent<T> extends JoinConditionEvent<T> {
        AndConditionEvent(T item, List<ConditionWithEvents> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            for (ConditionWithEvents evaluation : evaluatedConditions) {
                if (evaluation.events.containViolation()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new OrConditionEvent<>(getCorrespondingObject(), invert(evaluatedConditions)));
        }

        @Override
        public void describeTo(CollectsLines lines) {
            for (String line : getUniqueLinesOfViolations()) {
                lines.add(line);
            }
        }
    }

    private static class OrConditionEvent<T> extends JoinConditionEvent<T> {
        OrConditionEvent(T item, List<ConditionWithEvents> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            for (ConditionWithEvents evaluation : evaluatedConditions) {
                if (!evaluation.events.containViolation()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new AndConditionEvent<>(getCorrespondingObject(), invert(evaluatedConditions)));
        }

        @Override
        public void describeTo(CollectsLines lines) {
            lines.add(Joiner.on(" and ").join(getUniqueLinesOfViolations()));
        }
    }
}
