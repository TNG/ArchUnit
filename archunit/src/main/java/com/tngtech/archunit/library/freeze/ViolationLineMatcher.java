/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Allows to decide when two lines of two violations count as "equivalent". I.e. when {@link FreezingArchRule} determines if the description of an
 * occurring violation matches the description of an already stored one, it will use the configured {@link ViolationLineMatcher} to do so
 * by checking all the description lines of those two violations against each other.
 * <br><br>
 * A simple example could be to match any (xxx).java from lines and count all violations equivalent if they appear in the same class (as long as the
 * violations comply to the default description pattern adding 'in (ClassName.java:y) to the end of the lines). This would then effectively count all
 * violations in a class with any previous violation as known and not fail the check.
 */
@PublicAPI(usage = INHERITANCE)
public interface ViolationLineMatcher {

    /**
     * @param lineFromFirstViolation A line from the description of a violation of an {@link ArchRule}
     * @param lineFromSecondViolation A line from the description of another violation of an {@link ArchRule}
     * @return true, if and only if those two lines should be considered equivalent (e.g. because only the line number differs)
     */
    boolean matches(String lineFromFirstViolation, String lineFromSecondViolation);
}
