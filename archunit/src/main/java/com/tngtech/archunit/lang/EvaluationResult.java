/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.Convertible;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.io.Resources.readLines;
import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.ClassLoaders.getCurrentClassLoader;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Represents the result of evaluating an {@link ArchRule} against some {@link JavaClasses}.
 * To react to failures during evaluation of the rule, one can use {@link #handleViolations(ViolationHandler, Object[])}:
 * <br><br>
 * <pre><code>
 * result.handleViolations((Collection&lt;JavaAccess&lt;?&gt;&gt; violatingObjects, String message) -> {
 *     // do some reporting or react in any way to violation
 * });
 * </code></pre>
 */
@PublicAPI(usage = ACCESS)
public final class EvaluationResult {
    static final String ARCHUNIT_IGNORE_PATTERNS_FILE_NAME = "archunit_ignore_patterns.txt";
    private static final String COMMENT_LINE_PREFIX = "#";

    private final HasDescription rule;
    private final ArrayList<ConditionEvent> violations;
    private final Optional<String> informationAboutNumberOfViolations;
    private final Priority priority;

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, Priority priority) {
        this(rule, new ArrayList<>(), Optional.empty(), priority);
    }

    @PublicAPI(usage = ACCESS)
    public EvaluationResult(HasDescription rule, ConditionEvents events, Priority priority) {
        this(
                rule,
                new ArrayList<>(events.getViolating()),
                events.getInformationAboutNumberOfViolations(),
                priority
        );
    }

    private EvaluationResult(HasDescription rule, ArrayList<ConditionEvent> violations, Optional<String> informationAboutNumberOfViolations, Priority priority) {
        this.rule = rule;
        this.violations = createViolations(violations);
        this.informationAboutNumberOfViolations = informationAboutNumberOfViolations;
        this.priority = priority;
    }

    @PublicAPI(usage = ACCESS)
    public FailureReport getFailureReport() {
        ImmutableList<String> result = violations.stream()
                .flatMap(event -> event.getDescriptionLines().stream())
                .sorted(natural())
                .collect(toImmutableList());
        FailureMessages failureMessages = new FailureMessages(result, informationAboutNumberOfViolations);
        return new FailureReport(rule, priority, failureMessages);
    }

    @PublicAPI(usage = ACCESS)
    public void add(EvaluationResult part) {
        violations.addAll(part.violations);
    }

    /**
     * Passes violations to the supplied {@link ViolationHandler}. The passed violations will automatically
     * be filtered by the type of the given {@link ViolationHandler}. That is, when a
     * <code>ViolationHandler&lt;SomeClass&gt;</code> is passed, only violations by objects assignable to
     * <code>SomeClass</code> will be reported. Note that this will be unsafe for generics, i.e. ArchUnit
     * cannot filter to match the full generic type signature. E.g.
     * <pre><code>
     * handleViolations((Collection&lt;Optional&lt;String&gt;&gt; objects, String message) ->
     *     assertType(objects.iterator().next().get(), String.class)
     * )
     * </code></pre>
     * might throw an exception if there are also {@code Optional<Integer>} violations.
     * So, in general it is safer to use the wildcard {@code ?} for generic types, unless it is absolutely
     * certain from the context what the type parameter will be
     * (for example when only analyzing methods it might be clear that the type parameter will be {@link JavaMethod}).
     * <br><br>
     * For any {@link ViolationHandler ViolationHandler&lt;T&gt;} violating objects that are not of type <code>T</code>,
     * but implement {@link Convertible} will be {@link Convertible#convertTo(Class) converted} to <code>T</code>
     * and the result will be passed on to the {@link ViolationHandler}. This makes sense for example for a client
     * who wants to handle {@link Dependency}, but the {@link ConditionEvents} corresponding objects are of type
     * {@link JavaAccess} which does not share any common meaningful type.
     *
     * @param <T> Type of the relevant objects causing violations. E.g. {@code JavaAccess<?>}
     * @param violationHandler The violation handler that is supposed to handle all violations matching the
     *                         respective type parameter
     * @param __ignored_parameter_to_reify_type__ This parameter will be ignored; its only use is to make the
     *                                            generic type reified, so we can retrieve it at runtime.
     *                                            Without this parameter, the generic type would be erased.
     */
    @SafeVarargs
    @SuppressWarnings("unused")
    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public final <T> void handleViolations(ViolationHandler<T> violationHandler, T... __ignored_parameter_to_reify_type__) {
        Class<T> correspondingObjectType = componentTypeOf(__ignored_parameter_to_reify_type__);
        ConditionEvent.Handler eventHandler = convertToEventHandler(correspondingObjectType, violationHandler);
        for (ConditionEvent event : violations) {
            event.handleWith(eventHandler);
        }
    }

    @SuppressWarnings("unchecked") // The cast is safe, since the component type of T[] will be type T
    private <T> Class<T> componentTypeOf(T[] array) {
        return (Class<T>) array.getClass().getComponentType();
    }

    private <ITEM> ConditionEvent.Handler convertToEventHandler(Class<? extends ITEM> correspondingObjectType, ViolationHandler<ITEM> violationHandler) {
        return (correspondingObjects, message) -> {
            Collection<ITEM> collection = getObjectsToHandle(correspondingObjects, correspondingObjectType);
            if (!collection.isEmpty()) {
                violationHandler.handle(collection, message);
            }
        };
    }

    @SuppressWarnings("unchecked") // compatibility asserted via reflection
    private <T> Collection<T> getObjectsToHandle(Collection<?> objects, Class<? extends T> supportedType) {
        Set<T> result = new HashSet<>();
        for (Object object : objects) {
            if (supportedType.isInstance(object)) {
                result.add((T) object);
            } else if (object instanceof Convertible) {
                result.addAll(((Convertible) object).convertTo(supportedType));
            }
        }
        return result;
    }

    @PublicAPI(usage = ACCESS)
    public boolean hasViolation() {
        return !violations.isEmpty();
    }

    @PublicAPI(usage = ACCESS)
    public Priority getPriority() {
        return priority;
    }

    /**
     * Filters all recorded {@link ConditionEvent ConditionEvents} by their textual description.
     * I.e. the lines of the description of an event are passed to the supplied predicate to
     * decide if the event is relevant.
     * @param linePredicate A predicate to determine which lines of events match. Predicate.test(..) == true will imply the violation will be preserved.
     * @return A new {@link EvaluationResult} containing only matching events
     */
    @PublicAPI(usage = ACCESS)
    public EvaluationResult filterDescriptionsMatching(Predicate<String> linePredicate) {
        ArrayList<ConditionEvent> filtered = filterEvents(violations, linePredicate);
        return new EvaluationResult(rule, filtered, Optional.empty(), priority);
    }

    private static ArrayList<ConditionEvent> filterEvents(Collection<ConditionEvent> violations, Predicate<String> linePredicate) {
        return violations.stream()
                .map(e -> new FilteredEvent(e, linePredicate))
                .filter(FilteredEvent::isViolation)
                .collect(toCollection(ArrayList::new));
    }

    private static ArrayList<ConditionEvent> createViolations(ArrayList<ConditionEvent> violations) {
        Set<Pattern> patterns = readPatternsFrom(ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
        return patterns.isEmpty() ? violations : filterEvents(violations, notMatchedByAny(patterns));
    }

    private static Predicate<String> notMatchedByAny(Set<Pattern> patterns) {
        return message -> {
            String normalizedMessage = message.replaceAll("\r*\n", " ");
            return patterns.stream().noneMatch(pattern -> pattern.matcher(normalizedMessage).matches());
        };
    }

    private static Set<Pattern> readPatternsFrom(String fileNameInClassPath) {
        URL ignorePatternsResource = getCurrentClassLoader(ArchRule.Assertions.class).getResource(fileNameInClassPath);
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

    private static class FilteredEvent implements ConditionEvent {
        private final ConditionEvent delegate;
        private final Predicate<String> linePredicate;
        private final List<String> filteredDescriptionLines;

        private FilteredEvent(ConditionEvent delegate, Predicate<String> linePredicate) {
            this.delegate = delegate;
            this.linePredicate = linePredicate;
            filteredDescriptionLines = delegate.getDescriptionLines().stream().filter(linePredicate).collect(toList());
        }

        @Override
        public boolean isViolation() {
            return delegate.isViolation() && !getDescriptionLines().isEmpty();
        }

        @Override
        public ConditionEvent invert() {
            return new FilteredEvent(delegate.invert(), linePredicate);
        }

        @Override
        public List<String> getDescriptionLines() {
            return filteredDescriptionLines;
        }

        @Override
        public void handleWith(Handler handler) {
            delegate.handleWith(new FilteredHandler(handler, linePredicate));
        }
    }

    private static class FilteredHandler implements ConditionEvent.Handler {
        private final ConditionEvent.Handler delegate;
        private final Predicate<String> linePredicate;

        private FilteredHandler(ConditionEvent.Handler delegate, Predicate<String> linePredicate) {
            this.delegate = delegate;
            this.linePredicate = linePredicate;
        }

        @Override
        public void handle(Collection<?> correspondingObjects, String message) {
            if (linePredicate.test(message)) {
                delegate.handle(correspondingObjects, message);
            }
        }
    }
}
