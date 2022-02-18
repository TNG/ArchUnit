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

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface ConditionEvents extends Iterable<ConditionEvent> {

    /**
     * Adds a {@link ConditionEvent} to these events.
     * @param event A {@link ConditionEvent} caused by an {@link ArchCondition} when checking some element
     */
    void add(ConditionEvent event);

    /**
     * Can be used to override the information about the number of violations. If absent the violated rule
     * will simply report the number of violation lines as the number of violations (which is typically
     * correct, since ArchUnit usually reports one violation per line). However, in cases where
     * violations are omitted (e.g. because a limit of reported violations is configured), this information
     * can be supplied here to inform users that there actually were more violations than reported.
     * @param informationAboutNumberOfViolations The text to be shown for the number of times a rule has been violated
     */
    void setInformationAboutNumberOfViolations(String informationAboutNumberOfViolations);

    /**
     * @return All {@link ConditionEvent events} that correspond to violations.
     */
    Collection<ConditionEvent> getViolating();

    /**
     * @deprecated This method will be removed without any replacement. If you think you need this method, please
     *             file an issue at https://github.com/TNG/ArchUnit/issues
     * @return All {@link ConditionEvent events} that correspond to non-violating elements.
     */
    @Deprecated
    Collection<ConditionEvent> getAllowed();

    /**
     * @return {@code true}, if these events contain any {@link #getViolating() violating} event, otherwise {@code false}
     */
    boolean containViolation();

    /**
     * @deprecated This method will be removed without any replacement. If you think you need this method, please
     *             file an issue at https://github.com/TNG/ArchUnit/issues
     * @return {@code true}, if these events contain any event, be it {@link #getViolating() violating} or {@link #getAllowed() allowed}.
     */
    @Deprecated
    boolean isEmpty();

    /**
     * @return Sorted failure messages describing the contained failures of these events.
     *         Also offers information about the number of violations contained in these events.
     */
    FailureMessages getFailureMessages();

    /**
     * Passes violations to the supplied {@link ViolationHandler}. The passed violations will automatically
     * be filtered by the reified type of the given {@link ViolationHandler}. That is, if a
     * <code>ViolationHandler&lt;SomeClass&gt;</code> is passed, only violations by objects assignable to
     * <code>SomeClass</code> will be reported. The term 'reified' means that the type parameter
     * was not erased, i.e. ArchUnit can still determine the actual type parameter of the passed violation handler,
     * otherwise the upper bound, in extreme cases {@link Object}, will be used (i.e. all violations will be passed).
     *
     * @param violationHandler The violation handler that is supposed to handle all violations matching the
     *                         respective type parameter
     */
    @PublicAPI(usage = INHERITANCE, state = EXPERIMENTAL)
    void handleViolations(ViolationHandler<?> violationHandler);

    @PublicAPI(usage = ACCESS)
    final class Factory {
        private Factory() {
        }

        @PublicAPI(usage = ACCESS)
        public static ConditionEvents create() {
            return new ConditionEventsInternal();
        }
    }
}
