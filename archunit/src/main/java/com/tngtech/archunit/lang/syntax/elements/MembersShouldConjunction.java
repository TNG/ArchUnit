/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Same as {@link ClassesShouldConjunction} but for rules about {@link JavaMember members}.
 * In particular, the explanation about associativity of joining further {@link ArchCondition conditions}
 * via {@link #andShould(ArchCondition)} and {@link #orShould(ArchCondition)}
 * as explained within {@link ClassesShouldConjunction} also holds for the methods defined in this
 * hierarchy of classes.
 * @param <MEMBER> The concrete type of {@link JavaMember} this conjunction describes, e.g. {@link JavaMethod}.
 */
@PublicAPI(usage = ACCESS)
public interface MembersShouldConjunction<MEMBER extends JavaMember> extends ArchRule {

    /**
     * Same as {@link ClassesShouldConjunction#andShould(ArchCondition)} but for {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    MembersShouldConjunction<MEMBER> andShould(ArchCondition<? super MEMBER> condition);

    /**
     * Same as {@link ClassesShouldConjunction#andShould()} but for {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    MembersShould<? extends MembersShouldConjunction<MEMBER>> andShould();

    /**
     * Same as {@link ClassesShouldConjunction#orShould(ArchCondition)} but for {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    MembersShouldConjunction<MEMBER> orShould(ArchCondition<? super MEMBER> condition);

    /**
     * Same as {@link ClassesShouldConjunction#orShould()} but for {@link JavaMember}
     */
    @PublicAPI(usage = ACCESS)
    MembersShould<? extends MembersShouldConjunction<MEMBER>> orShould();
}
