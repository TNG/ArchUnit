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
package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.GivenMembers;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersConjunction;

abstract class AbstractGivenMembersInternal<MEMBER extends JavaMember, SELF extends AbstractGivenMembersInternal<MEMBER, SELF>>
        extends AbstractGivenObjects<MEMBER, SELF>
        implements GivenMembers<MEMBER>, GivenMembersConjunction<MEMBER> {

    AbstractGivenMembersInternal(
            AbstractGivenObjects.Factory<MEMBER, SELF> factory,
            Priority priority,
            ClassesTransformer<MEMBER> classesTransformer,
            Function<ArchCondition<MEMBER>, ArchCondition<MEMBER>> prepareCondition,
            PredicateAggregator<MEMBER> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
    }

    @Override
    public GivenMembersThatInternal<MEMBER, SELF> that() {
        return new GivenMembersThatInternal<>(self(), currentPredicate());
    }

    @Override
    public GivenMembersThatInternal<MEMBER, SELF> and() {
        return new GivenMembersThatInternal<>(self(), currentPredicate().thatANDs());
    }

    @Override
    public GivenMembersThatInternal<MEMBER, SELF> or() {
        return new GivenMembersThatInternal<>(self(), currentPredicate().thatORs());
    }

    @Override
    public ArchRule should(ArchCondition<? super MEMBER> condition) {
        return new MembersShouldInternal<>(finishedClassesTransformer(), priority, condition.<MEMBER>forSubType(), this.prepareCondition);
    }

    static class GivenMembersInternal extends AbstractGivenMembersInternal<JavaMember, GivenMembersInternal> {

        GivenMembersInternal(Priority priority, ClassesTransformer<JavaMember> classesTransformer) {
            this(priority, classesTransformer, Functions.<ArchCondition<JavaMember>>identity());
        }

        GivenMembersInternal(
                Priority priority,
                ClassesTransformer<JavaMember> classesTransformer,
                Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition) {

            this(new GivenMembersFactory(),
                    priority,
                    classesTransformer,
                    prepareCondition,
                    new PredicateAggregator<JavaMember>(),
                    Optional.<String>absent());
        }

        private GivenMembersInternal(
                Factory<JavaMember, GivenMembersInternal> factory,
                Priority priority,
                ClassesTransformer<JavaMember> classesTransformer,
                Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition,
                PredicateAggregator<JavaMember> relevantObjectsPredicates,
                Optional<String> overriddenDescription) {

            super(factory, priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }

        private static class GivenMembersFactory implements AbstractGivenObjects.Factory<JavaMember, GivenMembersInternal> {

            @Override
            public GivenMembersInternal create(
                    Priority priority,
                    ClassesTransformer<JavaMember> classesTransformer,
                    Function<ArchCondition<JavaMember>, ArchCondition<JavaMember>> prepareCondition,
                    PredicateAggregator<JavaMember> relevantObjectsPredicates,
                    Optional<String> overriddenDescription) {

                return new GivenMembersInternal(this,
                        priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
            }
        }
    }
}
