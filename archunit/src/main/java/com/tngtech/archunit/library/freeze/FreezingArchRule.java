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
package com.tngtech.archunit.library.freeze;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.library.freeze.ViolationStoreFactory.FREEZE_STORE_PROPERTY_NAME;

/**
 * A decorator around an existing {@link ArchRule} that "freezes" the state of all violations on the first call instead of failing the test.
 * This means in particular that the first run of a {@link FreezingArchRule} will always pass.
 * Consecutive calls will only fail if "unknown" violations are introduced (read below for further explanations when a violation is "unknown").
 * Once resolved, initially "known" violations will fail again if they were re-introduced.
 * <br><br>
 * You might consider using this class when introducing a new {@link ArchRule} to an existing project that causes too many violations to solve
 * at the current time. A typical example is a huge legacy project where a new rule might cause thousands of violations. Even if it is impossible
 * to fix all those violations at the moment, it is typically a good idea to a) make sure no further violations are introduced and
 * b) incrementally fix those violations over time one by one.
 * <br><br>
 * {@link FreezingArchRule} uses two concepts to support this use case:
 * <ul>
 * <li>
 *   a {@link ViolationStore} to store the result of the current evaluation and retrieve the result of the previous evaluation of this rule.<br>
 *   The default {@link ViolationStore} stores violations in plain text files
 *   within the path specified by {@code freeze.store.default.path} of
 *   {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME} (default: {@code archunit_store})<br>
 *   A custom implementation can be provided in two ways.
 *   Either programmatically via {@link #persistIn(ViolationStore)}, or by specifying the fully qualified class name within
 *   {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}, e.g.
 *   <pre><code>freeze.store=com.fully.qualified.MyViolationStore</code></pre>
 * </li>
 * <li>
 *   a {@link ViolationLineMatcher} to decide which violations are "known", i.e. have already been present in the previous evaluation.<br>
 *   The default {@link ViolationLineMatcher} compares violations ignoring the line number of their source code location
 *   and auto-generated numbers of anonymous classes or lambda expressions.<br>
 *   A custom implementation can be configured in two ways.
 *   Again either programmatically via {@link #associateViolationLinesVia(ViolationLineMatcher)}, or within
 *   {@value com.tngtech.archunit.ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}, e.g.
 *   <pre><code>freeze.lineMatcher=com.fully.qualified.MyViolationLineMatcher</code></pre>
 * </li>
 * </ul>
 */
@PublicAPI(usage = ACCESS)
public final class FreezingArchRule implements ArchRule {
    private static final Logger log = LoggerFactory.getLogger(FreezingArchRule.class);

    private final ArchRule delegate;
    private final ViolationStore store;
    private final ViolationLineMatcher matcher;

    private FreezingArchRule(ArchRule delegate, ViolationStore store, ViolationLineMatcher matcher) {
        this.delegate = checkNotNull(delegate);
        this.store = checkNotNull(store);
        this.matcher = checkNotNull(matcher);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public void check(JavaClasses classes) {
        Assertions.check(this, classes);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public FreezingArchRule because(String reason) {
        return new FreezingArchRule(delegate.because(reason), store, matcher);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public FreezingArchRule as(String newDescription) {
        return new FreezingArchRule(delegate.as(newDescription), store, matcher);
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public EvaluationResult evaluate(JavaClasses classes) {
        store.initialize(ArchConfiguration.get().getSubProperties(FREEZE_STORE_PROPERTY_NAME));

        EvaluationResult result = delegate.evaluate(classes);
        if (!store.contains(delegate)) {
            return storeViolationsAndReturnSuccess(result);
        } else {
            return removeObsoleteViolationsFromStoreAndReturnNewViolations(result);
        }
    }

    private EvaluationResult storeViolationsAndReturnSuccess(EvaluationResult result) {
        log.debug("No results present for rule '{}'. Freezing rule result...", delegate.getDescription());
        store.save(delegate, result.getFailureReport().getDetails());
        return new EvaluationResult(delegate, result.getPriority());
    }

    private EvaluationResult removeObsoleteViolationsFromStoreAndReturnNewViolations(EvaluationResult result) {
        log.debug("Found frozen result for rule '{}'", delegate.getDescription());
        final List<String> knownViolations = store.getViolations(delegate);
        CategorizedViolations categorizedViolations = new CategorizedViolations(matcher, result, knownViolations);
        removeObsoleteViolationsFromStore(categorizedViolations);
        return filterOutKnownViolations(result, categorizedViolations.getKnownActualViolations());
    }

    private void removeObsoleteViolationsFromStore(CategorizedViolations categorizedViolations) {
        List<String> solvedViolations = categorizedViolations.getStoredSolvedViolations();
        log.debug("Removing {} obsolete violations from store: {}", solvedViolations.size(), solvedViolations);
        if (!solvedViolations.isEmpty()) {
            store.save(delegate, categorizedViolations.getStoredUnsolvedViolations());
        }
    }

    private EvaluationResult filterOutKnownViolations(EvaluationResult result, final Set<String> knownActualViolations) {
        log.debug("Filtering out known violations: {}", knownActualViolations);
        return result.filterDescriptionsMatching(new Predicate<String>() {
            @Override
            public boolean apply(String violation) {
                return !knownActualViolations.contains(violation);
            }
        });
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return delegate.getDescription();
    }

    /**
     * Allows to reconfigure the {@link ViolationStore} to use. The {@link ViolationStore} will be used to store the initial state of a
     * {@link FreezingArchRule} and update this state on further evaluation of this rule.
     *
     * @param store The {@link ViolationStore} to use
     * @return An adjusted {@link FreezingArchRule} which will store violations in the passed {@link ViolationStore}

     * @see FreezingArchRule
     */
    @PublicAPI(usage = ACCESS)
    public FreezingArchRule persistIn(ViolationStore store) {
        return new FreezingArchRule(delegate, store, matcher);
    }

    /**
     * Allows to reconfigure how this {@link FreezingArchRule} will decide if an occurring violation is known or not.
     *
     * @param matcher A {@link ViolationLineMatcher} that decides which lines of a violation description are known and which are unknown and should
     *                cause a failure of this rule
     * @return An adjusted {@link FreezingArchRule} which will compare occurring violations to stored ones with the given {@link ViolationLineMatcher}

     * @see FreezingArchRule
     */
    @PublicAPI(usage = ACCESS)
    public FreezingArchRule associateViolationLinesVia(ViolationLineMatcher matcher) {
        return new FreezingArchRule(delegate, store, matcher);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + delegate + "}";
    }

    /**
     * @param rule An {@link ArchRule} that should be "frozen" on the first call, i.e. all occurring violations will be stored for comparison
     *             on consecutive calls.
     * @return A {@link FreezingArchRule} wrapping the original rule

     * @see FreezingArchRule
     */
    @PublicAPI(usage = ACCESS)
    public static FreezingArchRule freeze(ArchRule rule) {
        return new FreezingArchRule(rule, ViolationStoreFactory.create(), ViolationLineMatcherFactory.create());
    }

    private static class CategorizedViolations {
        private final Set<String> knownActualViolations = new HashSet<>();
        private final List<String> storedSolvedViolations;
        private final List<String> storedUnsolvedViolations = new ArrayList<>();

        CategorizedViolations(ViolationLineMatcher matcher, EvaluationResult actualResult, List<String> storedViolations) {
            List<String> storedViolationsLeft = new ArrayList<>(storedViolations);
            for (String actualViolation : actualResult.getFailureReport().getDetails()) {
                for (Iterator<String> iterator = storedViolationsLeft.iterator(); iterator.hasNext(); ) {
                    String storedViolation = iterator.next();
                    if (matcher.matches(actualViolation, storedViolation)) {
                        iterator.remove();
                        knownActualViolations.add(actualViolation);
                        storedUnsolvedViolations.add(storedViolation);
                        break;
                    }
                }
            }
            storedSolvedViolations = new ArrayList<>(storedViolations);
            storedSolvedViolations.removeAll(storedUnsolvedViolations);
        }

        Set<String> getKnownActualViolations() {
            return knownActualViolations;
        }

        List<String> getStoredSolvedViolations() {
            return storedSolvedViolations;
        }

        List<String> getStoredUnsolvedViolations() {
            return storedUnsolvedViolations;
        }
    }
}
