package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public ArchCondition<T> and(ArchCondition<T> condition) {
        return new AndCondition<>(this, condition);
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

    private static abstract class JoinCondition<T> extends ArchCondition<T> {
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

    private static abstract class JoinConditionEvent implements ConditionEvent {
        final List<ConditionWithEvents> evaluatedConditions;

        JoinConditionEvent(List<ConditionWithEvents> evaluatedConditions) {
            this.evaluatedConditions = evaluatedConditions;
        }

        @Override
        public void describeTo(CollectsLines messages) {
            for (ConditionWithEvents evaluation : evaluatedConditions) {
                for (ConditionEvent event : evaluation.events) {
                    if (event.isViolation()) {
                        event.describeTo(messages);
                    }
                }
            }
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
            events.add(new AndConditionEvent(evaluateConditions(item)));
        }
    }

    private static class AndConditionEvent extends JoinConditionEvent {
        AndConditionEvent(List<ConditionWithEvents> evaluatedConditions) {
            super(evaluatedConditions);
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
            events.add(new OrConditionEvent(invert(evaluatedConditions)));
        }
    }

    private static class OrConditionEvent extends JoinConditionEvent {
        OrConditionEvent(List<ConditionWithEvents> evaluatedConditions) {
            super(evaluatedConditions);
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
            events.add(new AndConditionEvent(invert(evaluatedConditions)));
        }
    }
}
