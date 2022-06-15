/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@PublicAPI(usage = INHERITANCE)
public abstract class ArchCondition<T> {
    private final String description;

    public ArchCondition(String description, Object... args) {
        this.description = String.format(description, args);
    }

    /**
     * Can be used/overridden to prepare this condition with respect to the collection of all objects the condition
     * will be tested against.<br>
     * ArchUnit will call this method once in the beginning, before starting to check single items.
     *
     * @param allObjectsToTest All objects that {@link #check(Object, ConditionEvents)} will be called against
     */
    public void init(Collection<T> allObjectsToTest) {
    }

    public abstract void check(T item, ConditionEvents events);

    /**
     * Can be used/overridden to finish the evaluation of this condition.<br>
     * ArchUnit will call this method once after every single item was checked (by {@link #check(Object, ConditionEvents)}).<br>
     * This method can be used, if violations are dependent on multiple/all {@link #check(Object, ConditionEvents)} calls,
     * on the contrary to the default case, where each single {@link #check(Object, ConditionEvents)} stands for itself.
     */
    public void finish(ConditionEvents events) {
    }

    public ArchCondition<T> and(ArchCondition<? super T> condition) {
        return new AndCondition<>(this, condition.forSubtype());
    }

    public ArchCondition<T> or(ArchCondition<? super T> condition) {
        return new OrCondition<>(this, condition.forSubtype());
    }

    public String getDescription() {
        return description;
    }

    public ArchCondition<T> as(String description, Object... args) {
        return new ArchCondition<T>(description, args) {
            @Override
            public void init(Collection<T> allObjectsToTest) {
                ArchCondition.this.init(allObjectsToTest);
            }

            @Override
            public void check(T item, ConditionEvents events) {
                ArchCondition.this.check(item, events);
            }

            @Override
            public void finish(ConditionEvents events) {
                ArchCondition.this.finish(events);
            }
        };
    }

    public static <T extends HasDescription & HasSourceCodeLocation> ArchConditionBuilder<T> from( DescribedPredicate<T> predicate ) {
       return new ArchConditionBuilder( predicate );
    }

    public static class ArchConditionBuilder<T extends HasDescription & HasSourceCodeLocation> {
       private final DescribedPredicate predicate;

       private ArchConditionBuilder(DescribedPredicate predicate) {
          this.predicate = predicate;
       }

       public ArchCondition<T> describeViolatedAs(Function<T, String> singleFailureTextDescriptor) {
          return new ConditionByPredicate<T>( predicate, singleFailureTextDescriptor );
          //TODO: decide whether to return instantly or allow further customizations
       }

       public ArchCondition<T> withViolationString(final String singularViolation) {
          return new ConditionByPredicate<T>(predicate, c -> c.getDescription() + " " + singularViolation + " " + predicate.getDescription() );
          //TODO: decide whether to return instantly or allow further customizations
       }

       private static class ConditionByPredicate<T extends HasDescription & HasSourceCodeLocation> extends ArchCondition<T> {
          private final DescribedPredicate<T> predicate;
          private final Function<T, String> singleFailureTextDescriptor;

          ConditionByPredicate(DescribedPredicate<? super T> predicate, Function<T, String> singleFailureTextDescriptor) {
             super(predicate.getDescription());
             this.predicate = predicate.forSubtype();
             this.singleFailureTextDescriptor = singleFailureTextDescriptor;
          }

          @Override
          public void check(T object, ConditionEvents events) {
             boolean satisfied = predicate.test(object);
             String message = singleFailureTextDescriptor.apply(object) + " in " + object.getSourceCodeLocation();
             events.add(new SimpleConditionEvent(object, satisfied, message));
          }
       }
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @SuppressWarnings("unchecked") // Cast is safe since input parameter is contravariant
    public <U extends T> ArchCondition<U> forSubtype() {
        return (ArchCondition<U>) this;
    }

    private abstract static class JoinCondition<T> extends ArchCondition<T> {
        private final Collection<ArchCondition<T>> conditions;

        private JoinCondition(String infix, Collection<ArchCondition<T>> conditions) {
            super(joinDescriptionsOf(infix, conditions));
            this.conditions = conditions;
        }

        private static <T> String joinDescriptionsOf(String infix, Collection<ArchCondition<T>> conditions) {
            return conditions.stream().map(ArchCondition::getDescription).collect(joining(" " + infix + " "));
        }

        @Override
        public void init(Collection<T> allObjectsToTest) {
            for (ArchCondition<T> condition : conditions) {
                condition.init(allObjectsToTest);
            }
        }

        @Override
        public void finish(ConditionEvents events) {
            for (ArchCondition<T> condition : conditions) {
                condition.finish(events);
            }
        }

        List<ConditionWithEvents<T>> evaluateConditions(T item) {
            return conditions.stream().map(condition -> new ConditionWithEvents<>(condition, item)).collect(toList());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + conditions + "}";
        }
    }

    private static class ConditionWithEvents<T> {
        private final ArchCondition<T> condition;
        private final ConditionEvents events;

        ConditionWithEvents(ArchCondition<T> condition, T item) {
            this(condition, check(condition, item));
        }

        ConditionWithEvents(ArchCondition<T> condition, ConditionEvents events) {
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

    private abstract static class JoinConditionEvent<T> implements ConditionEvent {
        final T correspondingObject;
        final List<ConditionWithEvents<T>> evaluatedConditions;

        JoinConditionEvent(T correspondingObject, List<ConditionWithEvents<T>> evaluatedConditions) {
            this.correspondingObject = correspondingObject;
            this.evaluatedConditions = evaluatedConditions;
        }

        List<String> getUniqueLinesOfViolations() { // TODO: Sort by line number, then lexicographically
            final Set<String> result = new TreeSet<>();
            for (ConditionWithEvents<T> evaluation : evaluatedConditions) {
                for (ConditionEvent event : evaluation.events) {
                    if (event.isViolation()) {
                        result.addAll(event.getDescriptionLines());
                    }
                }
            }
            return ImmutableList.copyOf(result);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("evaluatedConditions", evaluatedConditions)
                    .toString();
        }

        List<ConditionWithEvents<T>> invert(List<ConditionWithEvents<T>> evaluatedConditions) {
            return evaluatedConditions.stream().map(this::invert).collect(toList());
        }

        private ConditionWithEvents<T> invert(ConditionWithEvents<T> evaluation) {
            ConditionEvents invertedEvents = new ConditionEvents();
            for (ConditionEvent event : evaluation.events) {
                event.addInvertedTo(invertedEvents);
            }
            return new ConditionWithEvents<>(evaluation.condition, invertedEvents);
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
        AndConditionEvent(T item, List<ConditionWithEvents<T>> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            return evaluatedConditions.stream().anyMatch(evaluation -> evaluation.events.containViolation());
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new OrConditionEvent<>(correspondingObject, invert(evaluatedConditions)));
        }

        @Override
        public List<String> getDescriptionLines() {
            return getUniqueLinesOfViolations();
        }

        @Override
        public void handleWith(final Handler handler) {
            for (ConditionWithEvents<T> condition : evaluatedConditions) {
                condition.events.handleViolations(handler::handle);
            }
        }
    }

    private static class OrConditionEvent<T> extends JoinConditionEvent<T> {
        OrConditionEvent(T item, List<ConditionWithEvents<T>> evaluatedConditions) {
            super(item, evaluatedConditions);
        }

        @Override
        public boolean isViolation() {
            return evaluatedConditions.stream().allMatch(evaluation -> evaluation.events.containViolation());
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new AndConditionEvent<>(correspondingObject, invert(evaluatedConditions)));
        }

        @Override
        public List<String> getDescriptionLines() {
            return ImmutableList.of(createMessage());
        }

        private String createMessage() {
            return Joiner.on(" and ").join(getUniqueLinesOfViolations());
        }

        @Override
        public void handleWith(final Handler handler) {
            handler.handle(Collections.singleton(correspondingObject), createMessage());
        }
    }
}
